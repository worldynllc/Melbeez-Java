package com.mlbeez.feeder.service.exception;

public class InternalServerException extends RuntimeException{

    public InternalServerException(String message){
        super(message);
    }
}
