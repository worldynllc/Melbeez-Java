package com.mlbeez.feeder.service.exception;

public class UserIdRequiredException extends ApplicationException {
    public UserIdRequiredException(String message) {
        super(message,"USER_ID_REQUIRED");
    }
}
