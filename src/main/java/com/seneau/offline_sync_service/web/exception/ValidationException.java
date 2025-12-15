package com.seneau.offline_sync_service.web.exception;

import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {
    private final List<String> errors;
    
    public ValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }
}
