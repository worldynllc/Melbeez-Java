package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.UserRequest;
import com.mlbeez.feeder.service.ThirdPartyService;
import com.mlbeez.feeder.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class UserController {

    private final ThirdPartyService thirdPartyService;

    @Autowired
    private UserService userService;

    public UserController(ThirdPartyService thirdPartyService) {
        this.thirdPartyService = thirdPartyService;
    }

    @PostMapping("/send-user")
    public Mono<String> sendUserDetails(@RequestBody UserRequest userRequest) {
        return thirdPartyService.sendUserDetails(userRequest);
    }

}
