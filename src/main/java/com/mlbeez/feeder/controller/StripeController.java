package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.InsurancePayment;
import com.mlbeez.feeder.repository.InsurancePaymentRepository;
import com.mlbeez.feeder.service.CheckoutService;
import com.mlbeez.feeder.service.InsurancePaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionCancelParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;



@RestController
public class StripeController {

    @Autowired
    private InsurancePaymentRepository insurancePaymentRepository;

    @Autowired
    CheckoutService checkoutService;

    @Autowired
    InsurancePaymentService insurancePaymentService;

    private static final Logger logger= LoggerFactory.getLogger(StripeController.class);

    @PostMapping("/create-checkout-session")
    public Map<String, String> createCheckoutSession(@RequestBody Map<String, String> Details) {
        logger.info("Requested to create stripe checkout page");
        Map<String, String> responseData = new HashMap<>();

        try {
            responseData = checkoutService.createCheckoutSession(Details);
        } catch (Exception e) {
            responseData.put("error", e.getMessage());
        }

        return responseData;
    }


    @DeleteMapping("subscriptions/{id}")
    public ResponseEntity<String> cancelSubscription(@PathVariable("id") String id) throws StripeException {
        // Retrieve the subscription
        Subscription resource = Subscription.retrieve(id);

        // Cancel the subscription
        SubscriptionCancelParams params = SubscriptionCancelParams.builder().build();
        Subscription subscription = resource.cancel(params);

        // Retrieve the customer ID from the canceled subscription
        String customerId = subscription.getCustomer();

        // Update the insurance payment status to "cancelled"
        InsurancePayment existingPayment = insurancePaymentRepository.findByCustomer(customerId);
        if (existingPayment != null) {
            existingPayment.setSubscription_Status("cancelled");
            insurancePaymentService.updatePayment(existingPayment);
            return ResponseEntity.ok("Subscription and insurance payment status updated to cancelled.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Insurance payment record not found.");
        }
    }
}