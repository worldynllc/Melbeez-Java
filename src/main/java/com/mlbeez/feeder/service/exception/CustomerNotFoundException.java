package com.mlbeez.feeder.service.exception;

public class CustomerNotFoundException extends ApplicationException {
    public CustomerNotFoundException(String message) {
        super(message,"CUSTOMER_NOT_FOUND");
    }
}
