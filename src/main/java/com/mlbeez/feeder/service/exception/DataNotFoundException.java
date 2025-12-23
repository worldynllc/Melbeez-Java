package com.mlbeez.feeder.service.exception;

import lombok.Getter;

@Getter
public class DataNotFoundException extends ApplicationException {

    public DataNotFoundException(String message) {
        super(message,"DATA_NOT_FOUND");
    }

}
