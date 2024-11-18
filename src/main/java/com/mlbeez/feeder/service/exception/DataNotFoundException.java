package com.mlbeez.feeder.service.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DataNotFoundException extends RuntimeException {
    private String details;

    public DataNotFoundException(String message) {
        super(message);
    }

    public DataNotFoundException(String message, String details) {
        super(message);
        this.details = details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
