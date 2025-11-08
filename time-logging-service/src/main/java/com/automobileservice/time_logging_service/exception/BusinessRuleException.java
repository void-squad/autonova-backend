package com.automobileservice.time_logging_service.exception;

public class BusinessRuleException extends RuntimeException {
    
    public BusinessRuleException(String message) {
        super(message);
    }
}