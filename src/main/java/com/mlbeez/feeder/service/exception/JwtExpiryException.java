package com.mlbeez.feeder.service.exception;

public class JwtExpiryException extends ApplicationException {
    public JwtExpiryException(String message) {
        super(message,"JWT_EXPIRY");
    }
}
