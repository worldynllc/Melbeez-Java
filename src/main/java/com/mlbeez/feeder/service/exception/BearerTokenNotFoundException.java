package com.mlbeez.feeder.service.exception;

public class BearerTokenNotFoundException extends ApplicationException {
    public BearerTokenNotFoundException(String message) {
        super(message,"BEARER_TOKEN_NOT_FOUND");
    }
}
