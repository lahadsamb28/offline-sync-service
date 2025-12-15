// Exceptions personnalisées
package com.seneau.offline_sync_service.web.exception;

public class SyncException extends RuntimeException {
    public SyncException(String message) {
        super(message);
    }
    
    public SyncException(String message, Throwable cause) {
        super(message, cause);
    }
}