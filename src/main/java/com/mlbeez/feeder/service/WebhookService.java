package com.mlbeez.feeder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlbeez.feeder.model.*;
import com.mlbeez.feeder.repository.*;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

    @Autowired private CardDetailsRepository cardDetailsRepository;
    @Autowired private TransactionService transactionService;
    @Autowired private WarrantyRepository warrantyRepository;
    @Autowired private InsurancePaymentService insurancePaymentService;
    @Autowired private AspNetUserRepository aspNetUserRepository;
    @Autowired private InsurancePaymentRepository insurancePaymentRepository;
    @Autowired private AddressesRepository addressesRepository;

    private final ThirdPartyService thirdPartyService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    public WebhookService(ThirdPartyService thirdPartyService) {
        this.thirdPartyService = thirdPartyService;
    }

    // ------------------------- RAW JSON HELPERS -------------------------

    private final ObjectMapper objectMapper = new ObjectMapper();


    private JsonNode getRoot(Event event) {
        try {
            String rawJson = event.getDataObjectDeserializer().getRawJson();
            return objectMapper.readTree(rawJson);
        } catch (Exception e) {
            logger.error("Failed to parse event raw JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to parse Stripe event JSON", e);
        }
    }

    private String getPriceId(JsonNode root) {
        try {
            return root.get("lines")
                    .get("data")
                    .get(0)
                    .get("pricing")
                    .get("price_details")
                    .get("price")
                    .asText();
        } catch (Exception e) {
            logger.error("Failed to extract priceId from raw JSON: {}", e.getMessage());
            return null;
        }
    }

    private String getProductId(JsonNode root) {
        try {
            return root.get("lines")
                    .get("data")
                    .get(0)
                    .get("pricing")
                    .get("price_details")
                    .get("product")
                    .asText();
        } catch (Exception e) {
            logger.error("Failed to extract productId: {}", e.getMessage());
            return null;
        }
    }

    private String getSubscriptionItem(JsonNode root) {
        try {
            return root.get("lines")
                    .get("data")
                    .get(0)
                    .get("parent")
                    .get("subscription_item_details")
                    .get("subscription_item")
                    .asText();
        } catch (Exception e) {
            return null;
        }
    }

    private String getInterval(JsonNode root) {
        try {
            return root.get("lines")
                    .get("data")
                    .get(0)
                    .get("pricing")
                    .get("type")     // price_details
                    .asText();
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------- HANDLERS -------------------------

    public void handleChargeSucceeded(Charge charge, CardDetails cardDetails) {

        if (charge == null) {
            logger.error("charge.succeeded => Charge object null");
            return;
        }

        try {
            Customer customer = Customer.retrieve(charge.getCustomer());
            UserResponseBaseModel userDetail =
                    aspNetUserRepository.findById(customer.getMetadata().get("userId"))
                            .orElseThrow(() -> new DataNotFoundException("User not found"));

            PaymentMethod pm = PaymentMethod.retrieve(charge.getPaymentMethod());

            saveCardDetails(pm, userDetail, customer.getId(), cardDetails);

        } catch (Exception e) {
            logger.error("Error in charge.succeeded: {}", e.getMessage());
        }
    }

    public void handleInvoicePaymentSucceeded(
            Invoice invoice,
            InsurancePayment insurancePayment,
            Transactions transaction,
            Event event
    ) {
        if (invoice == null) {
            logger.error("invoice.payment_succeeded => Invoice null");
            return;
        }

        JsonNode root = getRoot(event);

        String priceId = getPriceId(root);
        String productId = getProductId(root);
        String interval = getInterval(root);

        if (priceId == null || productId == null) {
            logger.error("Missing priceId/productId in raw JSON");
            return;
        }

        try {
            Price price = Price.retrieve(priceId);
            String mode = price.getMetadata().get("type");   // monthly/one_time

            Customer customer = Customer.retrieve(invoice.getCustomer());
            String userId = customer.getMetadata().get("userId");

            UserResponseBaseModel user = aspNetUserRepository.findById(userId)
                    .orElseThrow(() -> new DataNotFoundException("User not found"));

            UserAddressesModel address = addressesRepository.findByCreatedBy(userId);

            Subscription subscription = Subscription.retrieve(invoice.getSubscription());
            String paymentMethodId = subscription.getDefaultPaymentMethod();

            Optional<Warranty> warrantyOptional =
                    warrantyRepository.findByProductId(productId);

            if (warrantyOptional.isEmpty()) {
                logger.error("Warranty not found for product {}", productId);
                return;
            }

            Charge charge = Charge.retrieve(invoice.getCharge());
            String invoiceMode = invoice.getLines().getData().get(0).getType();
            Warranty warranty = warrantyOptional.get();

            // Delete previous record if exists
            insurancePaymentRepository
                    .findBySubscriptionIdAndUserIdAndWarrantyId(
                            subscription.getId(), userId, warranty.getWarrantyId()
                    )
                    .ifPresent(prev -> insurancePaymentRepository.deleteById(prev.getId()));

            // SAVE insurance payment
            insurancePayment.setSubscriptionId(subscription.getId());
            insurancePayment.setUserId(userId);
            insurancePayment.setProductId(productId);
            insurancePayment.setInvoiceId(invoice.getId());
            insurancePayment.setInvoice_status(invoice.getStatus());
            insurancePayment.setAmount(invoice.getAmountPaid());
            insurancePayment.setChargeRequest_status(charge.getStatus());
            insurancePayment.setCustomer(invoice.getCustomer());
            insurancePayment.setDefault_payment_method(paymentMethodId);
            insurancePayment.setMode(invoiceMode);
            insurancePayment.setSubscription_Status(subscription.getStatus());
            insurancePayment.setPhoneNumber(user.getPhoneNumber());
            insurancePayment.setName(invoice.getCustomerName());
            insurancePayment.setEmail(invoice.getCustomerEmail());
            insurancePayment.setWarrantyId(warranty.getWarrantyId());
            insurancePayment.setCurrency(invoice.getCurrency());
            insurancePayment.setSubscriptionMode(mode);

            insurancePaymentService.storePayment(insurancePayment);

            // ---------------- TRANSACTION LOG -----------------

            PaymentIntent intent = PaymentIntent.retrieve(invoice.getPaymentIntent());
            String chargeId = intent.getLatestCharge();

            if (paymentMethodId != null) {
                PaymentMethod method = PaymentMethod.retrieve(paymentMethodId);
                Product product = Product.retrieve(productId);

                transaction.setUserId(userId);
                transaction.setUserName(user.getUsername());
                transaction.setCustomerId(invoice.getCustomer());
                transaction.setPaymentMethod(method.getCard().getLast4());
                transaction.setProductId(productId);
                transaction.setProductName(product.getName());
                transaction.setVendor(warranty.getVendor());
                transaction.setCard(method.getCard().getLast4());
                transaction.setEmail(user.getEmail());
                transaction.setPhoneNumber(user.getPhoneNumber());
                transaction.setPrice(invoice.getAmountPaid());
                transaction.setChargeRequest_status(intent.getStatus());
                transaction.setInvoice_status(invoice.getStatus());
                transaction.setReceiptUrl(invoice.getHostedInvoiceUrl());
                transaction.setTransactionId(chargeId);
                transaction.setInterval(subscription.getItems().getData().get(0).getPrice().getRecurring().getInterval());

                transactionService.storeHistory(transaction);
            }

            // ---------------- SEND TO THIRD PARTY -----------------

            UserRequest req = new UserRequest();
            UserRequest.User u = new UserRequest.User();
            UserRequest.Profile profile = new UserRequest.Profile();

            u.setEmail(user.getEmail());
            u.setPhone(user.getPhoneNumber());
            u.setIs_primary(true);

            profile.setFirst_name(user.getFirstname());
            profile.setLast_name(user.getLastname());
            profile.setAddress(address != null ? address.getAddressLine1() : "");
            profile.setCity(address != null ? address.getCityName() : "");
            profile.setZip(address != null ? address.getZipCode() : "");

            u.setProfile(profile);

            List<UUID> monthly = Arrays.stream(warranty.getProductMonthlyPriceIds().split(","))
                    .map(String::trim).map(UUID::fromString).collect(Collectors.toList());

            List<UUID> yearly = Arrays.stream(warranty.getProductYearlyPriceIds().split(","))
                    .map(String::trim).map(UUID::fromString).collect(Collectors.toList());

            u.setProduct_price_ids(interval.equals("month") ? monthly : yearly);

            req.setUsers(List.of(u));

            thirdPartyService.sendUserDetails(req);

        } catch (Exception e) {
            logger.error("Exception in invoice.payment_succeeded: {}", e.getMessage());
        }
    }

    public void handleCheckoutSessionCompleted(Session session) {
        if (session == null) return;

        try {
            Subscription sub = Subscription.retrieve(session.getSubscription());

            if ("one_time".equals(session.getMetadata().get("type"))) {
                checkoutService.cancelSubscriptionAtPeriodEnd(sub.getId());
            }
        } catch (Exception e) {
            logger.error("checkout.session.completed error: {}", e.getMessage());
        }
    }

    public void handleCustomerSubscriptionDeleted(Subscription subscription) {
        insurancePaymentService.deleteSubscriptionPayment(
                subscription.getCustomer(),
                subscription.getId()
        );
    }

    // ---------------- CARD SAVE -------------------------

    public void saveCardDetails(PaymentMethod pm, UserResponseBaseModel user, String customerId, CardDetails card) {
        try {
            cardDetailsRepository.findByUserId(user.getId())
                    .ifPresent(existing -> cardDetailsRepository.deleteById(existing.getId()));

            card.setPayment_methodId(pm.getId());
            card.setCardBrand(pm.getCard().getBrand());
            card.setCard_Last4(pm.getCard().getLast4());
            card.setExp_month(pm.getCard().getExpMonth());
            card.setExp_year(pm.getCard().getExpYear());
            card.setCountry(pm.getCard().getCountry());
            card.setFunding(pm.getCard().getFunding());
            card.setUserId(user.getId());
            card.setCustomer(customerId);
            card.setEmail(user.getEmail());
            card.setName(user.getUsername());
            card.setType(pm.getType());

            cardDetailsRepository.save(card);

        } catch (Exception e) {
            logger.error("Error saving card: {}", e.getMessage());
        }
    }
}





































//package com.mlbeez.feeder.service;
//
//import com.google.gson.JsonObject;
//import com.mlbeez.feeder.model.*;
//import com.mlbeez.feeder.repository.*;
//import com.mlbeez.feeder.service.exception.DataNotFoundException;
//import com.stripe.exception.StripeException;
//import com.stripe.model.*;
//import com.stripe.model.checkout.Session;
//import com.stripe.net.ApiResource;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//
//@Service
//public class WebhookService {
//
//    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
//
//    @Autowired
//    private CardDetailsRepository cardDetailsRepository;
//
//    @Autowired
//    private TransactionService transactionService;
//
//    @Autowired
//    private WarrantyRepository warrantyRepository;
//
//    @Autowired
//    private InsurancePaymentService insurancePaymentService;
//
//    @Autowired
//    private AspNetUserRepository aspNetUserRepository;
//
//    @Autowired
//    private InsurancePaymentRepository insurancePaymentRepository;
//
//    @Autowired
//    private AddressesRepository addressesRepository;
//
//    private final ThirdPartyService thirdPartyService;
//
//    @Autowired
//    public WebhookService(ThirdPartyService thirdPartyService) {
//        this.thirdPartyService = thirdPartyService;
//    }
//
//    @Autowired
//    private CheckoutService checkoutService;
//
//    public void handleChargeSucceeded(Charge charge, CardDetails cardDetails) {
//
//        try {
//
//            if (charge == null) {
//                logger.error("Charge is null in charge.succeeded event");
//                return;
//            }
//
//            Customer customer = Customer.retrieve(charge.getCustomer());
//
//            if (customer == null) {
//                logger.error("customerId not found");
//                return;
//            }
//            UserResponseBaseModel userDetail = aspNetUserRepository.findById(customer.getMetadata().get("userId")).orElseThrow(() -> new DataNotFoundException("Data not found!"));
//
//            PaymentMethod paymentMethod;
//
//            paymentMethod = PaymentMethod.retrieve(charge.getPaymentMethod());
//            if (paymentMethod != null) {
//                saveCardDetails(paymentMethod, userDetail, charge.getCustomer(), cardDetails);
//            } else {
//                logger.error("Payment method is null for paymentMethodId: {}, paymentMethodId", customer);
//            }
//        } catch (StripeException e) {
//            logger.error("StripeException while retrieving payment method for paymentMethodId {}:",
//                    e.getMessage());
//        } catch (Exception e) {
//            logger.error("Exception while saving card details for paymentMethodId {}:", e.getMessage());
//        }
//
//    }
//
//    public void handleInvoicePaymentSucceeded(Invoice invoice, InsurancePayment insurancePayment, Transactions transaction,Event event) {
//        if (invoice == null) {
//            logger.error("Invoice is null in invoice.payment_succeeded event");
//            return;
//        }
//
//        String mode;
//        try {
//            String priceId = extractPriceIdFromEvent(event);
//
//            Price price = Price.retrieve(priceId);
//            System.out.println("price ====> " + price);
//            mode = price.getMetadata().get("type");
//        } catch (StripeException e) {
//            throw new RuntimeException(e);
//        }
//
//        try {
//            Customer customer = Customer.retrieve(invoice.getCustomer());
//            UserResponseBaseModel userResponseBaseModel = aspNetUserRepository.findById(customer.getMetadata().get("userId")).orElseThrow(() -> new DataNotFoundException("data not found!"));
//
//            UserAddressesModel userAddressesModel = addressesRepository.findByCreatedBy(customer.getMetadata().get("userId"));
//            Subscription subscription = Subscription.retrieve(invoice.getSubscription());
//            String paymentMethodId = subscription.getDefaultPaymentMethod();
//            String receiptUrl = invoice.getHostedInvoiceUrl();
//            logger.info("Received invoice.payment_succeeded event with receipt URL: {}", receiptUrl);
//
//            Optional<Warranty> warrantyOptional = warrantyRepository.findByProductId(invoice.getLines().getData().get(0).getPlan().getProduct());
//            if (warrantyOptional.isEmpty()) {
//                logger.error("Warranty not found");
//                return;
//            }
//
//            Charge charge = Charge.retrieve(invoice.getCharge());
//
//            Warranty warranty = warrantyOptional.get();
//            String productId = extractProductIdFromEvent(event);
//            System.out.println("productId ==> " + productId);
//            Optional<InsurancePayment> insurance =
//                    insurancePaymentRepository.findBySubscriptionIdAndUserIdAndWarrantyId(subscription.getId(),
//                            customer.getMetadata().get("userId"), warranty.getWarrantyId());
//            if (insurance.isPresent()) {
//                InsurancePayment insurancePaymentGet = insurance.get();
//                insurancePaymentRepository.deleteById(insurancePaymentGet.getId());
//            }
//
//            insurancePayment.setSubscriptionId(subscription.getId());
//            insurancePayment.setUserId(customer.getMetadata().get("userId"));
//            insurancePayment.setDefault_payment_method(paymentMethodId);
//            insurancePayment.setProductId(productId);
//            insurancePayment.setEmail(invoice.getCustomerEmail());
//            insurancePayment.setName(invoice.getCustomerName());
//            insurancePayment.setPhoneNumber(userResponseBaseModel.getPhoneNumber());
//            insurancePayment.setCustomer(invoice.getCustomer());
//            insurancePayment.setInvoice_status(invoice.getStatus());
//            insurancePayment.setWarrantyId(warranty.getWarrantyId());
//            insurancePayment.setAmount(invoice.getAmountPaid());
//            insurancePayment.setInvoiceId(invoice.getId());
//            insurancePayment.setSubscriptionMode(mode);
//            insurancePayment.setCurrency(invoice.getCurrency());
//            insurancePayment.setSubscription_Status(subscription.getStatus());
//            insurancePayment.setChargeRequest_status(charge.getStatus());
//            insurancePayment.setMode(invoice.getLines().getData().get(0).getType());
//            insurancePaymentService.storePayment(insurancePayment);
//
//            String paymentIntentId = invoice.getPaymentIntent();
//            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
//
//            String chargeId = paymentIntent.getLatestCharge();
//            Product product = Product.retrieve(productId);
//
//            logger.info("Payment succeeded! Receipt URL: {}", receiptUrl);
//
//            if (paymentMethodId != null) {
//                PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
//                transaction.setUserId(customer.getMetadata().get("userId"));
//                transaction.setProductName(product.getName());
//                transaction.setProductId(productId);
//                transaction.setCustomerId(invoice.getCustomer());
//                transaction.setUserName(invoice.getCustomerName());
//                transaction.setVendor(warranty.getVendor());
//                transaction.setCard(paymentMethod.getCard().getLast4());
//                transaction.setPrice(invoice.getAmountPaid());
//                transaction.setReceiptUrl(receiptUrl);
//                transaction.setPhoneNumber(userResponseBaseModel.getPhoneNumber());
//                transaction.setEmail(userResponseBaseModel.getEmail());
//                transaction.setPaymentMethod(paymentMethod.getType());
//                transaction.setInvoice_status(invoice.getStatus());
//                transaction.setChargeRequest_status(charge.getStatus());
//                transaction.setTransactionId(chargeId);
//                transaction.setInterval(subscription.getItems().getData().get(0).getPrice().getRecurring().getInterval());
//
//                transactionService.storeHistory(transaction);
//            }
//
//            UserRequest userRequest = new UserRequest();
//
//            UserRequest.User user = new UserRequest.User();
//            UserRequest.Profile profile = new UserRequest.Profile();
//
//            user.setPhone(userResponseBaseModel.getPhoneNumber());
//            user.setEmail(userResponseBaseModel.getEmail());
//            user.setIs_primary(true);
//
//            profile.setFirst_name(userResponseBaseModel.getFirstname());
//            profile.setLast_name(userResponseBaseModel.getLastname());
//            profile.setAddress(userAddressesModel != null ? userAddressesModel.getAddressLine1() : "");
//            profile.setCity(userAddressesModel != null ? userAddressesModel.getCityName() : "");
//            profile.setZip(userAddressesModel != null ? userAddressesModel.getZipCode() : "");
//
//            user.setProfile(profile);
//            List<UUID> productMonthlyPriceIds = Arrays.stream(warranty.getProductMonthlyPriceIds().split(","))
//                    .map(String::trim)
//                    .map(UUID::fromString)
//                    .collect(Collectors.toList());
//            List<UUID> productYearlyPriceIds = Arrays.stream(warranty.getProductYearlyPriceIds().split(","))
//                    .map(String::trim)
//                    .map(UUID::fromString)
//                    .collect(Collectors.toList());
//            if (productMonthlyPriceIds.isEmpty()) {
//                logger.error("No valid product_price_ids found to send to third party. Input: {}", warranty.getProductMonthlyPriceIds());
//                return;
//            }
//            if (productYearlyPriceIds.isEmpty()) {
//                logger.error("No valid product_price_ids found to send to third party. Input: {}", warranty.getProductYearlyPriceIds());
//                return;
//            }
//            user.setProduct_price_ids(Objects.equals(subscription.getItems().getData().get(0).getPrice().getRecurring().getInterval(), "month") ? productMonthlyPriceIds : productYearlyPriceIds);
//            userRequest.setUsers(List.of(user));
//            logger.info("Sending request payload: {}", userRequest);
//            thirdPartyService.sendUserDetails(userRequest);
//        } catch (Exception e) {
//            logger.error("Error processing invoice.payment_succeeded event: {}", e.getMessage());
//        }
//    }
//
//    public void handleCheckoutSessionCompleted(Session session) {
//        if (session == null) {
//            logger.error("Session is null in checkout.session.completed event");
//            return;
//        }
//
//        Subscription subscription;
//        try {
//            subscription = retrieveSubscription(session.getSubscription());
//            if (subscription == null) {
//                logger.error("Failed to retrieve subscription for session: {}", session.getId());
//                return;
//            }
//        } catch (Exception e) {
//            logger.error("StripeException while retrieving subscription for session {}: {}", session.getId(),
//                    e.getMessage(), e);
//            return;
//        }
//        boolean isOneTimeSubscription = checkIfOneTimeSubscription(session);
//        if (isOneTimeSubscription) {
//            checkoutService.cancelSubscriptionAtPeriodEnd(subscription.getId());
//        }
//
//    }
//
//    private boolean checkIfOneTimeSubscription(Session session) {
//        logger.info("Requested to check the one time or Recurring payment");
//        return session != null && "one_time".equals(session.getMetadata().get("type"));
//    }
//
//
//    public void handleCustomerSubscriptionDeleted(Subscription subscription) {
//        insurancePaymentService.deleteSubscriptionPayment(subscription.getCustomer(), subscription.getId());
//    }
//
//    private Subscription retrieveSubscription(String subscriptionId) {
//        try {
//            return Subscription.retrieve(subscriptionId);
//        } catch (StripeException e) {
//            logger.error("Error retrieving subscription: {}", e.getMessage());
//            return null;
//        }
//    }
//
//    public void saveCardDetails(PaymentMethod paymentMethod, UserResponseBaseModel userDetail, String customerId, CardDetails cardDetails) {
//        logger.info("Requested to save the card details");
//        try {
//            Optional<CardDetails> findUSerId = cardDetailsRepository.findByUserId(userDetail.getId());
//            if (findUSerId.isPresent()) {
//                CardDetails details = findUSerId.get();
//                Long cardId = details.getId();
//                cardDetailsRepository.deleteById(cardId);
//            }
//            cardDetails.setPayment_methodId(paymentMethod.getId());
//            cardDetails.setCardBrand(paymentMethod.getCard().getBrand());
//            cardDetails.setCard_Last4(paymentMethod.getCard().getLast4());
//            cardDetails.setExp_month(paymentMethod.getCard().getExpMonth());
//            cardDetails.setExp_year(paymentMethod.getCard().getExpYear());
//            cardDetails.setCountry(paymentMethod.getCard().getCountry());
//            cardDetails.setFunding(paymentMethod.getCard().getFunding());
//            cardDetails.setUserId(userDetail.getId());
//            cardDetails.setCustomer(customerId);
//            cardDetails.setType(paymentMethod.getType());
//            cardDetails.setEmail(paymentMethod.getBillingDetails().getEmail());
//            cardDetails.setName(paymentMethod.getBillingDetails().getName());
//
//            cardDetailsRepository.save(cardDetails);
//        } catch (Exception e) {
//            logger.error("Failed to save card details: {}", e.getMessage());
//        }
//    }
//
//    private String extractPriceIdFromEvent(Event event) {
//        String rawJson = event.getDataObjectDeserializer().getRawJson();
//        JsonObject root = ApiResource.GSON.fromJson(rawJson, JsonObject.class);
//
//        JsonObject lineItem = root
//                .getAsJsonObject("lines")
//                .getAsJsonArray("data")
//                .get(0)
//                .getAsJsonObject();
//
//        JsonObject pricing = lineItem
//                .getAsJsonObject("pricing")
//                .getAsJsonObject("price_details");
//
//        return pricing.getAsJsonPrimitive("price").getAsString();
//    }
//
//    private String extractProductIdFromEvent(Event event) {
//        String rawJson = event.getDataObjectDeserializer().getRawJson();
//        JsonObject root = ApiResource.GSON.fromJson(rawJson, JsonObject.class);
//
//        JsonObject lineItem = root
//                .getAsJsonObject("lines")
//                .getAsJsonArray("data")
//                .get(0)
//                .getAsJsonObject();
//
//        JsonObject pricing = lineItem
//                .getAsJsonObject("pricing")
//                .getAsJsonObject("price_details");
//
//        return pricing.getAsJsonPrimitive("product").getAsString();
//    }
//
//
//}
//
