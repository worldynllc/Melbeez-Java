package com.mlbeez.feeder.service.exception;

public class InternalServerException extends RuntimeException{

    public InternalServerException(String message, Exception eMessage){
        super(message);
    }
}
