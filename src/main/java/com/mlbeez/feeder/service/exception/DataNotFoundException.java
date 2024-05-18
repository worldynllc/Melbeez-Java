package com.mlbeez.feeder.service.exception;

import java.io.Serial;


public class DataNotFoundException extends RuntimeException{

    @Serial
    private static final long serialVersionUID = 1L;
    private String message;
    private String details;

    public DataNotFoundException(String message, String details, Throwable e) {
        super(e);
        this.message = message;
        this.details = details;
    }

    public DataNotFoundException(String message, String details) {

        this.message = message;
        this.details = details;
    }
    public DataNotFoundException(String message) {
        super(message);
    }



    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

}


