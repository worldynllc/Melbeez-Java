package com.mlbeez.feeder.service.exception;

public class WarrantyNotFoundException extends ApplicationException {
    public WarrantyNotFoundException(String message) {
        super(message,"WARRANTY_NOT_FOUND");
    }
}
