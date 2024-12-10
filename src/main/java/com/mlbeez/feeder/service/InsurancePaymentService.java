package com.mlbeez.feeder.service;
import com.mlbeez.feeder.model.InsurancePayment;
import com.mlbeez.feeder.repository.InsurancePaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InsurancePaymentService {

    private static final Logger logger= LoggerFactory.getLogger(InsurancePaymentService.class);

    @Autowired
    InsurancePaymentRepository insurancePaymentRepository;

    public void storePayment(InsurancePayment insurancePayment)
    {
        insurancePaymentRepository.save(insurancePayment);
    }

    public List<InsurancePayment> getByUser(String userId) {
        return insurancePaymentRepository.findByUserId(userId);
    }

    public void deleteSubscriptionPayment(String customerId, String subscriptionId){
        InsurancePayment existingPayment = insurancePaymentRepository.findByCustomerAndSubscriptionId(customerId,subscriptionId);
        if (existingPayment != null) {
            insurancePaymentRepository.deleteById(existingPayment.getId());
            logger.info("Subscription canceled and insurance payment record deleted.");
        } else {
            logger.info("Insurance payment record not found.");
        }
    }

}