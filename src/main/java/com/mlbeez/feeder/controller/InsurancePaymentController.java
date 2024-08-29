package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.InsurancePayment;
import com.mlbeez.feeder.service.InsurancePaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class InsurancePaymentController {

    @Autowired
    InsurancePaymentService insurancePaymentService;

    @GetMapping("/insurance-payment/{userId}")
    public List<InsurancePayment> getByUser(@PathVariable("userId") String userId){
        return insurancePaymentService.getByUser(userId);
    }

}
