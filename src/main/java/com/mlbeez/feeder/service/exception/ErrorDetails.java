package com.mlbeez.feeder.service.exception;

import lombok.Getter;

@Getter
public class ErrorDetails {
    private String message;


    public ErrorDetails(String message) {
        super();
        this.message = message;

    }

    public void setMessage(String message) {
        this.message = message;
    }

}
