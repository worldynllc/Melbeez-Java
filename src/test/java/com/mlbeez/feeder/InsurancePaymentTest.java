package com.mlbeez.feeder;

import com.mlbeez.feeder.model.InsurancePayment;
import com.mlbeez.feeder.repository.InsurancePaymentRepository;
import com.mlbeez.feeder.service.InsurancePaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InsurancePaymentTest {

    @InjectMocks
    private InsurancePaymentService insurancePaymentService;

    @Mock
    private InsurancePaymentRepository insurancePaymentRepository;

    @Test
    void testStorePayment_Success() {
        InsurancePayment insurancePayment = new InsurancePayment();
        insurancePayment.setSubscriptionId("sub123");
        insurancePayment.setUserId("user123");
        insurancePayment.setDefault_payment_method("credit card");
        insurancePayment.setProductId("prod001");
        insurancePayment.setEmail("user@example.com");
        insurancePayment.setName("John Doe");
        insurancePayment.setPhoneNumber("1234567890");
        insurancePayment.setCustomer("customer123");
        insurancePayment.setInvoice_status("Paid");
        insurancePayment.setWarrantyId("warranty123");
        insurancePayment.setAmount(1000L);
        insurancePayment.setInvoiceId("invoice123");
        insurancePayment.setSubscriptionMode("monthly");
        insurancePayment.setCurrency("USD");
        insurancePayment.setSubscription_Status("active");
        insurancePayment.setChargeRequest_status("success");
        insurancePayment.setMode("online");

        insurancePaymentService.storePayment(insurancePayment);

        verify(insurancePaymentRepository).save(insurancePayment);
    }

    @Test
    void testGetByUser_Success() {
        String userId = "user123";

        InsurancePayment insurancePaymentOne = new InsurancePayment();
        insurancePaymentOne.setUserId(userId);
        insurancePaymentOne.setSubscriptionId("sub123");

        InsurancePayment insurancePaymentTwo = new InsurancePayment();
        insurancePaymentTwo.setUserId(userId);
        insurancePaymentTwo.setSubscriptionId("sub124");

        List<InsurancePayment> insurancePayments = Arrays.asList(insurancePaymentOne, insurancePaymentTwo);

        when(insurancePaymentRepository.findByUserId(userId)).thenReturn(insurancePayments);

        List<InsurancePayment> result = insurancePaymentService.getByUser(userId);

        verify(insurancePaymentRepository).findByUserId(userId);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(userId, result.get(1).getUserId());
    }
}
