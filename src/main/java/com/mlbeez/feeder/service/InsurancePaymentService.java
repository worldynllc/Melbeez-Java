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

    public InsurancePayment storePayment(InsurancePayment insurancePayment)
    {
        return insurancePaymentRepository.save(insurancePayment);
    }

    public List<InsurancePayment> getByUser(String userId) {
        return insurancePaymentRepository.findByUserId(userId);
    }

    public InsurancePayment updatePayment(InsurancePayment updatedPayment) {
        // Retrieve the existing payment by customer
        InsurancePayment existingPayment = insurancePaymentRepository.findByCustomer(updatedPayment.getCustomer());

        // Update the existing payment fields
        if (existingPayment != null) {
            existingPayment.setSubscription_Status(updatedPayment.getSubscription_Status());
            existingPayment.setSubscriptionId(updatedPayment.getSubscriptionId());
            existingPayment.setInvoiceId(updatedPayment.getInvoiceId());
            existingPayment.setInvoice_status(updatedPayment.getInvoice_status());
            existingPayment.setChargeRequest_status(updatedPayment.getChargeRequest_status());
            existingPayment.setCurrency(updatedPayment.getCurrency());
            existingPayment.setAmount(updatedPayment.getAmount());
            existingPayment.setMode(updatedPayment.getMode());
            existingPayment.setDefault_payment_method(updatedPayment.getDefault_payment_method());

            // Save the updated payment and return
            return insurancePaymentRepository.save(existingPayment);
        } else {
            logger.error("No insurance payment found for customer: {}", updatedPayment.getCustomer());
            return null;
        }
    }
}