package com.mlbeez.feeder.service.exception;

public class RequiredWarrantyIdException extends ApplicationException {
    public RequiredWarrantyIdException(String message) {
        super(message,"REQUIRED_WARRANTY_ID");
    }
}
