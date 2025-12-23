package com.mlbeez.feeder.service.exception;

public class ProductNotFoundException extends ApplicationException {
    public ProductNotFoundException(String message) {
        super(message,"PRODUCT_NOT_FOUND");
    }
}
