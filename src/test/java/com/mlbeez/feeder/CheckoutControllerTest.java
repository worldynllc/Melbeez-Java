package com.mlbeez.feeder;

import com.mlbeez.feeder.controller.StripeController;
import com.mlbeez.feeder.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CheckoutControllerTest {

    @Mock
    private CheckoutService checkoutService;

    @InjectMocks
    private StripeController stripeController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(stripeController).build();
    }

    @Test
    void testSinglePayment() throws Exception {
        // Sample request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("warrantyId", "warrantyId_123");
        requestBody.put("userId", "userId_123");
        requestBody.put("userName", "John Doe");
        requestBody.put("phoneNumber", "1234567890");
        requestBody.put("email", "john.doe@example.com");
        requestBody.put("currency", "usd");
        requestBody.put("cityName", "New York");
        requestBody.put("stateName", "NY");
        requestBody.put("zipCode", "10001");
        requestBody.put("addressLine1", "123 Main St");
        requestBody.put("firstName", "John");
        requestBody.put("lastName", "Doe");
        requestBody.put("monthlyPrice", "10.00");
        requestBody.put("subscriptionType", "monthly");

        // Sample response from service
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("url", "https://checkout.stripe.com/session/abc123");

        // Mocking the service
        when(checkoutService.createCheckoutSession(requestBody)).thenReturn(responseBody);

        // Perform the payment request
        mockMvc.perform(post("/create-checkout-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warrantyId\":\"warrantyId_123\"," +
                                "\"userId\":\"userId_123\"," +
                                "\"userName\":\"John Doe\"," +
                                "\"phoneNumber\":\"1234567890\"," +
                                "\"email\":\"john.doe@example.com\"," +
                                "\"currency\":\"usd\"," +
                                "\"cityName\":\"New York\"," +
                                "\"stateName\":\"NY\"," +
                                "\"zipCode\":\"10001\"," +
                                "\"addressLine1\":\"123 Main St\"," +
                                "\"firstName\":\"John\"," +
                                "\"lastName\":\"Doe\"," +
                                "\"monthlyPrice\":\"10.00\"," +
                                "\"subscriptionType\":\"monthly\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/session/abc123"));
    }


    @Test
    void testDoublePaymentHandling() throws Exception {
        // Sample request body for first payment
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("warrantyId", "warrantyId_123");
        requestBody.put("userId", "userId_123");
        requestBody.put("userName", "John Doe");
        requestBody.put("phoneNumber", "1234567890");
        requestBody.put("email", "john.doe@example.com");
        requestBody.put("currency", "usd");
        requestBody.put("cityName", "New York");
        requestBody.put("stateName", "NY");
        requestBody.put("zipCode", "10001");
        requestBody.put("addressLine1", "123 Main St");
        requestBody.put("firstName", "John");
        requestBody.put("lastName", "Doe");
        requestBody.put("monthlyPrice", "10.00");
        requestBody.put("subscriptionType", "monthly");

        // Sample response from service for first payment
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("url", "https://checkout.stripe.com/session/abc123");

        // Mocking the service to return the same URL
        when(checkoutService.createCheckoutSession(requestBody)).thenReturn(responseBody);

        // Perform the first payment request
        mockMvc.perform(post("/create-checkout-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warrantyId\":\"warrantyId_123\"," +
                                "\"userId\":\"userId_123\"," +
                                "\"userName\":\"John Doe\"," +
                                "\"phoneNumber\":\"1234567890\"," +
                                "\"email\":\"john.doe@example.com\"," +
                                "\"currency\":\"usd\"," +
                                "\"cityName\":\"New York\"," +
                                "\"stateName\":\"NY\"," +
                                "\"zipCode\":\"10001\"," +
                                "\"addressLine1\":\"123 Main St\"," +
                                "\"firstName\":\"John\"," +
                                "\"lastName\":\"Doe\"," +
                                "\"monthlyPrice\":\"10.00\"," +
                                "\"subscriptionType\":\"monthly\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/session/abc123"));

        // Mocking the service to handle the case of an active subscription
        responseBody.put("error", "You already have an active subscription for this warranty.");
        when(checkoutService.createCheckoutSession(requestBody)).thenReturn(responseBody);

        // Perform the second payment request
        mockMvc.perform(post("/create-checkout-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warrantyId\":\"warrantyId_123\"," +
                                "\"userId\":\"userId_123\"," +
                                "\"userName\":\"John Doe\"," +
                                "\"phoneNumber\":\"1234567890\"," +
                                "\"email\":\"john.doe@example.com\"," +
                                "\"currency\":\"usd\"," +
                                "\"cityName\":\"New York\"," +
                                "\"stateName\":\"NY\"," +
                                "\"zipCode\":\"10001\"," +
                                "\"addressLine1\":\"123 Main St\"," +
                                "\"firstName\":\"John\"," +
                                "\"lastName\":\"Doe\"," +
                                "\"monthlyPrice\":\"10.00\"," +
                                "\"subscriptionType\":\"monthly\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("You already have an active subscription for this warranty."));
    }
    @Test
    void testPaymentWithInvalidData() throws Exception {
        // Invalid request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("warrantyId", "");
        requestBody.put("userId", "userId_123");
        requestBody.put("userName", "John Doe");
        requestBody.put("phoneNumber", "1234567890");
        requestBody.put("email", "john.doe@example.com");
        requestBody.put("currency", "usd");
        requestBody.put("cityName", "New York");
        requestBody.put("stateName", "NY");
        requestBody.put("zipCode", "10001");
        requestBody.put("addressLine1", "123 Main St");
        requestBody.put("firstName", "John");
        requestBody.put("lastName", "Doe");
        requestBody.put("monthlyPrice", "10.00");
        requestBody.put("subscriptionType", "monthly");

        // Mocking the service to return an error
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("error", "Invalid warranty ID.");
        when(checkoutService.createCheckoutSession(requestBody)).thenReturn(responseBody);

        // Perform the payment request with invalid data
        mockMvc.perform(post("/create-checkout-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warrantyId\":\"\"," +
                                "\"userId\":\"userId_123\"," +
                                "\"userName\":\"John Doe\"," +
                                "\"phoneNumber\":\"1234567890\"," +
                                "\"email\":\"john.doe@example.com\"," +
                                "\"currency\":\"usd\"," +
                                "\"cityName\":\"New York\"," +
                                "\"stateName\":\"NY\"," +
                                "\"zipCode\":\"10001\"," +
                                "\"addressLine1\":\"123 Main St\"," +
                                "\"firstName\":\"John\"," +
                                "\"lastName\":\"Doe\"," +
                                "\"monthlyPrice\":\"10.00\"," +
                                "\"subscriptionType\":\"monthly\"}")) // Invalid warranty ID
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid warranty ID."));
    }
}