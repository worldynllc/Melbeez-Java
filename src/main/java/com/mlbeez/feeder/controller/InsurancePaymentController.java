package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.InsurancePayment;
import com.mlbeez.feeder.service.InsurancePaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class InsurancePaymentController {

    private static final Logger logger= LoggerFactory.getLogger(InsurancePaymentController.class);

    @Autowired
    InsurancePaymentService insurancePaymentService;

    @GetMapping("/insurance-payment/{userId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPERADMIN')")
    public List<InsurancePayment> getByUser(@PathVariable("userId") String userId){
        logger.info("Request to Get By User {}",userId);
        return insurancePaymentService.getByUser(userId);
    }

}
