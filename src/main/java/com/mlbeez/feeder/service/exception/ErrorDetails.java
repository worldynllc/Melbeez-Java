package com.mlbeez.feeder.service.exception;

import lombok.Getter;

@Getter
public class ErrorDetails {
    private String message;
    private String details;

    public ErrorDetails(String message, String details) {
        super();
        this.message = message;
        this.details = details;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
