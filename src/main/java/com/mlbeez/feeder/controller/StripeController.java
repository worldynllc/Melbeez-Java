package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.service.CheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;



@RestController
public class StripeController {

    @Autowired
    CheckoutService checkoutService;

    private static final Logger logger= LoggerFactory.getLogger(StripeController.class);

    @PostMapping("/create-checkout-session")
    @PreAuthorize("hasAnyRole('USER','SUPERADMIN')")
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
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPERADMIN')")
    public void cancelSubscription(@PathVariable("id") String id) {
        logger.info("Requested to cancel subscription");
       checkoutService.deleteSubscription(id);
    }

}