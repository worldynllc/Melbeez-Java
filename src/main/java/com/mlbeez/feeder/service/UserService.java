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

    public User storeUser(User user)
    {
        return userRepository.save(user);
    }

    public User getOrCreateUser(String userId, String userName, String email, String phoneNumber) throws StripeException {
        Optional<User> userDetail = userRepository.findByUserId(userId);
        User user;

        if (userDetail.isPresent()) {
            user = userDetail.get();
            if (user.getCustomerId() == null) {
                // Create Stripe customer
                CustomerCreateParams customerParams = CustomerCreateParams.builder()
                        .setName(userName)
                        .setEmail(email)
                        .build();
                Customer customer = Customer.create(customerParams);

                // Save Stripe customerId to user
                user.setCustomerId(customer.getId());
                userRepository.save(user);
            }
        } else {
            // Create a new Stripe customer
            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                    .setName(userName)
                    .setEmail(email)
                    .build();
            Customer customer = Customer.create(customerParams);

            // Create a new user in the system
            user = new User();
            user.setUserId(userId);
            user.setCustomerId(customer.getId());
            user.setUserName(userName);
            user.setPhoneNumber(phoneNumber);
            user.setEmail(email);

            userRepository.save(user);
        }

        return user;
    }


}
