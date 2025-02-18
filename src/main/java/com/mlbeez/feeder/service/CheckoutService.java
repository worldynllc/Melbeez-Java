package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.*;
import com.mlbeez.feeder.repository.AspNetUserRepository;
import com.mlbeez.feeder.repository.CustomerRepository;
import com.mlbeez.feeder.repository.InsurancePaymentRepository;
import com.mlbeez.feeder.repository.WarrantyRepository;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import com.stripe.Stripe;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CheckoutService {

    @Value("${stripe.api.key}")
    public String stripeApiKey;
    @Value("${payment.success.uri}")
    public String successUri;

    @Value("${payment.cancel.uri}")
    public String cancelUri;

    @Autowired
    private WarrantyRepository warrantyRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AspNetUserRepository aspNetUserRepository;

    @Autowired
    private InsurancePaymentRepository insurancePaymentRepository;

    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    @Retryable(value = ApiConnectionException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Map<String, String> createCheckoutSession(Map<String, String> details) {

        Map<String, String> responseData = new HashMap<>();
        Stripe.apiKey = stripeApiKey;
        try {

            String warrantyId = details.get("warrantyId");
            String userId = details.get("userId");
            String currency = details.get("currency");
            String monthlyPriceStr = details.get("monthlyPrice");
            float monthlyPrice = Float.parseFloat(monthlyPriceStr);
            Long monthlyPriceLong = (long) (monthlyPrice * 100);
            String subscriptionType = details.get("subscriptionType");
            String paymentType = details.get("paymentType");


            String idempotencyKey = userId + "_" + System.currentTimeMillis();

            Optional<InsurancePayment> findSubscription =
                    insurancePaymentRepository.findByUserIdAndWarrantyId(userId, warrantyId);

            if (findSubscription.isPresent()) {
                String subscriptionId = findSubscription.get().getSubscriptionId();
                logger.info("Retrieved subscriptionId: {}", subscriptionId);
                if (subscriptionId != null && !subscriptionId.isEmpty()) {
                    Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
                    if ("active".equals(stripeSubscription.getStatus())) {
                        responseData.put("error", "You already have an active subscription for this warranty.");
                        logger.info("User already has an active subscription: {}", subscriptionId);
                        return responseData;
                    }
                } else {
                    logger.warn("Invalid subscription ID for userId: {}, warrantyId: {}", userId, warrantyId);
                }
            }
            logger.info("Requested to Get the WarrantyId in warranty table");
            Optional<Warranty> findWarranty = warrantyRepository.findByWarrantyId(warrantyId);
            if (findWarranty.isEmpty()) {
                logger.error("Warranty not found in warranty table");
                responseData.put("error", "Warranty not found.");
                return responseData;
            }

            Warranty warranties = findWarranty.get();
            logger.info("Requested to get the productId  from Warranty table");
            String productId = warranties.getProductId();

            String customerId = getOrCreateStripeCustomer(userId);

            logger.info("Requested to create the Interval in stripe");
            PriceCreateParams.Recurring.Interval interval = "yearly".equals(subscriptionType)
                    ? PriceCreateParams.Recurring.Interval.YEAR
                    : PriceCreateParams.Recurring.Interval.MONTH;

            logger.info("Creating Stripe price for product: {}", productId);
            PriceCreateParams priceParams = PriceCreateParams.builder()
                    .setUnitAmount(monthlyPriceLong)
                    .setCurrency(currency)
                    .setRecurring(
                            PriceCreateParams.Recurring.builder()
                                    .setInterval(interval)
                                    .build()
                    )
                    .setProduct(productId)
                    .putMetadata("type", paymentType)
                    .build();
            Price price = Price.create(priceParams);
            logger.info("Stripe price created successfully with ID: {}", price.getId());

            logger.info("Requested to create the Session for Subscription");
            SessionCreateParams sessionParams = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(successUri)
                    .setCancelUrl(cancelUri)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPrice(price.getId())
                                    .build()
                    )
                    .putMetadata("type", paymentType)
                    .build();

            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .setConnectTimeout(60000)
                    .setReadTimeout(60000)
                    .build();

            Session session = Session.create(sessionParams, requestOptions);
            responseData.put("url", session.getUrl());
            logger.info("Checkout session successfully created for user {} with session URL: {}", userId, session.getUrl());
            return responseData;
        } catch (StripeException e) {
            logger.error("Stripe API error: {}", e.getMessage(), e);
            throw new RuntimeException("Stripe API error occurred", e);
        } catch (NumberFormatException e) {
            logger.error("Invalid number format for monthly price: {}", e.getMessage());
            responseData.put("error", "Invalid price format.");
        } catch (Exception e) {
            logger.error("Unexpected error occurred during checkout session creation: {}", e.getMessage());
            responseData.put("error", "An unexpected error occurred. Please try again later.");
        }
        return responseData;
    }

    @Recover
    public Session recover(ApiConnectionException e) {
        logger.error("Unable to connect to Stripe after retries: " + e.getMessage(), e);
        throw new RuntimeException("Unable to connect to Stripe after retries: " + e.getMessage());
    }

    public String getOrCreateStripeCustomer(String userId) throws StripeException {
        Optional<Customers> optionalCustomer = customerRepository.findByUserId(userId);

        if (optionalCustomer.isPresent()) {
            Customers customer = optionalCustomer.get();

            if (customer.getCustomerId() == null || customer.getCustomerId().isEmpty()) {
                String customerId = createAndSaveStripeCustomer(userId);
                customer.setCustomerId(customerId);
                customerRepository.save(customer);
                return customerId;
            }
            return customer.getCustomerId();
        }
        return createAndSaveStripeCustomer(userId);
    }

    private String createAndSaveStripeCustomer(String userId) throws StripeException {
        logger.info("Creating a new Stripe customer for user ID: {}", userId);

        UserResponseBaseModel userResponse = aspNetUserRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User data not found for ID: " + userId));

        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setName(userResponse.getUsername())
                .setEmail(userResponse.getEmail())
                .putMetadata("userId", userId)
                .build();
        Customer stripeCustomer = Customer.create(customerParams);

        Customers customer = new Customers();
        customer.setUserId(userId);
        customer.setCustomerId(stripeCustomer.getId());
        customerRepository.save(customer);

        return stripeCustomer.getId();
    }


    public void deleteSubscription(String subscriptionId) {
        Subscription subscription = null;
        try {
            subscription = Subscription.retrieve(subscriptionId);
        } catch (StripeException e) {
            throw new RuntimeException(e.getMessage());
        }
        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("cancel_at_period_end", true);
        try {
            subscription.update(updateParams);
        } catch (StripeException e) {
            throw new RuntimeException(e.getMessage());
        }
        logger.info("Requested to subscription cancelled At the end of the billing period!");
    }

    public void cancelSubscriptionAtPeriodEnd(String subscriptionId) {
        logger.info("Requested to confirm the one-time payment");
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            if (subscription == null) {
                logger.error("Failed to retrieve subscription with ID: {}", subscriptionId);
                return;
            }
            Map<String, Object> updateParams = new HashMap<>();
            updateParams.put("cancel_at_period_end", true);
            subscription.update(updateParams);

            logger.info("Subscription {} is set to cancel at the end of the current billing period.", subscriptionId);
        } catch (StripeException e) {
            logger.error("StripeException occurred while canceling subscription {}: {}", subscriptionId, e.getMessage());
        } catch (Exception e) {
            logger.error("Exception occurred while canceling subscription {}: {}", subscriptionId, e.getMessage(), e);
        }
    }
}