package com.mlbeez.feeder.service.exception;

public class TokenValidationFailedException extends ApplicationException {
    public TokenValidationFailedException(String message) {
        super(message,"TOKEN_VALIDATION_FAILED");
    }
}
