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
            float monthlyPrice = Float.parseFloat(monthlyPriceStr);
            Long monthlyPriceLong = (long) (monthlyPrice * 100);
            String subscriptionType = details.get("subscriptionType");
            String paymentType=details.get("paymentType");


            String idempotencyKey = userId + "_" + System.currentTimeMillis();

            Optional<InsurancePayment> findSubscription=
                    insurancePaymentRepository.findByUserIdAndWarrantyId(userId,warrantyId );

            if (findSubscription.isPresent()){
                String subscriptionId=findSubscription.get().getSubscriptionId();
                Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
                if ("active".equals(stripeSubscription.getStatus())) {
                    responseData.put("error", "You already have an active subscription for this warranty.");
                    logger.info("Requested to user already purchased productId in InsurancePayment table");
                    return responseData;
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

            User user = userService.getOrCreateUser(userId, userName, email, phoneNumber, firstName, lastName, cityName,
                    stateName, zipCode, addressLine1);

            logger.info("Requested to create the Interval in stripe");
            PriceCreateParams.Recurring.Interval interval = "yearly".equals(subscriptionType)
                    ? PriceCreateParams.Recurring.Interval.YEAR
                    : PriceCreateParams.Recurring.Interval.MONTH;


            logger.info("Requested to create the Price in stripe");
            PriceCreateParams priceParams = PriceCreateParams.builder()
                    .setUnitAmount(monthlyPriceLong)
                    .setCurrency(currency)
                    .setRecurring(
                            PriceCreateParams.Recurring.builder()
                                    .setInterval(interval)
                                    .build()
                    )
                    .setProduct(productId)
                    .putMetadata("type",paymentType)
                    .build();
            Price price = Price.create(priceParams);

            logger.info("Requested to create the Session for Subscription");
            SessionCreateParams sessionParams = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(user.getCustomerId())
                    .setSuccessUrl(successUri)
                    .setCancelUrl(cancelUri)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPrice(price.getId())
                                    .build()
                    )
                    .putMetadata("type",paymentType)
                    .build();

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

    @Recover
    public Session recover(ApiConnectionException e) {
        logger.error("Unable to connect to Stripe after retries: " + e.getMessage(),e);
        throw new RuntimeException("Unable to connect to Stripe after retries: " + e.getMessage());
    }

    public void deleteSubscription(String subscriptionId){
        Subscription subscription = null;
        try {
            subscription = Subscription.retrieve(subscriptionId);
        } catch (StripeException e) {
            throw new RuntimeException(e.getMessage());
        }
        Map<String,Object>updateParams=new HashMap<>();
        updateParams.put("cancel_at_period_end",true);
        try {
            subscription.update(updateParams);
        } catch (StripeException e) {
            throw new RuntimeException(e.getMessage());
        }
        logger.info("Requested to subscription cancelled At the end of the billing period!");
    }
    public void cancelSubscriptionAtPeriodEnd(String subscriptionId){
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
