package com.mlbeez.feeder.service.exception;

public class CommentEmptyException extends ApplicationException {
    public CommentEmptyException(String message) {
        super(message,"COMMENT_EMPTY");
    }
}
