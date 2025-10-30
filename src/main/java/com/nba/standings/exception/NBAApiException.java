package com.nba.standings.exception;

/**
 * Exception thrown when NBA API communication fails.
 * This exception is thrown immediately without retries to respect API rate limits.
 */
public class NBAApiException extends RuntimeException {
    
    public NBAApiException(String message) {
        super(message);
    }
    
    public NBAApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
