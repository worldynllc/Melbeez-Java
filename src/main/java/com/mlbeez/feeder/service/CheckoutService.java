package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.InsurancePayment;
import com.mlbeez.feeder.model.User;
import com.mlbeez.feeder.model.Warranty;
import com.mlbeez.feeder.repository.InsurancePaymentRepository;
import com.mlbeez.feeder.repository.WarrantyRepository;
import com.stripe.Stripe;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CheckoutService {

    @Value("${stripe.api.key}")
    public String stripeApiKey;
    @Autowired
    private UserService userService;

    @Autowired
    private WarrantyRepository warrantyRepository;

    @Autowired
    private InsurancePaymentRepository insurancePaymentRepository;

    private static final Logger logger= LoggerFactory.getLogger(CheckoutService.class);

    @Retryable(value = ApiConnectionException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Map<String, String> createCheckoutSession(Map<String, String> details) {

        Map<String, String> responseData = new HashMap<>();
        Stripe.apiKey = stripeApiKey;

        try {

            String warrantyId = details.get("warrantyId");
            String userId = details.get("userId");
            String userName = details.get("userName");
            String phoneNumber = details.get("phoneNumber");
            String email = details.get("email");
            String currency = details.get("currency");
            String cityName = details.get("cityName");
            String stateName = details.get("stateName");
            String zipCode = details.get("zipCode");
            String addressLine1 = details.get("addressLine1");
            String firstName = details.get("firstName");
            String lastName = details.get("lastName");
            String monthlyPriceStr = details.get("monthlyPrice");
            Float monthlyPrice = Float.parseFloat(monthlyPriceStr);
            Long monthlyPriceLong = (long) (monthlyPrice * 100);
            String subscriptionType = details.get("subscriptionType");

            String idempotencyKey = userId + "_" + System.currentTimeMillis();

            // Retrieve or create user and customer
            User user = userService.getOrCreateUser(userId, userName, email, phoneNumber, firstName, lastName, cityName,
                    stateName, zipCode, addressLine1);

            // Check if the warranty exists
            Optional<Warranty> findProduct = warrantyRepository.findByWarrantyId(warrantyId);
            if (findProduct.isEmpty()) {
                logger.error("Warranty not found in warranty table");
                responseData.put("error", "Warranty not found.");
                return responseData;
            }

            Warranty warranties = findProduct.get();
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

            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            Session session = Session.create(sessionParams, requestOptions);
            responseData.put("url", session.getUrl());
            logger.info("Checkout session successfully created for user {} with session URL: {}", userId, session.getUrl());
            return responseData;
        }
        catch (StripeException e) {
            logger.error("Stripe API error: {}",e.getMessage(),e);
            responseData.put("error","Failed to create Stripe checkout session due to Stripe API error.");
        }
        catch (NumberFormatException e) {
            logger.error("Invalid number format for monthly price: {}", e.getMessage(), e);
            responseData.put("error", "Invalid price format.");
        }
        catch (Exception e){
            logger.error("Unexpected error occurred during checkout session creation: {}", e.getMessage(), e);
            responseData.put("error", "An unexpected error occurred. Please try again later.");
        }
        return responseData;
    }

    // Fallback when retry attempts are exhausted
    @Recover
    public Session recover(ApiConnectionException e) {
        // Handle connection failure, possibly log and notify the user
        logger.error("Unable to connect to Stripe after retries: " + e.getMessage(),e);
        throw new RuntimeException("Unable to connect to Stripe after retries: " + e.getMessage());
    }

}
