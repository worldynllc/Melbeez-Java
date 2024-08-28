package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.InsurancePayment;
import com.mlbeez.feeder.model.User;
import com.mlbeez.feeder.model.Warranty;
import com.mlbeez.feeder.repository.InsurancePaymentRepository;
import com.mlbeez.feeder.repository.WarrantyRepository;
import com.stripe.Stripe;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CheckoutService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;
    @Autowired
    private UserService userService;

    @Autowired
    private WarrantyRepository warrantyRepository;

    @Autowired
    private InsurancePaymentRepository insurancePaymentRepository;

    public Map<String, String> createCheckoutSession(Map<String, String> details) throws Exception {
        Map<String, String> responseData = new HashMap<>();
        Stripe.apiKey = stripeApiKey;

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
        String lastName=details.get("lastName");
        String monthlyPriceStr = details.get("monthlyPrice");
        Float monthlyPrice = Float.parseFloat(monthlyPriceStr);
        Long monthlyPriceLong = (long) (monthlyPrice * 100);
        String subscriptionType = details.get("subscriptionType");

        String idempotencyKey = userId + "_" + System.currentTimeMillis();

        // Retrieve or create user
        User user = userService.getOrCreateUser(userId, userName, email, phoneNumber,firstName,lastName,cityName,
                stateName,zipCode,addressLine1);

        // Check if the warranty exists
        Optional<Warranty> findProduct = warrantyRepository.findByWarrantyId(warrantyId);
        if (findProduct.isEmpty()) {
            responseData.put("error", "Warranty not found.");
            return responseData;
        }

        Warranty warranties = findProduct.get();
        String productId = warranties.getProductId();
        List<InsurancePayment> findSubscriptionList = insurancePaymentRepository.findAllByProductId(productId);

//        List<InsurancePayment> findList = insurancePaymentRepository.findAllByUserId(userId);
//
//        System.out.println("findList :"+findList);

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

        return responseData;
    }

}
