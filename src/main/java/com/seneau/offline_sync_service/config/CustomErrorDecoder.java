package com.seneau.offline_sync_service.config;


import com.seneau.offline_sync_service.web.exception.PointageServiceException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class CustomErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("❌ Erreur Feign - Method: {}, Status: {}", methodKey, response.status());
        
        String errorMessage = extractErrorMessage(response);
        
        switch (response.status()) {
            case 400:
                return new PointageServiceException(
                    "Données invalides: " + errorMessage
                );
            case 404:
                return new PointageServiceException(
                    "Service de pointage non disponible"
                );
            case 500:
                return new PointageServiceException(
                    "Erreur interne du service de pointage: " + errorMessage
                );
            default:
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }
    
    private String extractErrorMessage(Response response) {
        try {
            if (response.body() != null) {
                return new String(
                    response.body().asInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
                );
            }
        } catch (IOException e) {
            log.error("Erreur lors de la lecture du corps de la réponse", e);
        }
        return "Erreur inconnue";
    }
}