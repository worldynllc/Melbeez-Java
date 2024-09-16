package com.mlbeez.feeder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mlbeez.feeder.model.*;
import com.mlbeez.feeder.repository.CardDetailsRepository;
import com.mlbeez.feeder.repository.InsurancePaymentRepository;
import com.mlbeez.feeder.repository.UserRepository;
import com.mlbeez.feeder.repository.WarrantyRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WebhookService {

    private static final Logger logger= LoggerFactory.getLogger(WebhookService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentFailedService paymentFailedService;

    @Autowired
    private CardDetailsRepository cardDetailsRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WarrantyRepository warrantyRepository;

    @Autowired
    private InsurancePaymentService insurancePaymentService;
    @Autowired
    private InsurancePaymentRepository insurancePaymentRepository;

    public void handleInvoicePaymentFailed(Invoice invoice) throws StripeException {
        if (invoice == null) {
            logger.error("Invoice is null in invoice.payment_failed event");
            return;
        }
        User userDetail = userRepository.findByCustomerId(invoice.getCustomer());
        if (userDetail == null) {
            logger.error("User not found for customerId: {}", invoice.getCustomer());
            return;
        }
        Charge charge = Charge.retrieve(invoice.getCharge());
        try {
            PaymentFailed paymentFailed = new PaymentFailed();
            paymentFailed.setCustomer(invoice.getCustomer());
            paymentFailed.setEmail(charge.getBillingDetails().getEmail());
            paymentFailed.setName(charge.getBillingDetails().getName());
            paymentFailed.setStatus(charge.getStatus());
            paymentFailed.setReason(charge.getOutcome().getReason());
            paymentFailed.setFailure_code(charge.getFailureCode());
            // Store payment failed details
            paymentFailedService.toStore(paymentFailed);

        } catch (Exception e) {
            logger.error("Failed to store paymentFailed: {}", e.getMessage(), e);
        }

        logger.info("PaymentMethod ID from PaymentIntent: {}", charge.getPaymentMethod());

        if (charge.getPaymentMethod() != null) {
            PaymentMethod paymentMethod = retrievePaymentMethod(charge.getPaymentMethod());
            if (paymentMethod != null) {
                saveCardDetails(paymentMethod, userDetail);
                String productId = invoice.getLines().getData().get(0).getPlan().getProduct();

                Product product = Product.retrieve(productId);

                try{
                    if (paymentMethod.getId() != null) {
                        Transactions transaction = new Transactions();
                        transaction.setUserId(userDetail.getUserId());
                        transaction.setProductName(product.getName());
                        transaction.setCard(paymentMethod.getCard().getLast4());
                        transaction.getCreatedAt();
                        transaction.setPrice(invoice.getLines().getData().get(0).getPlan().getAmount());
                        transaction.setPaymentMethod(paymentMethod.getType());
                        transaction.setChargeRequest_status(charge.getStatus());
                        transaction.setInvoice_status(invoice.getStatus());
                        transaction.setReceiptUrl("");
                        transaction.setCustomerId(invoice.getCustomer());
                        transaction.setEmail(userDetail.getEmail());
                        transaction.setPhoneNumber(userDetail.getPhoneNumber());
                        transaction.setProductId(productId);
                        transaction.setTransactionId(charge.getId());
                        transaction.setInterval(invoice.getLines().getData().get(0).getPrice().getRecurring().getInterval());

                        transactionService.storeHistory(transaction);

                        TransactionDto dto=transaction.toLogDTO();
                        ObjectMapper objectMapper=new ObjectMapper();
                        objectMapper.registerModule(new JavaTimeModule());
                        try{
                            String paymentFailedJson=objectMapper.writeValueAsString(dto);
                            logger.error("PaymentFailed entity details: {}",paymentFailedJson);
                        }catch (JsonProcessingException e)
                        {
                            logger.error("Error converting PaymentFailed object to JSON", e);
                        }
                    }
                }
                catch (Exception e){
                    logger.error("Failed to store transaction details for charge ID: {}. Error: {}", charge.getId(), e.getMessage(), e);
                }

            }
        }
    }

    public void handleInvoicePaymentSucceeded(Invoice invoice){
        if (invoice == null) {
            logger.error("Invoice is null in invoice.payment_succeeded event");
            return;
        }

        try {
            User userDetail = userRepository.findByCustomerId(invoice.getCustomer());
            if (userDetail == null) {
                logger.error("User not found for customerId: {}", invoice.getCustomer());
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

            Charge charge=Charge.retrieve(invoice.getCharge());

            Warranty warranty = warrantyOptional.get();
            String productId = subscription.getItems().getData().get(0).getPrice().getProduct();

            InsurancePayment insurancePayment = new InsurancePayment();
            insurancePayment.setSubscriptionId(subscription.getId());
            insurancePayment.setUserId(userDetail.getUserId());
            insurancePayment.setDefault_payment_method(paymentMethodId);
            insurancePayment.setProductId(productId);
            insurancePayment.setEmail(invoice.getCustomerEmail());
            insurancePayment.setName(invoice.getCustomerName());
            insurancePayment.setPhoneNumber(userDetail.getPhoneNumber());
            insurancePayment.setCustomer(invoice.getCustomer());
            insurancePayment.setInvoice_status(invoice.getStatus());
            insurancePayment.setWarrantyId(warranty.getWarrantyId());
            insurancePayment.setAmount(invoice.getAmountPaid());
            insurancePayment.setInvoiceId(invoice.getId());
            insurancePayment.setCurrency(invoice.getCurrency());
            insurancePayment.setSubscription_Status(subscription.getStatus());
            insurancePayment.setChargeRequest_status(charge.getStatus());
            insurancePayment.setMode(invoice.getLines().getData().get(0).getType());
            insurancePaymentService.storePayment(insurancePayment);

            // Retrieve the PaymentIntent from the Invoice
            String paymentIntentId = invoice.getPaymentIntent();
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            // Retrieve the Charge from the PaymentIntent
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
                transaction.getCreatedAt();
                transaction.setPrice(invoice.getAmountPaid());
                transaction.setReceiptUrl(receiptUrl);
                transaction.setPhoneNumber(userDetail.getPhoneNumber());
                transaction.setPaymentMethod(paymentMethod.getType());
                transaction.setInvoice_status(invoice.getStatus());
                transaction.setChargeRequest_status(charge.getStatus());
                transaction.setTransactionId(chargeId);
                transaction.setInterval(subscription.getItems().getData().get(0).getPrice().getRecurring().getInterval());

                transactionService.storeHistory(transaction);
            }

        } catch (Exception e) {
            logger.error("Error processing invoice.payment_succeeded event: {}", e.getMessage(), e);
        }
    }

    public void handleCheckoutSessionCompleted(Session session){
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
        }
        catch (Exception e){
            logger.error("StripeException while retrieving subscription for session {}: {}",session.getId(),
                    e.getMessage(),e);
            return;
        }
        User userDetail;
        try {
            userDetail = userRepository.findByCustomerId(session.getCustomer());
            if (userDetail == null) {
                logger.error("User not found for customerId: {}", session.getCustomer());
                return;
            }
        } catch (Exception e) {
            logger.error("Exception while fetching user details for customerId {}: {}", session.getCustomer(), e.getMessage(), e);
            return;
        }

        // Extract and save card details
        String paymentMethodId = subscription.getDefaultPaymentMethod();
        if (paymentMethodId != null) {

            PaymentMethod paymentMethod;
            try{
                paymentMethod=PaymentMethod.retrieve(paymentMethodId);
                if(paymentMethod!=null)
                {
                    saveCardDetails(paymentMethod, userDetail);
                }
                else {
                    logger.error("Payment method is null for paymentMethodId: {}, paymentMethodId",paymentMethodId);
                }
            }
            catch (StripeException e){
                logger.error("StripeException while retrieving payment method for paymentMethodId {}: {}", paymentMethodId, e.getMessage(),e);
            }
            catch (Exception e){
                logger.error("Exception while saving card details for paymentMethodId {}: {}",paymentMethodId, e.getMessage(),e);
            }

        }
    }

    public void handleSubscriptionDeleted(String customerId){
        // Update the insurance payment status to "cancelled"
        InsurancePayment existingPayment = insurancePaymentRepository.findByCustomer(customerId);
        if (existingPayment != null) {
            existingPayment.setSubscription_Status("cancelled");
            insurancePaymentService.updatePayment(existingPayment);
        } else {
            logger.error("No insurance payment found for customerId: {}", customerId);
        }
    }

    private PaymentMethod retrievePaymentMethod(String paymentMethodId) {
        try {
            return PaymentMethod.retrieve(paymentMethodId);
        } catch (StripeException e) {
            logger.error("Error retrieving payment method: {}", e.getMessage());
            return null;
        }
    }

    private Subscription retrieveSubscription(String subscriptionId) {
        try {
            return Subscription.retrieve(subscriptionId);
        } catch (StripeException e) {
            logger.error("Error retrieving subscription: {}", e.getMessage());
            return null;
        }
    }

    private void saveCardDetails(PaymentMethod paymentMethod, User userDetail) {
        try {
            CardDetails cardDetails = new CardDetails();
            cardDetails.setPayment_methodId(paymentMethod.getId());
            cardDetails.setCardBrand(paymentMethod.getCard().getBrand());
            cardDetails.setCard_Last4(paymentMethod.getCard().getLast4());
            cardDetails.setExp_month(paymentMethod.getCard().getExpMonth());
            cardDetails.setExp_year(paymentMethod.getCard().getExpYear());
            cardDetails.setCountry(paymentMethod.getCard().getCountry());
            cardDetails.setFunding(paymentMethod.getCard().getFunding());
            cardDetails.setCustomer(userDetail.getCustomerId());
            cardDetails.setType(paymentMethod.getType());
            cardDetails.setEmail(paymentMethod.getBillingDetails().getEmail());
            cardDetails.setName(paymentMethod.getBillingDetails().getName());

            cardDetailsRepository.save(cardDetails);
        } catch (Exception e) {
            logger.error("Failed to save card details: {}", e.getMessage(), e);

        }
    }
}

