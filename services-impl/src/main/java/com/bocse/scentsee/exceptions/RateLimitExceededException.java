package com.bocse.scentsee.exceptions;

/**
 * Created by bocse on 02.03.2016.
 */
public class RateLimitExceededException extends IllegalAccessException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
