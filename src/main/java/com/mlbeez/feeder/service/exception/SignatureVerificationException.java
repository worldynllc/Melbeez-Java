package com.mlbeez.feeder.service.exception;

public class SignatureVerificationException extends ApplicationException {
    public SignatureVerificationException(String message) {
        super(message,"INVALID_SIGNATURE");
    }
}
