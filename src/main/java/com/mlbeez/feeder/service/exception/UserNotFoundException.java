package com.mlbeez.feeder.service.exception;

public class UserNotFoundException extends ApplicationException {
    public UserNotFoundException(String message) {
        super(message,"USER_NOT_FOUND");
    }
}
