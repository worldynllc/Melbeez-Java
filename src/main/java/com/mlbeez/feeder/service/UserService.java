package com.mlbeez.feeder.service;
import com.mlbeez.feeder.model.User;
import com.mlbeez.feeder.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User getOrCreateUser(String userId, String userName, String email, String phoneNumber,String firstName,
                                String lastName,String cityName,String stateName,String zipCode,String addressLine1) throws StripeException {
        Optional<User> userDetail = userRepository.findByUserId(userId);
        User user;

        if (userDetail.isPresent()) {
            user = userDetail.get();
            if (user.getCustomerId() == null || user.getCustomerId().isEmpty()) {
                // Create Stripe customer and save customerId to user
                String customerId = createStripeCustomer(userName, email);
                user.setCustomerId(customerId);
                userRepository.save(user);
            }
        } else {
            // Create a new Stripe customer
           String customerId= createStripeCustomer(userName,email);

            // Create a new user in the system
            user = new User();
            user.setUserId(userId);
            user.setCustomerId(customerId);
            user.setUserName(userName);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setCityName(cityName);
            user.setStateName(stateName);
            user.setZipCode(zipCode);
            user.setAddressLine1(addressLine1);
            user.setPhoneNumber(phoneNumber);
            user.setEmail(email);

            userRepository.save(user);
        }

        return user;
    }
    private String createStripeCustomer(String userName, String email) throws StripeException {
        // Create Stripe customer
        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setName(userName)
                .setEmail(email)
                .build();
        Customer customer = Customer.create(customerParams);
        return customer.getId();
    }
}
