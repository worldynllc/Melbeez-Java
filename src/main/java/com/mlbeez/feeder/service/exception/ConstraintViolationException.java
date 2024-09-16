package com.mlbeez.feeder.service.exception;

public class ConstraintViolationException extends RuntimeException{

    public ConstraintViolationException(String message){
        super(message);
    }
}
