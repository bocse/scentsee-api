package com.bocse.scentsee.exceptions;

/**
 * Created by bocse on 02.03.2016.
 */
public class UnauthorizedAccessException extends IllegalAccessException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
