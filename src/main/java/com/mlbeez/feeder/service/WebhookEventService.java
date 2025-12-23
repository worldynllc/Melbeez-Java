package com.mlbeez.feeder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlbeez.feeder.errorlogservice.InvoicePaymentFailedService;
import com.mlbeez.feeder.model.*;
import com.mlbeez.feeder.service.exception.InternalServerException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebhookEventService {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final WebhookService webhookService;
    private final InvoicePaymentFailedService invoicePaymentFailedService;

    private static final Logger logger = LoggerFactory.getLogger(WebhookEventService.class);

    public WebhookEventService(WebhookService webhookService,
                               InvoicePaymentFailedService invoicePaymentFailedService) {
        this.webhookService = webhookService;
        this.invoicePaymentFailedService = invoicePaymentFailedService;
    }
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


    // ---------------- MAIN HANDLER ----------------------
    public String handleWebhookEvent(
            String payload,
            String sigHeader,
            InsurancePayment insurancePayment,
            Transactions transactions,
            CardDetails cardDetails,
            PaymentFailed paymentFailed
    ) {

        Event event;

        try {
            // Verify signature
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            logger.info("Stripe Webhook Event Received: {}", event.getType());

            switch (event.getType()) {

                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;

                case "charge.succeeded":
                    handleChargeSucceeded(event, cardDetails);
                    break;

                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event, insurancePayment, transactions);
                    break;

                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event, cardDetails, paymentFailed, transactions);
                    break;

                case "customer.subscription.deleted":
                    handleCustomerSubscriptionDeleted(event);
                    break;
            }

        } catch (SignatureVerificationException e) {
            logger.error("Invalid webhook signature");
            throw new com.mlbeez.feeder.service.exception.SignatureVerificationException("Invalid signature");

        } catch (Exception e) {
            logger.error("Webhook processing failed: {}", e.getMessage());
            throw new InternalServerException("Internal webhook processing error");
        }

        return "Webhook received";
    }

    // ---------------- EVENT HANDLERS ---------------------

    private void handleChargeSucceeded(Event event, CardDetails cardDetails) {
        logger.info("Processing charge.succeeded");

        try {
            JsonNode root = getRoot(event);

            String chargeId = root.get("id").asText();
            com.stripe.model.Charge charge = com.stripe.model.Charge.retrieve(chargeId);

            webhookService.handleChargeSucceeded(charge, cardDetails);

        } catch (Exception e) {
            logger.error("charge.succeeded error: {}", e.getMessage());
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        logger.info("Processing checkout.session.completed");

        try {
            JsonNode root = getRoot(event);

            String sessionId = root.get("id").asText();
            Session session = Session.retrieve(sessionId);

            webhookService.handleCheckoutSessionCompleted(session);

        } catch (Exception e) {
            logger.error("checkout.session.completed error: {}", e.getMessage());
        }
    }

    private void handleInvoicePaymentSucceeded(
            Event event,
            InsurancePayment insurancePayment,
            Transactions transactions
    ) {
        logger.info("Processing invoice.payment_succeeded");

        try {
            JsonNode root = getRoot(event);

            String invoiceId = root.get("id").asText();
            Invoice invoice = Invoice.retrieve(invoiceId);

            webhookService.handleInvoicePaymentSucceeded(invoice, insurancePayment, transactions, event);

        } catch (Exception e) {
            logger.error("invoice.payment_succeeded error: {}", e.getMessage());
        }
    }

    private void handleInvoicePaymentFailed(
            Event event,
            CardDetails cardDetails,
            PaymentFailed paymentFailed,
            Transactions transactions
    ) {
        logger.info("Processing invoice.payment_failed");

        try {
            JsonNode root = getRoot(event);

            String invoiceId = root.get("id").asText();
            Invoice invoice = Invoice.retrieve(invoiceId);

            invoicePaymentFailedService.handleInvoicePaymentFailed(invoice,event,
                    cardDetails, paymentFailed, transactions
            );

        } catch (Exception e) {
            logger.error("invoice.payment_failed error: {}", e.getMessage());
        }
    }


    private void handleCustomerSubscriptionDeleted(Event event) {
        logger.info("Processing customer.subscription.deleted");

        try {
            JsonNode root = getRoot(event);

            String subscriptionId = root.get("id").asText();
            Subscription sub = Subscription.retrieve(subscriptionId);

            webhookService.handleCustomerSubscriptionDeleted(sub);

        } catch (Exception e) {
            logger.error("customer.subscription.deleted error: {}", e.getMessage());
        }
    }
}









































//package com.mlbeez.feeder.service;
//
//import com.google.gson.JsonObject;
//import com.google.gson.JsonSyntaxException;
//import com.mlbeez.feeder.errorlogservice.InvoicePaymentFailedService;
//import com.mlbeez.feeder.model.CardDetails;
//import com.mlbeez.feeder.model.InsurancePayment;
//import com.mlbeez.feeder.model.PaymentFailed;
//import com.mlbeez.feeder.model.Transactions;
//import com.mlbeez.feeder.service.exception.InternalServerException;
//import com.stripe.exception.EventDataObjectDeserializationException;
//import com.stripe.exception.SignatureVerificationException;
//import com.stripe.exception.StripeException;
//import com.stripe.model.*;
//import com.stripe.model.checkout.Session;
//import com.stripe.net.ApiResource;
//import com.stripe.net.Webhook;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//
//@Service
//public class WebhookEventService {
//
//    @Value("${stripe.webhook.secret}")
//    private String webhookSecret;
//
//    private final WebhookService webhookService;
//
//    private final InvoicePaymentFailedService invoicePaymentFailedService;
//
//    private static final Logger logger = LoggerFactory.getLogger(WebhookEventService.class);
//
//    public WebhookEventService(WebhookService webhookService, InvoicePaymentFailedService invoicePaymentFailedService) {
//        this.webhookService = webhookService;
//        this.invoicePaymentFailedService = invoicePaymentFailedService;
//    }
//
//    public String handleWebhookEvent(String payload,
//                                     String sigHeader, InsurancePayment insurancePayment, Transactions transactions, CardDetails cardDetails,
//                                     PaymentFailed paymentFailed){
//
//        Event event;
//
//        try {
//            event = Webhook.constructEvent(
//                    payload, sigHeader, webhookSecret
//            );
//
//        switch (event.getType()) {
//            case "checkout.session.completed":
//                handleCheckoutSessionCompleted(event);
//                break;
//            case "charge.succeeded":
//                handleChargeSucceeded(event,cardDetails);
//                break;
//            case "invoice.payment_succeeded":
//                handleInvoicePaymentSucceeded(event,insurancePayment,transactions);
//                break;
//            case "invoice.payment_failed":
//                handleInvoicePaymentFailed(event,cardDetails,paymentFailed,transactions);
//                break;
//            case "customer.subscription.deleted":
//                handleCustomerSubscriptionDeleted(event);
//        }
//        } catch (SignatureVerificationException e) {
//            logger.error("Signature verification failed for payload: {} and signature header: {}", payload, sigHeader);
//            throw new com.mlbeez.feeder.service.exception.SignatureVerificationException("Invalid signature");
//        } catch (JsonSyntaxException e) {
//            logger.error("Invalid JSON syntax in payload: {}", payload, e);
//            throw new com.mlbeez.feeder.service.exception.JsonSyntaxException("Invalid JSON syntax");
//        }
//        catch (StripeException e){
//            logger.error("Error : {}",e.getMessage());
//        }catch (Exception e) {
//            logger.error("Internal server error while processing webhook: {}", e.getMessage());
//            throw new InternalServerException("Internal server error");
//        }
//        return "Webhook received";
//
//    }
//
//    private void handleChargeSucceeded(Event event, CardDetails cardDetails) {
//        logger.info("Requested to handleChargeSucceeded");
//        Charge charge= convertEventObject(event, Charge.class);
//        webhookService.handleChargeSucceeded(charge,cardDetails);
//    }
//
//    private void handleCheckoutSessionCompleted(Event event){
//        logger.info("Requested to handleCheckoutSessionCompleted");
//        Session session = convertEventObject(event, Session.class);
//        webhookService.handleCheckoutSessionCompleted(session);
//    }
//
//    private void handleInvoicePaymentSucceeded(Event event, InsurancePayment insurancePayment,Transactions transactions) {
//        logger.info("Requested to handleInvoicePaymentSucceeded");
//        Invoice invoice = convertEventObject(event, Invoice.class);
//        webhookService.handleInvoicePaymentSucceeded(invoice,insurancePayment,transactions,event);
//    }
//
//    private void handleInvoicePaymentFailed(Event event, CardDetails cardDetails,PaymentFailed paymentFailed,Transactions transactions) throws StripeException {
//        logger.info("Requested to handleInvoicePaymentFailed");
//        Invoice invoice =convertEventObject(event, Invoice.class);
//        invoicePaymentFailedService.handleInvoicePaymentFailed(invoice,cardDetails,paymentFailed,transactions);
//    }
//
//    private void handleCustomerSubscriptionDeleted(Event event){
//        logger.info("Requested to handleCustomerSubscriptionDeleted");
//        Subscription subscription=(convertEventObject(event, Subscription.class));
//        if (subscription != null) {
//            webhookService.handleCustomerSubscriptionDeleted(subscription);
//        }
//    }
//
//    private <T> T convertEventObject(Event event, Class<T> clazz) {
//        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
//        StripeObject stripeObject;
//        try {
//            stripeObject = deserializer.getObject()
//                    .orElseGet(() -> {
//                        try {
//                            return deserializer.deserializeUnsafe();
//                        } catch (EventDataObjectDeserializationException e) {
//                            throw new RuntimeException("Failed to deserialize event object", e);
//                        }
//                    });
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to extract Stripe object", e);
//        }
//        String json = ApiResource.GSON.toJson(stripeObject);
//        return ApiResource.GSON.fromJson(json, clazz);
//    }
//
//}
