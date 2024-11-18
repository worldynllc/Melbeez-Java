package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.service.CheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;



@RestController
public class StripeController {

    @Autowired
    CheckoutService checkoutService;

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
    public ResponseEntity<String> cancelSubscription(@PathVariable("id") String id) {
      return checkoutService.deleteSubscription(id);
    }

}