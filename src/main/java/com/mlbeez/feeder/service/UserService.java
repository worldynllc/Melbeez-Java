package com.mlbeez.feeder.service;
import com.mlbeez.feeder.model.User;
import com.mlbeez.feeder.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.testhelpers.TestClock;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.testhelpers.TestClockCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final Logger logger= LoggerFactory.getLogger(UserService.class);

    public User getOrCreateUser(String userId, String userName, String email, String phoneNumber,String firstName,
                                String lastName,String cityName,String stateName,String zipCode,String addressLine1) throws StripeException {
        logger.info("Requested to Get the User and Stripe Customer or Create the User and Stripe Customer");

        Optional<User> userDetail = userRepository.findByUserId(userId);
        User user;

        if (userDetail.isPresent()) {
            user = userDetail.get();
            if (user.getCustomerId() == null || user.getCustomerId().isEmpty()) {
                String customerId = createStripeCustomer(userName, email,userId);
                user.setCustomerId(customerId);
                userRepository.save(user);
            }
        } else {
           String customerId= createStripeCustomer(userName,email,userId);

            logger.info("Requested to Create the User");

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
    private String createStripeCustomer(String userName, String email,String userId) throws StripeException {
        logger.info("Requested to Create the Stripe Customer and Test clock");

        TestClockCreateParams params =
                TestClockCreateParams.builder()
                        .setFrozenTime(1733549212L)
                        .setName("Monthly/Yearly renewal")
                        .build();
        TestClock testClock = TestClock.create(params);

        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setName(userName)
                .setEmail(email)
                .setTestClock(testClock.getId())
                .putMetadata("userId",userId)
                .build();
        Customer customer = Customer.create(customerParams);
        return customer.getId();
    }

}
