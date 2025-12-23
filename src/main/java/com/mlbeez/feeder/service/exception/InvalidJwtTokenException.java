package com.mlbeez.feeder.service.exception;

public class InvalidJwtTokenException extends ApplicationException {
    public InvalidJwtTokenException(String message) {
        super(message,"INVALID_JWT_TOKEN");
    }
}
