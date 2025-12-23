package com.mlbeez.feeder.service.exception;

public class InvoiceNotFoundException extends ApplicationException {
    public InvoiceNotFoundException(String message) {
        super(message,"INVOICE_NOT_FOUND");
    }
}
