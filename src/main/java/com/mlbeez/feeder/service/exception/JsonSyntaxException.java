package com.mlbeez.feeder.service.exception;

public class JsonSyntaxException extends ApplicationException {
    public JsonSyntaxException(String message) {
        super(message,"INVALID_JSON_SYNTAX");
    }
}
