package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.InsurancePayment;
import com.mlbeez.feeder.model.UserResponseBaseModel;
import com.mlbeez.feeder.model.Warranty;
import com.mlbeez.feeder.repository.AspNetUserRepository;
import com.mlbeez.feeder.repository.InsurancePaymentRepository;
import com.mlbeez.feeder.repository.WarrantyRepository;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import com.stripe.Stripe;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private String stripeApiKey;

    @Value("${payment.success.uri}")
    private String successUri;

    @Value("${payment.cancel.uri}")
    private String cancelUri;

    private final WarrantyRepository warrantyRepository;
    private final AspNetUserRepository aspNetUserRepository;
    private final InsurancePaymentRepository insurancePaymentRepository;

    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    public CheckoutService(WarrantyRepository warrantyRepository,
                           AspNetUserRepository aspNetUserRepository,
                           InsurancePaymentRepository insurancePaymentRepository) {
        this.warrantyRepository = warrantyRepository;
        this.aspNetUserRepository = aspNetUserRepository;
        this.insurancePaymentRepository = insurancePaymentRepository;
    }

    @Retryable(
            value = ApiConnectionException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public Map<String, String> createCheckoutSession(Map<String, String> details) {

        Stripe.apiKey = stripeApiKey;
        Map<String, String> responseData = new HashMap<>();

        try {
            String warrantyId = details.get("warrantyId");
            String userId = details.get("userId");
            String currency = details.get("currency");
            String monthlyPriceStr = details.get("monthlyPrice");
            String subscriptionType = details.get("subscriptionType"); // monthly / yearly
            String paymentType = details.get("paymentType");           // one_time / recurring

            if (warrantyId == null || userId == null || currency == null || monthlyPriceStr == null) {
                logger.error("Missing required checkout parameters");
                responseData.put("error", "Missing required parameters.");
                return responseData;
            }

            float monthlyPrice = Float.parseFloat(monthlyPriceStr);
            long unitAmount = (long) (monthlyPrice * 100);

            // Check existing subscription
            Optional<InsurancePayment> findSubscription =
                    insurancePaymentRepository.findByUserIdAndWarrantyId(userId, warrantyId);

            if (findSubscription.isPresent()) {
                String subscriptionId = findSubscription.get().getSubscriptionId();
                logger.info("Existing subscriptionId from DB: {}", subscriptionId);

                if (subscriptionId != null && !subscriptionId.isEmpty()) {
                    Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
                    if ("active".equals(stripeSubscription.getStatus())) {
                        responseData.put("error", "You already have an active subscription for this warranty.");
                        logger.info("User {} already has active subscription {}", userId, subscriptionId);
                        return responseData;
                    }
                } else {
                    logger.warn("Invalid subscriptionId stored for userId={}, warrantyId={}", userId, warrantyId);
                }
            }

            // Find warranty & productId
            logger.info("Looking up warranty {}", warrantyId);
            Warranty warranty = warrantyRepository.findByWarrantyId(warrantyId)
                    .orElseThrow(() -> {
                        logger.error("Warranty {} not found in DB", warrantyId);
                        return new DataNotFoundException("Warranty not found");
                    });

            String productId = warranty.getProductId();
            logger.info("Using Stripe productId {} for warranty {}", productId, warrantyId);

            // Get / create Stripe customer
            String customerId = getOrCreateStripeCustomer(userId);

            // Choose recurring interval
            PriceCreateParams.Recurring.Interval interval =
                    "yearly".equalsIgnoreCase(subscriptionType)
                            ? PriceCreateParams.Recurring.Interval.YEAR
                            : PriceCreateParams.Recurring.Interval.MONTH;

            // Create Price
            logger.info("Creating Stripe Price for product {} with interval {}", productId, interval);
            PriceCreateParams priceParams = PriceCreateParams.builder()
                    .setUnitAmount(unitAmount)
                    .setCurrency(currency)
                    .setRecurring(
                            PriceCreateParams.Recurring.builder()
                                    .setInterval(interval)
                                    .build()
                    )
                    .setProduct(productId)
                    .putMetadata("type", paymentType)
                    .putMetadata("subscription", subscriptionType)
                    .build();

            Price price = Price.create(priceParams);
            logger.info("Price created: {}", price.getId());

            // Create Checkout Session
            logger.info("Creating Checkout Session for customer {}", customerId);
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
                    .putMetadata("userId", userId)
                    .build();

            Session session = Session.create(sessionParams);

            logger.info("Checkout Session created for user {}: {}", userId, session.getUrl());
            responseData.put("url", session.getUrl());
            return responseData;

        } catch (StripeException e) {
            logger.error("Stripe API error: {}", e.getMessage(), e);
            throw new RuntimeException("Stripe API error occurred", e);
        } catch (NumberFormatException e) {
            logger.error("Invalid monthly price: {}", e.getMessage());
            responseData.put("error", "Invalid price format.");
        } catch (Exception e) {
            logger.error("Unexpected error during checkout session creation: {}", e.getMessage(), e);
            responseData.put("error", "An unexpected error occurred. Please try again later.");
        }

        return responseData;
    }

    @Recover
    public Session recover(ApiConnectionException e) {
        logger.error("Unable to connect to Stripe after retries: {}", e.getMessage(), e);
        throw new RuntimeException("Unable to connect to Stripe after retries: " + e.getMessage());
    }

    public String getOrCreateStripeCustomer(String userId) throws StripeException {
        logger.info("Creating Stripe customer for userId: {}", userId);

        UserResponseBaseModel user =
                aspNetUserRepository.findById(userId)
                        .orElseThrow(() -> new DataNotFoundException("User data not found for ID: " + userId));

        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setName(user.getUsername())
                .setEmail(user.getEmail())
                .putMetadata("userId", userId)
                .build();

        Customer stripeCustomer = Customer.create(customerParams);
        logger.info("Stripe customer {} created for user {}", stripeCustomer.getId(), userId);
        return stripeCustomer.getId();
    }

    public void deleteSubscription(String subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            Map<String, Object> params = new HashMap<>();
            params.put("cancel_at_period_end", true);
            subscription.update(params);
            logger.info("Subscription {} will cancel at period end.", subscriptionId);
        } catch (InvalidRequestException e) {
            throw new com.mlbeez.feeder.service.exception.InvalidRequestException("Subscription Id not valid");
        } catch (StripeException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void cancelSubscriptionAtPeriodEnd(String subscriptionId) {
        logger.info("Requested to confirm the one-time payment (cancel at period end)");
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            Map<String, Object> params = new HashMap<>();
            params.put("cancel_at_period_end", true);
            subscription.update(params);
            logger.info("Subscription {} is set to cancel at end of period.", subscriptionId);
        } catch (InvalidRequestException e) {
            logger.error("Subscription not found. Code: {}, Message: {}", e.getCode(), e.getMessage());
        } catch (StripeException e) {
            logger.error("StripeException while canceling subscription {}: {}", subscriptionId, e.getMessage());
        }
    }
}















































//package com.mlbeez.feeder.service;
//
//import com.mlbeez.feeder.model.*;
//import com.mlbeez.feeder.repository.AspNetUserRepository;
//import com.mlbeez.feeder.repository.InsurancePaymentRepository;
//import com.mlbeez.feeder.repository.WarrantyRepository;
//import com.mlbeez.feeder.service.exception.DataNotFoundException;
//import com.stripe.Stripe;
//import com.stripe.exception.ApiConnectionException;
//import com.stripe.exception.InvalidRequestException;
//import com.stripe.exception.StripeException;
//import com.stripe.model.Customer;
//import com.stripe.model.Price;
//import com.stripe.model.Subscription;
//import com.stripe.model.checkout.Session;
//import com.stripe.param.CustomerCreateParams;
//import com.stripe.param.PriceCreateParams;
//import com.stripe.param.checkout.SessionCreateParams;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.retry.annotation.Backoff;
//import org.springframework.retry.annotation.Recover;
//import org.springframework.retry.annotation.Retryable;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//@Service
//public class CheckoutService {
//
//    @Value("${stripe.api.key}")
//    public String stripeApiKey;
//    @Value("${payment.success.uri}")
//    public String successUri;
//
//    @Value("${payment.cancel.uri}")
//    public String cancelUri;
//
//    private final WarrantyRepository warrantyRepository;
//
//    private final AspNetUserRepository aspNetUserRepository;
//
//    private final InsurancePaymentRepository insurancePaymentRepository;
//
//    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);
//
//    public CheckoutService(WarrantyRepository warrantyRepository, AspNetUserRepository aspNetUserRepository, InsurancePaymentRepository insurancePaymentRepository) {
//        this.warrantyRepository = warrantyRepository;
//        this.aspNetUserRepository = aspNetUserRepository;
//        this.insurancePaymentRepository = insurancePaymentRepository;
//    }
//
//    @Retryable(value = ApiConnectionException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
//    public Map<String, String> createCheckoutSession(Map<String, String> details) {
//
//        Map<String, String> responseData = new HashMap<>();
//        Stripe.apiKey = stripeApiKey;
//        try {
//            String warrantyId = details.get("warrantyId");
//            String userId = details.get("userId");
//            String currency = details.get("currency");
//            String monthlyPriceStr = details.get("monthlyPrice");
//            float monthlyPrice = Float.parseFloat(monthlyPriceStr);
//            Long monthlyPriceLong = (long) (monthlyPrice * 100);
//            String subscriptionType = details.get("subscriptionType");
//            String paymentType = details.get("paymentType");
//
//            Optional<InsurancePayment> findSubscription =
//                    insurancePaymentRepository.findByUserIdAndWarrantyId(userId, warrantyId);
//
//            if (findSubscription.isPresent()) {
//                String subscriptionId = findSubscription.get().getSubscriptionId();
//                logger.info("Retrieved subscriptionId: {}", subscriptionId);
//                if (subscriptionId != null && !subscriptionId.isEmpty()) {
//                    Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
//                    if ("active".equals(stripeSubscription.getStatus())) {
//                        responseData.put("error", "You already have an active subscription for this warranty.");
//                        logger.info("User already has an active subscription: {}", subscriptionId);
//                        return responseData;
//                    }
//                } else {
//                    logger.warn("Invalid subscription ID for userId: {}, warrantyId: {}", userId, warrantyId);
//                }
//            }
//            logger.info("Requested to Get the WarrantyId in warranty table");
//            Optional<Warranty> findWarranty = warrantyRepository.findByWarrantyId(warrantyId);
//            if (findWarranty.isEmpty()) {
//                logger.error("Warranty not found in warranty table");
//                responseData.put("error", "Warranty not found.");
//                return responseData;
//            }
//
//            Warranty warranties = findWarranty.get();
//            logger.info("Requested to get the productId  from Warranty table");
//            String productId = warranties.getProductId();
//
//            String customerId = getOrCreateStripeCustomer(userId);
//
//            logger.info("Requested to create the Interval in stripe");
//            PriceCreateParams.Recurring.Interval interval = "yearly".equals(subscriptionType)
//                    ? PriceCreateParams.Recurring.Interval.YEAR
//                    : PriceCreateParams.Recurring.Interval.MONTH;
//
//            logger.info("Creating Stripe price for product: {}", productId);
//            PriceCreateParams priceParams = PriceCreateParams.builder()
//                    .setUnitAmount(monthlyPriceLong)
//                    .setCurrency(currency)
//                    .setRecurring(
//                            PriceCreateParams.Recurring.builder()
//                                    .setInterval(interval)
//                                    .build()
//                    )
//                    .setProduct(productId)
//                    .putMetadata("type", paymentType)
//                    .putMetadata("subscription",subscriptionType)
//                    .build();
//            Price price = Price.create(priceParams);
//            logger.info("Stripe price created successfully with ID: {}", price.getId());
//
//            logger.info("Requested to create the Session for Subscription");
//            SessionCreateParams sessionParams = SessionCreateParams.builder()
//                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
//                    .setCustomer(customerId)
//                    .setSuccessUrl(successUri)
//                    .setCancelUrl(cancelUri)
//                    .addLineItem(
//                            SessionCreateParams.LineItem.builder()
//                                    .setQuantity(1L)
//                                    .setPrice(price.getId())
//                                    .build()
//                    )
//                    .putMetadata("type", paymentType)
//                    .putMetadata("userId",userId)
//                    .build();
//
//            Session session = Session.create(sessionParams);
//            responseData.put("url", session.getUrl());
//            logger.info("Checkout session successfully created for user {} with session URL: {}", userId, session.getUrl());
//            return responseData;
//        } catch (StripeException e) {
//            logger.error("Stripe API error: {}", e.getMessage(), e);
//            throw new RuntimeException("Stripe API error occurred", e);
//        } catch (NumberFormatException e) {
//            logger.error("Invalid number format for monthly price: {}", e.getMessage());
//            responseData.put("error", "Invalid price format.");
//        } catch (Exception e) {
//            logger.error("Unexpected error occurred during checkout session creation: {}", e.getMessage());
//            responseData.put("error", "An unexpected error occurred. Please try again later.");
//        }
//        return responseData;
//    }
//
//    @Recover
//    public Session recover(ApiConnectionException e) {
//        logger.error("Unable to connect to Stripe after retries: {}", e.getMessage(), e);
//        throw new RuntimeException("Unable to connect to Stripe after retries: " + e.getMessage());
//    }
//
//    public String getOrCreateStripeCustomer(String userId) throws StripeException {
//        logger.info("Creating a new Stripe customer for user ID: {}", userId);
//
//        UserResponseBaseModel userResponse = aspNetUserRepository.findById(userId)
//                .orElseThrow(() -> new DataNotFoundException("User data not found for ID: " + userId));
//
//        CustomerCreateParams customerParams = CustomerCreateParams.builder()
//                .setName(userResponse.getUsername())
//                .setEmail(userResponse.getEmail())
//                .putMetadata("userId", userId)
//                .build();
//        Customer stripeCustomer = Customer.create(customerParams);
//        return stripeCustomer.getId();
//    }
//    public void deleteSubscription(String subscriptionId) {
//        Subscription subscription = null;
//        try {
//            subscription = Subscription.retrieve(subscriptionId);
//
//            Map<String, Object> updateParams = new HashMap<>();
//            updateParams.put("cancel_at_period_end", true);
//
//            subscription.update(updateParams);
//        }
//        catch (InvalidRequestException e){
//            throw new com.mlbeez.feeder.service.exception.InvalidRequestException("Subscription Id not valid");
//        }catch (StripeException e) {
//            throw new RuntimeException(e.getMessage());
//        }
//        logger.info("Requested to subscription cancelled At the end of the billing period!");
//    }
//
//    public void cancelSubscriptionAtPeriodEnd(String subscriptionId) {
//        logger.info("Requested to confirm the one-time payment");
//        try {
//            Subscription subscription = Subscription.retrieve(subscriptionId);
//            Map<String, Object> updateParams = new HashMap<>();
//            updateParams.put("cancel_at_period_end", true);
//            subscription.update(updateParams);
//            logger.info("Subscription {} is set to cancel at the end of the current billing period.", subscriptionId);
//        }catch (InvalidRequestException e){
//            logger.error("Subscription not found & Error code : {} & Error message : {} ",e.getCode(),e.getMessage());
//        } catch (StripeException e) {
//            logger.error("StripeException occurred while canceling subscription {}: {}", subscriptionId, e.getMessage());
//        }
//    }
//}