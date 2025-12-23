package com.mlbeez.feeder.service.exception;

public class FeedIdRequiredException extends ApplicationException {
    public FeedIdRequiredException(String message) {
        super(message,"FEED_ID_REQUIRED");
    }
}
