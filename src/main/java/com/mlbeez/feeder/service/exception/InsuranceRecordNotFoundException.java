package com.mlbeez.feeder.service.exception;

public class InsuranceRecordNotFoundException extends ApplicationException {
    public InsuranceRecordNotFoundException(String message) {
        super(message,"INSURANCE_RECORD_NOT_FOUND");
    }
}
