package com.petbuddy.feedDistributionService.exception;

public class FeedDistributionException extends RuntimeException{
    public FeedDistributionException(String message, Exception e){
        super(message,e);
    }
}
