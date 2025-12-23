package com.mlbeez.feeder.service.exception;

public class PaymentChargeNotFoundException extends ApplicationException {
    public PaymentChargeNotFoundException(String message) {
        super(message,"PAYMENT_CHARGE_NOT_FOUND");
    }
}
