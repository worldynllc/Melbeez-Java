package com.mlbeez.feeder.service.exception;

public class FeedNotFoundException extends ApplicationException {
    public FeedNotFoundException(String message) {
        super(message,"FEED_NOT_FOUND");
    }
}
