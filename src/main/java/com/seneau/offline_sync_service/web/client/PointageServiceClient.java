package com.seneau.offline_sync_service.web.client;

import com.seneau.offline_sync_service.config.FeignConfig;
import com.seneau.offline_sync_service.web.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Client Feign pour communiquer avec le microservice de pointage
 * Supporte 2 endpoints: standard et terrain
 */
@FeignClient(
    name = "pointage-service",
    url = "${services.pointage.url:http://localhost:8081}",
    configuration = FeignConfig.class
)
public interface PointageServiceClient {

    /**
     * Enregistrer des pointages standards (avec matricule)
     */
    @PostMapping("/api/v1/pointages/offline/batch")
    List<PointageBatchDto> enregistrerPointagesOffline(
            @RequestParam("email") String email,
            @RequestBody PointageBatchRequestDto batchDto
    );

    /**
     * Enregistrer des pointages terrain (avec GPS, sans matricule)
     * L'agent est identifié par son email/token
     */
    @PostMapping("/api/v1/pointages/offline/terrain/batch")
    List<PointageBatchDto> enregistrerPointagesOfflineTerrain(
            @RequestParam("email") String email,
            @RequestBody PointageTerrainBatchRequestDto batchDto,
            @RequestHeader("X-User-Privileges") List<String> privileges
    );
}
