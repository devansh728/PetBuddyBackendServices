package com.petbuddy.interaction.exception;

/**
 * Exception thrown when a user tries to unlike a post they haven't liked
 */
public class LikeNotFoundException extends RuntimeException {

    public LikeNotFoundException(String message) {
        super(message);
    }

    public LikeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

