package com.mlbeez.feeder.errorlogservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mlbeez.feeder.model.*;
import com.mlbeez.feeder.repository.AspNetUserRepository;
import com.mlbeez.feeder.service.PaymentFailedService;
import com.mlbeez.feeder.service.TransactionService;
import com.mlbeez.feeder.service.WebhookService;
import com.mlbeez.feeder.service.exception.*;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InvoicePaymentFailedService {

    private final PaymentFailedService paymentFailedService;
    private final AspNetUserRepository aspNetUserRepository;
    private final TransactionService transactionService;
    private final WebhookService webhookService;

    private static final Logger logger = LoggerFactory.getLogger(InvoicePaymentFailedService.class);

    public InvoicePaymentFailedService(PaymentFailedService paymentFailedService,
                                       AspNetUserRepository aspNetUserRepository,
                                       TransactionService transactionService,
                                       WebhookService webhookService) {
        this.paymentFailedService = paymentFailedService;
        this.aspNetUserRepository = aspNetUserRepository;
        this.transactionService = transactionService;
        this.webhookService = webhookService;
    }

    public void handleInvoicePaymentFailed(
            Invoice invoice,
            Event event,
            CardDetails cardDetails,
            PaymentFailed paymentFailed,
            Transactions transaction
    ) throws StripeException, JsonProcessingException {

        final ObjectMapper objectMapper = new ObjectMapper();

        if (invoice == null) {
            logger.error("Invoice is null in invoice.payment_failed event");
            throw new InvoiceNotFoundException("Invoice is missing");
        }

        // RAW JSON from event
        String rawJson = event.getDataObjectDeserializer().getRawJson();
        JsonNode root = objectMapper.readTree(rawJson);

        JsonNode line = null;
        if (root.has("lines") && root.get("lines").has("data") && root.get("lines").get("data").isArray()
                && root.get("lines").get("data").size() > 0) {
            line = root.get("lines").get("data").get(0);
        }

        String productId = null;
        String priceId = null;
        Long lineAmount = null;
        String interval = null;

        try {
            if (line != null) {
                JsonNode priceDetails = line.path("pricing").path("price_details");
                if (!priceDetails.isMissingNode()) {
                    productId = priceDetails.path("product").asText(null);
                    priceId = priceDetails.path("price").asText(null);
                }
                if (line.has("amount") && line.get("amount").isNumber()) {
                    lineAmount = line.get("amount").asLong();
                }
            }

            if (priceId != null) {
                Price price = Price.retrieve(priceId);
                if (price.getRecurring() != null) {
                    interval = price.getRecurring().getInterval();
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting product/price/interval from raw JSON: {}", e.getMessage());
        }

        // Customer and user
        Customer customer = Customer.retrieve(invoice.getCustomer());
        if (customer == null) {
            logger.error("Customer not found for invoice {}", invoice.getId());
            throw new CustomerNotFoundException("Customer not found");
        }

        UserResponseBaseModel userDetail =
                aspNetUserRepository.findById(customer.getMetadata().get("userId"))
                        .orElseThrow(() -> new UserNotFoundException("User not found!"));

        // Resolve charge ID (may be null directly on invoice)
        String chargeId = invoice.getCharge();
        if (chargeId == null) {
            String paymentIntentId = invoice.getPaymentIntent();
            if (paymentIntentId != null) {
                PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
                chargeId = pi.getLatestCharge();
            }
        }

        if (chargeId == null) {
            logger.error("No charge ID found for failed invoice {}", invoice.getId());
            throw new PaymentChargeNotFoundException("Customer payment charge can't be retrieved");
        }

        Charge charge = Charge.retrieve(chargeId);
        if (charge == null) {
            logger.error("Charge {} could not be retrieved", chargeId);
            throw new PaymentChargeNotFoundException("Customer payment charge can't be retrieved");
        }

        // Store PaymentFailed info
        try {
            paymentFailed.setCustomer(invoice.getCustomer());
            if (charge.getBillingDetails() != null) {
                paymentFailed.setEmail(charge.getBillingDetails().getEmail());
                paymentFailed.setName(charge.getBillingDetails().getName());
            }
            paymentFailed.setStatus(charge.getStatus());
            if (charge.getOutcome() != null) {
                paymentFailed.setReason(charge.getOutcome().getReason());
            }
            paymentFailed.setFailure_code(charge.getFailureCode());

            paymentFailedService.toStore(paymentFailed);

        } catch (Exception e) {
            logger.error("Failed to store PaymentFailed: {}", e.getMessage(), e);
        }

        logger.info("PaymentMethod ID from Charge: {}", charge.getPaymentMethod());

        if (charge.getPaymentMethod() != null) {
            PaymentMethod paymentMethod = retrievePaymentMethod(charge.getPaymentMethod());

            if (paymentMethod != null) {
                // Save latest card details
                webhookService.saveCardDetails(paymentMethod, userDetail, customer.getId(), cardDetails);

                // Resolve product by id
                Product product = null;
                if (productId != null) {
                    product = Product.retrieve(productId);
                }

                if (product == null) {
                    logger.error("Product can't be retrieved for customer {}", customer.getId());
                    throw new ProductNotFoundException("Product can't be retrieved for customer");
                }

                try {
                    if (paymentMethod.getId() != null) {
                        transaction.setUserId(userDetail.getId());
                        transaction.setProductName(product.getName());
                        transaction.setCard(paymentMethod.getCard().getLast4());

                        long priceValue = (lineAmount != null) ? lineAmount : invoice.getAmountDue();
                        transaction.setPrice(priceValue);

                        transaction.setPaymentMethod(paymentMethod.getType());
                        transaction.setChargeRequest_status(charge.getStatus());
                        transaction.setInvoice_status(invoice.getStatus());
                        transaction.setReceiptUrl(""); // no receipt for failed payment
                        transaction.setCustomerId(invoice.getCustomer());
                        transaction.setEmail(userDetail.getEmail());
                        transaction.setPhoneNumber(userDetail.getPhoneNumber());
                        transaction.setProductId(productId);
                        transaction.setTransactionId(charge.getId());
                        transaction.setInterval(interval);

                        transactionService.storeHistory(transaction);

                        // Log DTO as JSON
                        TransactionDto dto = transaction.toLogDTO();
                        objectMapper.registerModule(new JavaTimeModule());
                        try {
                            String paymentFailedJson = objectMapper.writeValueAsString(dto);
                            logger.info("PaymentFailed Transaction details: {}", paymentFailedJson);
                        } catch (JsonProcessingException e) {
                            logger.error("Error converting TransactionDTO to JSON", e);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to store transaction details for charge {}: {}", charge.getId(), e.getMessage(), e);
                }
            }
        }
    }

    private PaymentMethod retrievePaymentMethod(String paymentMethodId) {
        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            if (paymentMethod == null) {
                logger.error("Customer payment method can't be retrieved!");
                throw new PaymentMethodNotFoundException("Customer payment method can't be retrieved!");
            }
            return paymentMethod;
        } catch (StripeException e) {
            logger.error("Error retrieving payment method: {}", e.getMessage());
            return null;
        }
    }
}

































//package com.mlbeez.feeder.errorlogservice;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.mlbeez.feeder.model.*;
//import com.mlbeez.feeder.repository.AspNetUserRepository;
//import com.mlbeez.feeder.service.PaymentFailedService;
//import com.mlbeez.feeder.service.TransactionService;
//import com.mlbeez.feeder.service.WebhookService;
//import com.mlbeez.feeder.service.exception.*;
//import com.stripe.exception.StripeException;
//import com.stripe.model.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//
//@Service
//public class InvoicePaymentFailedService {
//
//    private final PaymentFailedService paymentFailedService;
//
//    private final AspNetUserRepository aspNetUserRepository;
//
//    private final TransactionService transactionService;
//
//    private final WebhookService webhookService;
//
//    private final Logger logger= LoggerFactory.getLogger(InvoicePaymentFailedService.class);
//
//    public InvoicePaymentFailedService(PaymentFailedService paymentFailedService, AspNetUserRepository aspNetUserRepository, TransactionService transactionService, WebhookService webhookService) {
//        this.paymentFailedService = paymentFailedService;
//        this.aspNetUserRepository = aspNetUserRepository;
//        this.transactionService = transactionService;
//        this.webhookService = webhookService;
//    }
//
//    public void handleInvoicePaymentFailed(Invoice invoice, CardDetails cardDetails,PaymentFailed paymentFailed,Transactions transaction) throws StripeException {
//        if (invoice == null) {
//            logger.error("Invoice is null in invoice.payment_failed event");
//            throw new InvoiceNotFoundException("Invoice is missing");
//        }
//        Customer customer = Customer.retrieve(invoice.getCustomer());
//
//        if (customer == null) {
//            logger.error("Customer not found");
//            throw new CustomerNotFoundException("Customer not found");
//        }
//        UserResponseBaseModel userDetail=aspNetUserRepository.findById(customer.getMetadata().get("userId")).orElseThrow(()->new UserNotFoundException("User not found!"));
//
//        Charge charge = Charge.retrieve(invoice.getCharge());
//
//        if(charge == null){
//            logger.error("customer payment charge can't retrieve for customerId: {}", customer.getId());
//            throw new PaymentChargeNotFoundException("customer payment charge can't retrieve");
//        }
//
//        try {
//            paymentFailed.setCustomer(invoice.getCustomer());
//            paymentFailed.setEmail(charge.getBillingDetails().getEmail());
//            paymentFailed.setName(charge.getBillingDetails().getName());
//            paymentFailed.setStatus(charge.getStatus());
//            paymentFailed.setReason(charge.getOutcome().getReason());
//            paymentFailed.setFailure_code(charge.getFailureCode());
//            paymentFailedService.toStore(paymentFailed);
//
//        } catch (Exception e) {
//            logger.error("Failed to store paymentFailed: {}", e.getMessage(), e);
//        }
//
//        logger.info("PaymentMethod ID from PaymentIntent: {}", charge.getPaymentMethod());
//
//        if (charge.getPaymentMethod() != null) {
//            PaymentMethod paymentMethod = retrievePaymentMethod(charge.getPaymentMethod());
//            if (paymentMethod != null) {
//                webhookService. saveCardDetails(paymentMethod, userDetail, customer.getId(), cardDetails);
//                String productId = invoice.getLines().getData().get(0).getPlan().getProduct();
//
//                Product product = Product.retrieve(productId);
//                if(product == null){
//                    logger.error("product can't retrieve for customer: {}",customer.getId());
//                    throw new ProductNotFoundException("product can't retrieve for customer");
//                }
//
//                try{
//                    if (paymentMethod.getId() != null) {
//                        transaction.setUserId(userDetail.getId());
//                        transaction.setProductName(product.getName());
//                        transaction.setCard(paymentMethod.getCard().getLast4());
//                        transaction.setPrice(invoice.getLines().getData().get(0).getPlan().getAmount());
//                        transaction.setPaymentMethod(paymentMethod.getType());
//                        transaction.setChargeRequest_status(charge.getStatus());
//                        transaction.setInvoice_status(invoice.getStatus());
//                        transaction.setReceiptUrl("");
//                        transaction.setCustomerId(invoice.getCustomer());
//                        transaction.setEmail(userDetail.getEmail());
//                        transaction.setPhoneNumber(userDetail.getPhoneNumber());
//                        transaction.setProductId(productId);
//                        transaction.setTransactionId(charge.getId());
//                        transaction.setInterval(invoice.getLines().getData().get(0).getPrice().getRecurring().getInterval());
//
//                        transactionService.storeHistory(transaction);
//
//                        TransactionDto dto=transaction.toLogDTO();
//                        ObjectMapper objectMapper=new ObjectMapper();
//                        objectMapper.registerModule(new JavaTimeModule());
//                        try{
//                            String paymentFailedJson=objectMapper.writeValueAsString(dto);
//                            logger.info("PaymentFailed entity details: {}",paymentFailedJson);
//                        }catch (JsonProcessingException e)
//                        {
//                            logger.error("Error converting PaymentFailed object to JSON", e);
//                        }
//                    }
//                }
//                catch (Exception e){
//                    logger.error("Failed to store transaction details for charge ID: {}. Error: {}", charge.getId(), e.getMessage(), e);
//                }
//
//            }
//        }
//    }
//
//    private PaymentMethod retrievePaymentMethod(String paymentMethodId) {
//        PaymentMethod paymentMethod = null;
//        try {
//            paymentMethod = PaymentMethod.retrieve(paymentMethodId);
//            if(paymentMethod == null){
//                logger.error("Customer payment method can't retrieved!");
//                throw new PaymentMethodNotFoundException("Customer payment method can't retrieved!");
//            }
//        } catch (StripeException e) {
//            logger.error("Error retrieving payment method: {}", e.getMessage());
//        }
//        return paymentMethod;
//    }
//
//}
