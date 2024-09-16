package com.mlbeez.feeder.controller;

import com.google.gson.JsonSyntaxException;
import com.mlbeez.feeder.service.WebhookService;
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
import java.util.*;


@RestController
public class WebhookController {
    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Autowired
    private WebhookService webhookService;

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) throws StripeException {

        logger.info("Requested to webhook listening");

        Event event;

        try {
            event = Webhook.constructEvent(
                    payload, sigHeader, endpointSecret
            );
        } catch (SignatureVerificationException e) {
            logger.error("Signature verification failed for payload: {} and signature header: {}", payload, sigHeader, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (JsonSyntaxException e) {
            logger.error("Invalid JSON syntax in payload: {}", payload, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid JSON syntax");
        } catch (Exception e) {
            logger.error("Internal server error while processing webhook: {}", e.getMessage(), e);
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
        }

        return ResponseEntity.ok("Webhook received");
    }

    private void handleCheckoutSessionCompleted(Event event){
        logger.info("Requested to handleCheckoutSessionCompleted");
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        webhookService.handleCheckoutSessionCompleted(session);
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        logger.info("Requested to handleInvoicePaymentSucceeded");
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        webhookService.handleInvoicePaymentSucceeded(invoice);
    }

    private void handleSubscriptionDeleted(Event event) {
        logger.info("Requested to handleSubscriptionDeleted");
        Map<String, Object> data = (Map<String, Object>) event.getData().getObject();
        String customerId = (String) data.get("customer");
        webhookService.handleSubscriptionDeleted(customerId);
    }
    private void handleInvoicePaymentFailed(Event event) throws StripeException {
        logger.info("Requested to handleInvoicePaymentFailed");
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        webhookService.handleInvoicePaymentFailed(invoice);
    }

}