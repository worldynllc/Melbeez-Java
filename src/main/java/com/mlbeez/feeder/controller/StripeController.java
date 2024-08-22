package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.InsurancePayment;
import com.mlbeez.feeder.model.User;
import com.mlbeez.feeder.model.Warranty;
import com.mlbeez.feeder.repository.InsurancePaymentRepository;
import com.mlbeez.feeder.repository.WarrantyRepository;
import com.mlbeez.feeder.service.InsurancePaymentService;
import com.mlbeez.feeder.service.UserService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
public class StripeController {
    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Autowired
    private InsurancePaymentRepository insurancePaymentRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WarrantyRepository warrantyRepository;

    @Autowired
    InsurancePaymentService insurancePaymentService;

    private static final Logger logger = LoggerFactory.getLogger(StripeController.class);

    @PostMapping("/create-checkout-session")
    public Map<String, String> createCheckoutSession(@RequestBody Map<String, String> Details) {

        Stripe.apiKey = stripeApiKey;

        logger.debug("Request to create checkout Stripe");
        String warrantyId = Details.get("warrantyId");
        String userId = Details.get("userId");
        String userName = Details.get("userName");
        String phoneNumber = Details.get("phoneNumber");
        String email = Details.get("email");
        String currency = Details.get("currency");
        String monthlyPriceStr = Details.get("monthlyPrice");
        Float monthlyPrice = Float.parseFloat(monthlyPriceStr);
        Long monthlyPriceLong = (long) (monthlyPrice * 100);
        String subscriptionType = Details.get("subscriptionType");

        Map<String, String> responseData = new HashMap<>();

        Map<String, String> checkData = new HashMap<>();
        try {
            String idempotencyKey = userId + "_" + System.currentTimeMillis();

            // Retrieve or create user
            User user = userService.getOrCreateUser(userId, userName, email, phoneNumber);

            // Check if the warranty exists
            Optional<Warranty> findProduct = warrantyRepository.findByWarrantyId(warrantyId);
            if (findProduct.isEmpty()) {
                responseData.put("error", "Warranty not found.");
                return responseData;
            }
            Warranty warranties;
            warranties= findProduct.get();
            String productId = warranties.getProductId();
            List<InsurancePayment> findSubscriptionList = insurancePaymentRepository.findAllByProductId(productId);

            // Check if any active subscription exists for the user
            for (InsurancePayment subscription : findSubscriptionList) {
                if (subscription != null && subscription.getSubscriptionId() != null && subscription.getUserId().equals(userId)) {
                    Subscription stripeSubscription = Subscription.retrieve(subscription.getSubscriptionId());
                    if ("active".equals(stripeSubscription.getStatus())) {
                        responseData.put("error", "You already have an active subscription for this warranty.");
                        return responseData;
                    }
                }
            }

            // Determine the interval for subscription
            PriceCreateParams.Recurring.Interval interval = "yearly".equals(subscriptionType)
                    ? PriceCreateParams.Recurring.Interval.YEAR
                    : PriceCreateParams.Recurring.Interval.MONTH;

            // Create a new recurring price for the product
            PriceCreateParams priceParams = PriceCreateParams.builder()
                    .setUnitAmount(monthlyPriceLong) // Amount in cents
                    .setCurrency(currency)
                    .setRecurring(
                            PriceCreateParams.Recurring.builder()
                                    .setInterval(interval)
                                    .build()
                    )
                    .setProduct(productId)
                    .build();
            Price price = Price.create(priceParams);

            // Create a checkout session for the subscription
            SessionCreateParams sessionParams = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(user.getCustomerId())
                    .setSuccessUrl("https://preprodjavaapi.melbeez.com/success")
                    .setCancelUrl("https://preprodjavaapi.melbeez.com/cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPrice(price.getId())
                                    .build()
                    ).build();

            // Pass the idempotency key in the API request options
            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();



            Session session = Session.create(sessionParams,requestOptions);

            responseData.put("url", session.getUrl());
            checkData.put("Client", session.getClientSecret());

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
            existingPayment.setStatus("cancelled");
            insurancePaymentService.updatePayment(existingPayment);
            return ResponseEntity.ok("Subscription and insurance payment status updated to cancelled.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Insurance payment record not found.");
        }
    }

}