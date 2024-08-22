package com.mlbeez.feeder.controller;

import com.google.gson.JsonSyntaxException;
import com.mlbeez.feeder.model.*;
import com.mlbeez.feeder.repository.*;
import com.mlbeez.feeder.service.InsurancePaymentService;
import com.mlbeez.feeder.service.PaymentFailedService;
import com.mlbeez.feeder.service.TransactionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
public class WebhookController {
    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Autowired
    InsurancePaymentRepository insurancePaymentRepository;

    @Autowired
    CardDetailsRepository cardDetailsRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TransactionService transactionService;

    @Autowired
    PaymentFailedService paymentFailedService;

    @Autowired
    InsurancePaymentService insurancePaymentService;

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) throws StripeException {

        Event event;

        try {
            event = Webhook.constructEvent(
                    payload, sigHeader, endpointSecret
            );
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (JsonSyntaxException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid JSON syntax");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }

        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event);
                break;
            case "invoice.payment_succeeded":
                handleInvoicePaymentSucceeded(event);
                break;
            case "customer.subscription.deleted":
                handleSubscriptionDeleted(event);
                break;
            case "invoice.payment_failed":
                handleInvoicePaymentFailed(event);
                break;
            case "invoice.payment_action_required":
                handleInvoicePaymentActionRequired(event);
                break;
        }

        return ResponseEntity.ok("Webhook received");
    }

    private void handleCheckoutSessionCompleted(Event event) throws StripeException {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) {
            logger.error("Session is null in checkout.session.completed event");
            return;
        }
        Subscription subscription = Subscription.retrieve(session.getSubscription());

        try {
            User userDetail = userRepository.findByCustomerId(session.getCustomer());
            if (userDetail == null) {
                logger.error("User not found for customerId: {}", session.getCustomer());
                return;
            }

            // Save card details when payment succeeds
            String paymentMethodId = subscription.getDefaultPaymentMethod();
            if (paymentMethodId != null) {
                PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

                // Store card details in the CardDetails entity
                CardDetails cardDetails = new CardDetails();
                cardDetails.setPayment_methodId(paymentMethod.getId());
                cardDetails.setCardBrand(paymentMethod.getCard().getBrand());
                cardDetails.setCard_Last4(paymentMethod.getCard().getLast4());
                cardDetails.setExp_month(paymentMethod.getCard().getExpMonth());
                cardDetails.setExp_year(paymentMethod.getCard().getExpYear());
                cardDetails.setCountry(paymentMethod.getCard().getCountry());
                cardDetails.setFunding(paymentMethod.getCard().getFunding());
                cardDetails.setCustomer(paymentMethod.getCustomer());
                cardDetails.setType(paymentMethod.getType());
                cardDetails.setEmail(paymentMethod.getBillingDetails().getEmail());
                cardDetails.setName(paymentMethod.getBillingDetails().getName());

                // Save card details to the database
                cardDetailsRepository.save(cardDetails);
            }


        } catch (Exception e) {
            logger.error("Error processing checkout session completed event: {}", e.getMessage(), e);
        }
    }

    private void handleInvoicePaymentSucceeded(Event event) throws StripeException {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
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

            InsurancePayment insurancePayment = new InsurancePayment();
            insurancePayment.setSubscriptionId(subscription.getId());
            insurancePayment.setUserId(userDetail.getUserId());
            insurancePayment.setDefault_payment_method(subscription.getDefaultPaymentMethod());
            String productId = subscription.getItems().getData().get(0).getPrice().getProduct();
            insurancePayment.setProductId(productId);
            insurancePayment.setEmail(invoice.getCustomerEmail());
            insurancePayment.setName(invoice.getCustomerName());
            insurancePayment.setPhoneNumber(userDetail.getPhoneNumber());
            insurancePayment.setCustomer(invoice.getCustomer());
            insurancePayment.setPayment_status(invoice.getStatus());
            insurancePayment.setAmount(invoice.getAmountPaid());
            insurancePayment.setInvoiceId(invoice.getId());
            insurancePayment.setCurrency(invoice.getCurrency());
            insurancePayment.setStatus(subscription.getStatus());
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
                transaction.setStatus(invoice.getStatus());
                transaction.setTransactionId(chargeId);
                transaction.setInterval(subscription.getItems().getData().get(0).getPrice().getRecurring().getInterval());

                transactionService.storeHistory(transaction);
            }
        } catch (Exception e) {
            logger.error("Error processing checkout session completed event: {}", e.getMessage(), e);
        }
    }
    private void handleSubscriptionDeleted(Event event) {
        Map<String, Object> data = (Map<String, Object>) event.getData().getObject();
        String customerId = (String) data.get("customer");

        // Update the insurance payment status to "cancelled"
        InsurancePayment existingPayment = insurancePaymentRepository.findByCustomer(customerId);
        if (existingPayment != null) {
            existingPayment.setStatus("cancelled");
            insurancePaymentService.updatePayment(existingPayment);
        } else {
            logger.error("No insurance payment found for customerId: {}", customerId);
        }
    }
    private void handleInvoicePaymentFailed(Event event) throws StripeException {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
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
        PaymentMethod paymentMethod = PaymentMethod.retrieve(charge.getPaymentMethod());

        String productId = invoice.getLines().getData().get(0).getPlan().getProduct();

        Product product = Product.retrieve(productId);

        if (paymentMethod.getCard() != null) {
            try {
                CardDetails cardDetails = new CardDetails();
                cardDetails.setPayment_methodId(paymentMethod.getId());
                cardDetails.setCardBrand(paymentMethod.getCard().getBrand());
                cardDetails.setCard_Last4(paymentMethod.getCard().getLast4());
                cardDetails.setExp_month(paymentMethod.getCard().getExpMonth());
                cardDetails.setExp_year(paymentMethod.getCard().getExpYear());
                cardDetails.setCountry(paymentMethod.getCard().getCountry());
                cardDetails.setFunding(paymentMethod.getCard().getFunding());
                cardDetails.setCustomer(invoice.getCustomer());
                cardDetails.setType(paymentMethod.getType());
                cardDetails.setEmail(paymentMethod.getBillingDetails().getEmail());
                cardDetails.setName(paymentMethod.getBillingDetails().getName());

                // Save card details to repository
                cardDetailsRepository.save(cardDetails);

            } catch (Exception e) {
                logger.error("Failed to store cardDetails: {}", e.getMessage(), e);
            }

            if (paymentMethod.getId() != null) {
                Transactions transaction = new Transactions();
                transaction.setUserId(userDetail.getUserId());
                transaction.setProductName(product.getName());
                transaction.setCard(paymentMethod.getCard().getLast4());
                transaction.getCreatedAt();
                transaction.setPrice(invoice.getLines().getData().get(0).getPlan().getAmount());
                transaction.setPaymentMethod(paymentMethod.getType());
                transaction.setStatus(charge.getStatus());
                transaction.setReceiptUrl("");
                transaction.setCustomerId(invoice.getCustomer());
                transaction.setPhoneNumber(userDetail.getPhoneNumber());
                transaction.setProductId(productId);
                transaction.setTransactionId(charge.getId());
                transaction.setInterval(invoice.getLines().getData().get(0).getPrice().getRecurring().getInterval());

                transactionService.storeHistory(transaction);
            }
        } else {
            logger.warn("Payment method has no card details for payment method ID: {}", paymentMethod.getId());
        }
    }
    private void handleInvoicePaymentActionRequired(Event event) throws StripeException {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        if (invoice == null) {
            logger.error("Invoice is null in invoice.payment_action_required event");
            return;
        }

        // Retrieve the Charge associated with the invoice
        Charge charge = Charge.retrieve(invoice.getCharge());

        // Log the payment method ID
        logger.info("PaymentMethod ID from PaymentIntent: {}", charge.getPaymentMethod());

        // Retrieve the PaymentMethod
        PaymentMethod paymentMethod = PaymentMethod.retrieve(charge.getPaymentMethod());

        // Handle the PaymentMethod details
        if (paymentMethod.getCard() != null) {
            try {
                CardDetails cardDetails = new CardDetails();
                cardDetails.setPayment_methodId(paymentMethod.getId());
                cardDetails.setCardBrand(paymentMethod.getCard().getBrand());
                cardDetails.setCard_Last4(paymentMethod.getCard().getLast4());
                cardDetails.setExp_month(paymentMethod.getCard().getExpMonth());
                cardDetails.setExp_year(paymentMethod.getCard().getExpYear());
                cardDetails.setCountry(paymentMethod.getCard().getCountry());
                cardDetails.setFunding(paymentMethod.getCard().getFunding());
                cardDetails.setCustomer(invoice.getCustomer());
                cardDetails.setType(paymentMethod.getType());
                cardDetails.setEmail(paymentMethod.getBillingDetails().getEmail());
                cardDetails.setName(paymentMethod.getBillingDetails().getName());

                // Save card details to repository
                cardDetailsRepository.save(cardDetails);

            } catch (Exception e) {
                logger.error("Failed to store cardDetails: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("Payment method has no card details for payment method ID: {}", paymentMethod.getId());
        }
    }
}