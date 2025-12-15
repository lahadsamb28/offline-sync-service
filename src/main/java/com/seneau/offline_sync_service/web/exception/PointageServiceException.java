package com.seneau.offline_sync_service.web.exception;

public class PointageServiceException extends RuntimeException {
    public PointageServiceException(String message) {
        super(message);
    }
    
    public PointageServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}