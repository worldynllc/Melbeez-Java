package com.mlbeez.feeder;

import com.mlbeez.feeder.model.User;
import com.mlbeez.feeder.model.Warranty;
import com.mlbeez.feeder.repository.InsurancePaymentRepository;
import com.mlbeez.feeder.repository.WarrantyRepository;
import com.mlbeez.feeder.service.CheckoutService;
import com.mlbeez.feeder.service.UserService;
import com.stripe.Stripe;
import com.stripe.model.Price;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CheckoutServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private WarrantyRepository warrantyRepository;

    @Mock
    private InsurancePaymentRepository insurancePaymentRepository;

    @InjectMocks
    private CheckoutService checkoutService;

    private Map<String, String> details;

    @Value("${stripe.api.key}")
    public String stripeApiKey;

    @BeforeEach
    void setUp() {
        // Set your Stripe API key here
        Stripe.apiKey = stripeApiKey;

        details = new HashMap<>();
        details.put("warrantyId", "warranty123");
        details.put("userId", "user123");
        details.put("userName", "John Doe");
        details.put("phoneNumber", "1234567890");
        details.put("email", "john.doe@example.com");
        details.put("currency", "usd");
        details.put("cityName", "New York");
        details.put("stateName", "NY");
        details.put("zipCode", "10001");
        details.put("addressLine1", "123 Main St");
        details.put("firstName", "John");
        details.put("lastName", "Doe");
        details.put("monthlyPrice", "10.00");
        details.put("subscriptionType", "monthly");
    }

    @Test
    void testCreateCheckoutSession_Success() throws Exception {

        // Mock UserService response
        User mockUser = new User();
        mockUser.setCustomerId("cus_12345");
        when(userService.getOrCreateUser(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(mockUser);

        // Mock WarrantyRepository response
        Warranty mockWarranty = new Warranty();
        mockWarranty.setProductId("prod_12345");
        when(warrantyRepository.findByWarrantyId(anyString())).thenReturn(Optional.of(mockWarranty));

        // Mock InsurancePaymentRepository response
        when(insurancePaymentRepository.findAllByProductId(anyString())).thenReturn(List.of());

        // Mock Stripe interactions with static mocking
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class);
             MockedStatic<Price> mockedPrice = mockStatic(Price.class)) {

            // Mock the Price creation
            Price mockPrice = mock(Price.class);
            when(mockPrice.getId()).thenReturn("price_12345");
            mockedPrice.when(() -> Price.create(any(PriceCreateParams.class))).thenReturn(mockPrice);

            // Mock the Session creation
            Session mockSession = mock(Session.class);
            when(mockSession.getUrl()).thenReturn("https://mock-session-url.com");
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class), any(RequestOptions.class))).thenReturn(mockSession);

            // Call the method to test
            Map<String, String> response = checkoutService.createCheckoutSession(details);

            // Verify and assert the results
            assertNotNull(response);
            assertEquals("https://mock-session-url.com", response.get("url"));
        }

    }
      @Test
        void testCreateCheckoutSession_WarrantyNotFound() throws Exception {
        // Mock WarrantyRepository response
        when(warrantyRepository.findByWarrantyId(anyString())).thenReturn(Optional.empty());

        // Call the method to test
        Map<String, String> response = checkoutService.createCheckoutSession(details);

        // Verify and assert the results
        assertNotNull(response);
        assertEquals("Warranty not found.", response.get("error"));
        verify(userService, times(1)).getOrCreateUser(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(warrantyRepository, times(1)).findByWarrantyId(anyString());
    }
}
