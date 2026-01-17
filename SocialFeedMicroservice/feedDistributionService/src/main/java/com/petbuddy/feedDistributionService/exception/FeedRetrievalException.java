package com.petbuddy.feedDistributionService.exception;

/**
 * Exception thrown when feed retrieval fails
 */
public class FeedRetrievalException extends Exception {

    public FeedRetrievalException(String message) {
        super(message);
    }

    public FeedRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}

