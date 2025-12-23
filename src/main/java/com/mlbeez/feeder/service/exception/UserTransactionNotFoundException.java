package com.mlbeez.feeder.service.exception;

public class UserTransactionNotFoundException extends ApplicationException {
    public UserTransactionNotFoundException(String message) {
        super(message,"USER_TRANSACTION_NOT_FOUND");
    }
}
