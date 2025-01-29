package com.mlbeez.feeder;

import com.mlbeez.feeder.model.Customers;
import com.mlbeez.feeder.model.InsurancePayment;
import com.mlbeez.feeder.model.Warranty;
import com.mlbeez.feeder.repository.CustomerRepository;
import com.mlbeez.feeder.repository.InsurancePaymentRepository;
import com.mlbeez.feeder.repository.WarrantyRepository;
import com.mlbeez.feeder.service.CheckoutService;
import com.stripe.Stripe;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentIntegrationTest {

    @Mock
    private WarrantyRepository warrantyRepository;

    @Mock
    private InsurancePaymentRepository insurancePaymentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CheckoutService checkoutService;

    @Value("${stripe.api.key}")
    private String apiKey;

    @BeforeEach
    void setUp() {
        Stripe.apiKey = apiKey;
    }

    @Test
    void testCreateCheckoutSession_Success() throws Exception {

        Map<String, String> details = new HashMap<>();
        details.put("warrantyId", "92630984");
        details.put("userId", "9def9633-d1d1-4fa0-b9e3-bb249545d95f");
        details.put("currency", "usd");
        details.put("monthlyPrice", "99.99");
        details.put("subscriptionType", "monthly");
        details.put("paymentType", "one_time");


        Warranty mockWarranty = new Warranty();
        mockWarranty.setProductId("prod_12345");
        when(warrantyRepository.findByWarrantyId("92630984"))
                .thenReturn(Optional.of(mockWarranty));


        Customers mockCustomer = new Customers();
        mockCustomer.setUserId("9def9633-d1d1-4fa0-b9e3-bb249545d95f");
        mockCustomer.setCustomerId("cust_mock_12345");
        when(customerRepository.findByUserId("9def9633-d1d1-4fa0-b9e3-bb249545d95f"))
                .thenReturn(Optional.of(mockCustomer));


        InsurancePayment mockPayment = new InsurancePayment();
        mockPayment.setSubscriptionId("sub_mock_12345");
        when(insurancePaymentRepository.findByUserIdAndWarrantyId("9def9633-d1d1-4fa0-b9e3-bb249545d95f", "92630984"))
                .thenReturn(Optional.empty());


        try (MockedStatic<Session> mockedSession = mockStatic(Session.class);
             MockedStatic<Price> mockedPrice = mockStatic(Price.class)) {


            Price mockPrice = mock(Price.class);
            when(mockPrice.getId()).thenReturn("price_12345");
            mockedPrice.when(() -> Price.create(any(PriceCreateParams.class))).thenReturn(mockPrice);


            Session mockSession = mock(Session.class);
            when(mockSession.getUrl()).thenReturn("https://mock-session-url.com");
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(mockSession);

            Map<String, String> response = checkoutService.createCheckoutSession(details);

            assertNotNull(response);
            assertEquals("https://mock-session-url.com", response.get("url"));
        }
    }

    @Test
    void testCreateCheckoutSession_WarrantyNotFound() throws Exception {
        Map<String, String> details = new HashMap<>();
        details.put("warrantyId", "invalid_warranty_id");
        details.put("userId", "9def9633-d1d1-4fa0-b9e3-bb249545d95f");
        details.put("currency", "usd");
        details.put("monthlyPrice", "99.99");
        details.put("subscriptionType", "monthly");
        details.put("paymentType", "one_time");

        when(warrantyRepository.findByWarrantyId("invalid_warranty_id"))
                .thenReturn(Optional.empty());

        Map<String, String> response = checkoutService.createCheckoutSession(details);

        assertNotNull(response);
        assertEquals("Warranty not found.", response.get("error"));
    }

    @Test
    void testCreateCheckoutSession_AlreadySubscribed() throws Exception {
        Map<String, String> details = new HashMap<>();
        details.put("warrantyId", "92630984");
        details.put("userId", "9def9633-d1d1-4fa0-b9e3-bb249545d95f");
        details.put("currency", "usd");
        details.put("monthlyPrice", "99.99");
        details.put("subscriptionType", "monthly");
        details.put("paymentType", "one_time");

        InsurancePayment mockPayment = new InsurancePayment();
        mockPayment.setSubscriptionId("sub_mock_12345");
        when(insurancePaymentRepository.findByUserIdAndWarrantyId("9def9633-d1d1-4fa0-b9e3-bb249545d95f", "92630984"))
                .thenReturn(Optional.of(mockPayment));

        try (MockedStatic<Subscription> mockedSubscription = mockStatic(Subscription.class)) {
            Subscription mockSubscription = mock(Subscription.class);
            when(mockSubscription.getStatus()).thenReturn("active");
            mockedSubscription.when(() -> Subscription.retrieve("sub_mock_12345")).thenReturn(mockSubscription);

            Map<String, String> response = checkoutService.createCheckoutSession(details);

            assertNotNull(response);
            assertEquals("You already have an active subscription for this warranty.", response.get("error"));
        }
    }
}
