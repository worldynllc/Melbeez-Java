package com.mlbeez.feeder.service.exception;

public class InvalidRequestException extends ApplicationException {
    public InvalidRequestException(String message) {
        super(message,"SubscriptionID Invalid");
    }
}
