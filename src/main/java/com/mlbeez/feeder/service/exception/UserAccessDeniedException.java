package com.mlbeez.feeder.service.exception;

public class UserAccessDeniedException extends ApplicationException {
    public UserAccessDeniedException(String message) {
        super(message,"USER_ACCESS_DENIED");
    }
}
