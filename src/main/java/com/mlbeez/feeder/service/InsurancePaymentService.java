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

    @Autowired
    InsurancePaymentRepository insurancePaymentRepository;

    private static final Logger logger = LoggerFactory.getLogger(InsurancePaymentService.class);

    public void storePayment(InsurancePayment insurancePayment)
    {
        insurancePaymentRepository.save(insurancePayment);
    }

    public List<InsurancePayment> getByUser(String userId) {
        return insurancePaymentRepository.findByUserId(userId);
    }

    public void updatePayment(InsurancePayment updatedPayment,String subscriptionId) {
        InsurancePayment existingPayment =
                insurancePaymentRepository.findByCustomerAndSubscriptionId(updatedPayment.getCustomer(),subscriptionId);

        if (existingPayment != null) {
            existingPayment.setSubscription_Status(updatedPayment.getSubscription_Status());
            existingPayment.setSubscriptionId(updatedPayment.getSubscriptionId());
            existingPayment.setCustomer(updatedPayment.getCustomer());
            existingPayment.setName(updatedPayment.getName());
            existingPayment.setEmail(updatedPayment.getEmail());
            existingPayment.setPhoneNumber(updatedPayment.getPhoneNumber());
            existingPayment.setProductId(updatedPayment.getProductId());
            existingPayment.setUserId(updatedPayment.getUserId());
            existingPayment.setWarrantyId(updatedPayment.getWarrantyId());
            existingPayment.setInvoiceId(updatedPayment.getInvoiceId());
            existingPayment.setInvoice_status(updatedPayment.getInvoice_status());
            existingPayment.setChargeRequest_status(updatedPayment.getChargeRequest_status());
            existingPayment.setCurrency(updatedPayment.getCurrency());
            existingPayment.setAmount(updatedPayment.getAmount());
            existingPayment.setMode(updatedPayment.getMode());
            existingPayment.setDefault_payment_method(updatedPayment.getDefault_payment_method());

            insurancePaymentRepository.save(existingPayment);
        } else {
            logger.error("No insurance payment found for customer: {}", updatedPayment.getCustomer());
        }
    }
}