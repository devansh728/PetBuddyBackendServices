package com.petbuddy.interaction.exception;

/**
 * Exception thrown when a user tries to like a post they've already liked
 */
public class DuplicateLikeException extends RuntimeException {

    public DuplicateLikeException(String message) {
        super(message);
    }

    public DuplicateLikeException(String message, Throwable cause) {
        super(message, cause);
    }
}

