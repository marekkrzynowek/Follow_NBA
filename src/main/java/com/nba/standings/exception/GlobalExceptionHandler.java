package com.nba.standings.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;

/**
 * Global exception handler for the NBA Standings Viewer application.
 * Provides RFC 7807 compliant error responses using Spring's ProblemDetail.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles InvalidDateException when date validation fails.
     * Returns 400 Bad Request with ProblemDetail.
     */
    @ExceptionHandler(InvalidDateException.class)
    public ResponseEntity<ProblemDetail> handleInvalidDate(
            InvalidDateException ex, 
            WebRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, 
                ex.getMessage()
        );
        problemDetail.setTitle("Bad Request");
        problemDetail.setInstance(getRequestUri(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handles MissingServletRequestParameterException when required parameters are missing.
     * Returns 400 Bad Request with ProblemDetail.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(
            MissingServletRequestParameterException ex, 
            WebRequest request) {
        
        String detail = String.format(
                "Required parameter '%s' is missing",
                ex.getParameterName()
        );
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, 
                detail
        );
        problemDetail.setTitle("Bad Request");
        problemDetail.setInstance(getRequestUri(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handles MethodArgumentTypeMismatchException for invalid enum values.
     * Returns 400 Bad Request with ProblemDetail.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleInvalidEnum(
            MethodArgumentTypeMismatchException ex, 
            WebRequest request) {
        
        String detail;
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            detail = String.format(
                    "Invalid value '%s' for parameter '%s'. Must be one of: %s",
                    ex.getValue(),
                    ex.getName(),
                    String.join(", ", getEnumValues(ex.getRequiredType()))
            );
        } else {
            detail = String.format(
                    "Invalid value '%s' for parameter '%s'",
                    ex.getValue(),
                    ex.getName()
            );
        }
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, 
                detail
        );
        problemDetail.setTitle("Bad Request");
        problemDetail.setInstance(getRequestUri(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handles NBAApiException when NBA API communication fails.
     * Returns 500 Internal Server Error with ProblemDetail.
     */
    @ExceptionHandler(NBAApiException.class)
    public ResponseEntity<ProblemDetail> handleNBAApiError(
            NBAApiException ex, 
            WebRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                ex.getMessage()
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setInstance(getRequestUri(request));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    /**
     * Handles all other unhandled exceptions.
     * Returns 500 Internal Server Error with ProblemDetail.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericError(
            Exception ex, 
            WebRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "An unexpected error occurred. Please try again later."
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setInstance(getRequestUri(request));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    /**
     * Helper method to extract enum constant names.
     */
    private String[] getEnumValues(Class<?> enumClass) {
        Object[] enumConstants = enumClass.getEnumConstants();
        String[] values = new String[enumConstants.length];
        for (int i = 0; i < enumConstants.length; i++) {
            values[i] = enumConstants[i].toString();
        }
        return values;
    }

    /**
     * Helper method to extract the full request URI including query string.
     */
    private URI getRequestUri(WebRequest request) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        String fullUri = servletRequest.getRequestURL().toString();
        String query = servletRequest.getQueryString();
        if (query != null) {
            fullUri += "?" + query;
        }
        return URI.create(fullUri);
    }
}
