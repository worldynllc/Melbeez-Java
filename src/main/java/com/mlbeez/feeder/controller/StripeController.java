package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.service.CheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class StripeController {

    private final CheckoutService checkoutService;

    private static final Logger logger= LoggerFactory.getLogger(StripeController.class);

    public StripeController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/create-checkout-session")
    @PreAuthorize("hasAnyRole('USER','SUPERADMIN')")
    public Map<String, String> createCheckoutSession(@RequestBody Map<String, String> Details) {
        logger.info("Requested to create stripe checkout page");
        return checkoutService.createCheckoutSession(Details);
    }


    @DeleteMapping("subscriptions/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPERADMIN')")
    public void cancelSubscription(@PathVariable("id") String id) {
        logger.info("Requested to cancel subscription");
       checkoutService.deleteSubscription(id);
    }

}