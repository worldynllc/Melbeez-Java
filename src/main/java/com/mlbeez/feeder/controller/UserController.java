package com.mlbeez.feeder.controller;
import com.mlbeez.feeder.service.UserService;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {


    @Autowired
    private UserService userService;

    //using for store the user details in local database
    @PostMapping("/create-user")
    public Map<String,String> createUser(@RequestBody Map<String,String> details) throws StripeException {
        String userId = details.get("userId");
        String userName = details.get("userName");
        String phoneNumber = details.get("phoneNumber");
        String email = details.get("email");
        String cityName = details.get("cityName");
        String stateName = details.get("stateName");
        String zipCode = details.get("zipCode");
        String addressLine1 = details.get("addressLine1");
        String firstName = details.get("firstName");
        String lastName = details.get("lastName");

        userService.getOrCreateUser(userId, userName, email, phoneNumber, firstName, lastName, cityName,
                stateName, zipCode, addressLine1);
        return details;
    }
}
