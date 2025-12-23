package com.mlbeez.feeder.service.exception;

public class WarrantyNameAndPlanNotFoundException extends ApplicationException {
    public WarrantyNameAndPlanNotFoundException(String message) {
        super(message,"WARRANTY_NAME || WARRANTY_PLAN NOT FOUND");
    }
}
