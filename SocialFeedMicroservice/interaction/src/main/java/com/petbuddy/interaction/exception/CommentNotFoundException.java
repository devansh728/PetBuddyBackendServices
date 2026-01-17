package com.petbuddy.interaction.exception;

/**
 * Exception thrown when a comment is not found
 */
public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(String message) {
        super(message);
    }

    public CommentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

