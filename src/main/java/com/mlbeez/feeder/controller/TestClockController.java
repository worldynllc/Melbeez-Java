package com.mlbeez.feeder.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.testhelpers.TestClock;
import com.stripe.param.testhelpers.TestClockAdvanceParams;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestClockController {

    @PostMapping("/{clockId}/clock")
    public void testClock(@PathVariable("clockId")String clockId){
        TestClock resource = null;
        try {
            resource = TestClock.retrieve(clockId);
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }

        TestClockAdvanceParams params =
                TestClockAdvanceParams.builder().setFrozenTime(1735910252L).build();

        try {
            TestClock testClock = resource.advance(params);
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }
}
