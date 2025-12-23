package com.mlbeez.feeder.service.exception;

public class PaymentMethodNotFoundException extends ApplicationException  {
    public PaymentMethodNotFoundException(String message) {
        super(message,"PAYMENT_METHOD_NOT_FOUND");
    }
}
