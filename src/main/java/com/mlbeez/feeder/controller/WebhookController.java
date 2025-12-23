package com.mlbeez.feeder.controller;

import com.google.gson.JsonSyntaxException;
import com.mlbeez.feeder.errorlogservice.InvoicePaymentFailedService;
import com.mlbeez.feeder.model.CardDetails;
import com.mlbeez.feeder.model.InsurancePayment;
import com.mlbeez.feeder.model.PaymentFailed;
import com.mlbeez.feeder.model.Transactions;
import com.mlbeez.feeder.service.WebhookEventService;
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



@RestController
public class WebhookController {

//    private final WebhookService webhookService;

//    private final InvoicePaymentFailedService invoicePaymentFailedService;

    private final WebhookEventService webhookEventService;

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    public WebhookController(WebhookService webhookService, InvoicePaymentFailedService invoicePaymentFailedService, WebhookEventService webhookEventService) {
//        this.webhookService = webhookService;
//        this.invoicePaymentFailedService = invoicePaymentFailedService;
        this.webhookEventService = webhookEventService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader, InsurancePayment insurancePayment, Transactions transactions, CardDetails cardDetails,
            PaymentFailed paymentFailed
            ) throws StripeException {

        logger.info("Requested to webhook listening");
        String response = webhookEventService.handleWebhookEvent(payload,sigHeader,insurancePayment,transactions,cardDetails,paymentFailed);

//        Event event;
//
//        try {
//            event = Webhook.constructEvent(
//                    payload, sigHeader, endpointSecret
//            );
//        } catch (SignatureVerificationException e) {
//            logger.error("Signature verification failed for payload: {} and signature header: {}", payload, sigHeader);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
//        } catch (JsonSyntaxException e) {
//            logger.error("Invalid JSON syntax in payload: {}", payload, e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid JSON syntax");
//        } catch (Exception e) {
//            logger.error("Internal server error while processing webhook: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
//        }
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



        return ResponseEntity.ok(response);
    }

//    private void handleChargeSucceeded(Event event, CardDetails cardDetails) {
//        logger.info("Requested to handleChargeSucceeded");
//        Charge charge= (Charge) event.getDataObjectDeserializer().getObject().orElse(null);
//        webhookService.handleChargeSucceeded(charge,cardDetails);
//    }
//
//    private void handleCheckoutSessionCompleted(Event event){
//        logger.info("Requested to handleCheckoutSessionCompleted");
//        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
//        webhookService.handleCheckoutSessionCompleted(session);
//    }
//
//    private void handleInvoicePaymentSucceeded(Event event, InsurancePayment insurancePayment,Transactions transactions) {
//        logger.info("Requested to handleInvoicePaymentSucceeded");
//        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
//        webhookService.handleInvoicePaymentSucceeded(invoice,insurancePayment,transactions);
//    }
//
//    private void handleInvoicePaymentFailed(Event event, CardDetails cardDetails,PaymentFailed paymentFailed,Transactions transactions) throws StripeException {
//        logger.info("Requested to handleInvoicePaymentFailed");
//        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
//        invoicePaymentFailedService.handleInvoicePaymentFailed(invoice,cardDetails,paymentFailed,transactions);
//    }
//
//    private void handleCustomerSubscriptionDeleted(Event event){
//        logger.info("Requested to handleCustomerSubscriptionDeleted");
//        Subscription subscription=(Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
//        if (subscription != null) {
//            webhookService.handleCustomerSubscriptionDeleted(subscription);
//        }
//    }
}