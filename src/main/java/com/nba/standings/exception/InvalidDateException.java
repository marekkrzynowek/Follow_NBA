package com.nba.standings.exception;

/**
 * Exception thrown when date validation fails.
 * This exception is thrown when a user provides a date that is invalid
 * (e.g., before the current NBA season started or in the future).
 */
public class InvalidDateException extends RuntimeException {
    
    public InvalidDateException(String message) {
        super(message);
    }
    
    public InvalidDateException(String message, Throwable cause) {
        super(message, cause);
    }
}
