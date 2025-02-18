package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.*;
import com.mlbeez.feeder.repository.*;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

    @Autowired
    private CardDetailsRepository cardDetailsRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WarrantyRepository warrantyRepository;

    @Autowired
    private InsurancePaymentService insurancePaymentService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AspNetUserRepository aspNetUserRepository;

    @Autowired
    private InsurancePaymentRepository insurancePaymentRepository;

    @Autowired
    private AddressesRepository addressesRepository;

    private final ThirdPartyService thirdPartyService;

    @Autowired
    public WebhookService(ThirdPartyService thirdPartyService) {
        this.thirdPartyService = thirdPartyService;
    }

    @Autowired
    private CheckoutService checkoutService;

    public void handleChargeSucceeded(Charge charge) {

        try {

            if (charge == null) {
                logger.error("Charge is null in charge.succeeded event");
                return;
            }
            String customerId = charge.getCustomer();

            Customers customers = customerRepository.findByCustomerId(customerId);

            if (customers == null) {
                logger.error("customerId not found: {}", customerId);
                return;
            }
            UserResponseBaseModel userDetail = aspNetUserRepository.findById(customers.getUserId()).orElseThrow(() -> new DataNotFoundException("Data not found!"));

            PaymentMethod paymentMethod;

            paymentMethod = PaymentMethod.retrieve(charge.getPaymentMethod());
            if (paymentMethod != null) {
                saveCardDetails(paymentMethod, userDetail, customers.getCustomerId());
            } else {
                logger.error("Payment method is null for paymentMethodId: {}, paymentMethodId", customerId);
            }
        } catch (StripeException e) {
            logger.error("StripeException while retrieving payment method for paymentMethodId {}:",
                    e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while saving card details for paymentMethodId {}:", e.getMessage());
        }

    }

    public void handleInvoicePaymentSucceeded(Invoice invoice) {
        if (invoice == null) {
            logger.error("Invoice is null in invoice.payment_succeeded event");
            return;
        }

        String mode;
        try {
            Price price = Price.retrieve(invoice.getLines().getData().get(0).getPrice().getId());
            mode = price.getMetadata().get("type");
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }

        try {
            Customers userDetail = customerRepository.findByCustomerId(invoice.getCustomer());

            if (userDetail == null) {
                logger.error("customersId not found: {}", invoice.getCustomer());
                return;
            }

            UserResponseBaseModel userResponseBaseModel = aspNetUserRepository.findById(userDetail.getUserId()).orElseThrow(() -> new DataNotFoundException("data not found!"));

            UserAddressesModel userAddressesModel = addressesRepository.findByCreatedBy(userDetail.getUserId());


            if (userAddressesModel == null) {
                logger.error("User address not found for userId: {}", userDetail.getUserId());
                return;
            }

            Subscription subscription = Subscription.retrieve(invoice.getSubscription());
            String paymentMethodId = subscription.getDefaultPaymentMethod();
            String receiptUrl = invoice.getHostedInvoiceUrl();
            logger.info("Received invoice.payment_succeeded event with receipt URL: {}", receiptUrl);

            Optional<Warranty> warrantyOptional = warrantyRepository.findByProductId(invoice.getLines().getData().get(0).getPlan().getProduct());
            if (warrantyOptional.isEmpty()) {
                logger.error("Warranty not found");
                return;
            }

            Charge charge = Charge.retrieve(invoice.getCharge());

            Warranty warranty = warrantyOptional.get();
            String productId = subscription.getItems().getData().get(0).getPrice().getProduct();

            Optional<InsurancePayment> insurance =
                    insurancePaymentRepository.findBySubscriptionIdAndUserIdAndWarrantyId(subscription.getId(),
                            userDetail.getUserId(), warranty.getWarrantyId());
            if (insurance.isPresent()) {
                InsurancePayment insurancePayment = insurance.get();
                insurancePaymentRepository.deleteById(insurancePayment.getId());
            }

            InsurancePayment insurancePayment = new InsurancePayment();
            insurancePayment.setSubscriptionId(subscription.getId());
            insurancePayment.setUserId(userDetail.getUserId());
            insurancePayment.setDefault_payment_method(paymentMethodId);
            insurancePayment.setProductId(productId);
            insurancePayment.setEmail(invoice.getCustomerEmail());
            insurancePayment.setName(invoice.getCustomerName());
            insurancePayment.setPhoneNumber(userResponseBaseModel.getPhoneNumber());
            insurancePayment.setCustomer(invoice.getCustomer());
            insurancePayment.setInvoice_status(invoice.getStatus());
            insurancePayment.setWarrantyId(warranty.getWarrantyId());
            insurancePayment.setAmount(invoice.getAmountPaid());
            insurancePayment.setInvoiceId(invoice.getId());
            insurancePayment.setSubscriptionMode(mode);
            insurancePayment.setCurrency(invoice.getCurrency());
            insurancePayment.setSubscription_Status(subscription.getStatus());
            insurancePayment.setChargeRequest_status(charge.getStatus());
            insurancePayment.setMode(invoice.getLines().getData().get(0).getType());
            insurancePaymentService.storePayment(insurancePayment);

            String paymentIntentId = invoice.getPaymentIntent();
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            String chargeId = paymentIntent.getLatestCharge();
            Product product = Product.retrieve(productId);

            logger.info("Payment succeeded! Receipt URL: {}", receiptUrl);

            if (paymentMethodId != null) {
                PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
                Transactions transaction = new Transactions();
                transaction.setUserId(userDetail.getUserId());
                transaction.setProductName(product.getName());
                transaction.setProductId(productId);
                transaction.setCustomerId(invoice.getCustomer());
                transaction.setCard(paymentMethod.getCard().getLast4());
                transaction.setPrice(invoice.getAmountPaid());
                transaction.setReceiptUrl(receiptUrl);
                transaction.setPhoneNumber(userResponseBaseModel.getPhoneNumber());
                transaction.setEmail(userResponseBaseModel.getEmail());
                transaction.setPaymentMethod(paymentMethod.getType());
                transaction.setInvoice_status(invoice.getStatus());
                transaction.setChargeRequest_status(charge.getStatus());
                transaction.setTransactionId(chargeId);
                transaction.setInterval(subscription.getItems().getData().get(0).getPrice().getRecurring().getInterval());

                transactionService.storeHistory(transaction);
            }


            UserRequest userRequest = new UserRequest();

            UserRequest.User user = new UserRequest.User();
            UserRequest.Profile profile = new UserRequest.Profile();

            user.setPhone(userResponseBaseModel.getPhoneNumber());
            user.setEmail(userResponseBaseModel.getEmail());
            user.setIs_primary(true);

            profile.setFirst_name(userResponseBaseModel.getFirstname());
            profile.setLast_name(userResponseBaseModel.getLastname());
            profile.setAddress(userAddressesModel.getAddressLine1());
            profile.setCity(userAddressesModel.getCityName());
            profile.setZip(userAddressesModel.getZipCode());
            profile.setState(userAddressesModel.getStateName());
            user.setProfile(profile);
            List<UUID> productPriceIds = Arrays.stream(warranty.getProduct_price_ids().split(","))
                    .map(String::trim)
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
            user.setProduct_price_ids(productPriceIds);
            userRequest.setUsers(List.of(user));
            logger.info("Sending request payload: {}", userRequest);
            thirdPartyService.sendUserDetails(userRequest);
        } catch (Exception e) {
            logger.error("Error processing invoice.payment_succeeded event: {}", e.getMessage());
        }
    }

    public void handleCheckoutSessionCompleted(Session session) {
        if (session == null) {
            logger.error("Session is null in checkout.session.completed event");
            return;
        }

        Subscription subscription;
        try {
            subscription = retrieveSubscription(session.getSubscription());
            if (subscription == null) {
                logger.error("Failed to retrieve subscription for session: {}", session.getId());
                return;
            }
        } catch (Exception e) {
            logger.error("StripeException while retrieving subscription for session {}: {}", session.getId(),
                    e.getMessage(), e);
            return;
        }
        boolean isOneTimeSubscription = checkIfOneTimeSubscription(session);
        if (isOneTimeSubscription) {
            checkoutService.cancelSubscriptionAtPeriodEnd(subscription.getId());
        }

    }

    private boolean checkIfOneTimeSubscription(Session session) {
        logger.info("Requested to check the one time or Recurring payment");
        return session != null && "one_time".equals(session.getMetadata().get("type"));
    }


    public void handleCustomerSubscriptionDeleted(Subscription subscription) {
        insurancePaymentService.deleteSubscriptionPayment(subscription.getCustomer(), subscription.getId());
    }

    private Subscription retrieveSubscription(String subscriptionId) {
        try {
            return Subscription.retrieve(subscriptionId);
        } catch (StripeException e) {
            logger.error("Error retrieving subscription: {}", e.getMessage());
            return null;
        }
    }

    public void saveCardDetails(PaymentMethod paymentMethod, UserResponseBaseModel userDetail, String customerId) {
        logger.info("Requested to save the card details");
        try {
            Optional<CardDetails> findUSerId = cardDetailsRepository.findByUserId(userDetail.getId());
            if (findUSerId.isPresent()) {
                CardDetails cardDetails = findUSerId.get();
                Long cardId = cardDetails.getId();
                cardDetailsRepository.deleteById(cardId);
            }
            CardDetails cardDetails = new CardDetails();
            cardDetails.setPayment_methodId(paymentMethod.getId());
            cardDetails.setCardBrand(paymentMethod.getCard().getBrand());
            cardDetails.setCard_Last4(paymentMethod.getCard().getLast4());
            cardDetails.setExp_month(paymentMethod.getCard().getExpMonth());
            cardDetails.setExp_year(paymentMethod.getCard().getExpYear());
            cardDetails.setCountry(paymentMethod.getCard().getCountry());
            cardDetails.setFunding(paymentMethod.getCard().getFunding());
            cardDetails.setUserId(userDetail.getId());
            cardDetails.setCustomer(customerId);
            cardDetails.setType(paymentMethod.getType());
            cardDetails.setEmail(paymentMethod.getBillingDetails().getEmail());
            cardDetails.setName(paymentMethod.getBillingDetails().getName());

            cardDetailsRepository.save(cardDetails);
        } catch (Exception e) {
            logger.error("Failed to save card details: {}", e.getMessage());
        }
    }
}

