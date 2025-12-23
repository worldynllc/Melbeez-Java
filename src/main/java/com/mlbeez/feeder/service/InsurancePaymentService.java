package com.mlbeez.feeder.service;
import com.mlbeez.feeder.model.InsurancePayment;
import com.mlbeez.feeder.repository.InsurancePaymentRepository;
import com.mlbeez.feeder.service.exception.InsuranceRecordNotFoundException;
import com.mlbeez.feeder.service.exception.UserIdRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InsurancePaymentService {

    private static final Logger logger = LoggerFactory.getLogger(InsurancePaymentService.class);

    private final InsurancePaymentRepository insurancePaymentRepository;

    public InsurancePaymentService(InsurancePaymentRepository insurancePaymentRepository) {
        this.insurancePaymentRepository = insurancePaymentRepository;
    }

    public void storePayment(InsurancePayment insurancePayment) {
        insurancePaymentRepository.save(insurancePayment);
    }

    public List<InsurancePayment> getByUser(String userId) {
        if(userId == null){
            logger.error("user id is must!!");
            throw new UserIdRequiredException("user id is must!!");
        }
        return insurancePaymentRepository.findByUserId(userId).orElseThrow(()->{
            logger.error("Insurance payment record not found for this user id : {}",userId);
            return new InsuranceRecordNotFoundException("Insurance payment record not found");
        });
    }

    public void deleteSubscriptionPayment(String customerId, String subscriptionId) {
        if(customerId == null){
            logger.error("customer id is null");
            throw new UserIdRequiredException("customer id is null");
        }
        InsurancePayment existingPayment = insurancePaymentRepository.findByCustomerAndSubscriptionId(customerId, subscriptionId).orElseThrow(()->{
            logger.info("Insurance payment record not found for this customerId : {}",customerId);
            throw new InsuranceRecordNotFoundException("Insurance payment record not found");
        });
            insurancePaymentRepository.deleteById(existingPayment.getId());
            logger.info("Subscription canceled and insurance payment record deleted");
    }
}