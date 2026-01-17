package com.petbuddy.social_feed.Exception;

public class UnauthorizedDeleteException extends RuntimeException {
    public UnauthorizedDeleteException(String message) {
        super(message);
    }
}
