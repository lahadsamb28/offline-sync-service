package com.seneau.offline_sync_service.web.controller;

import com.seneau.offline_sync_service.web.dto.*;
import com.seneau.offline_sync_service.service.SyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/offline-sync")
@RequiredArgsConstructor
@Slf4j
//@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class SyncController {

    private final SyncService offlineSyncService;

    /**
     * Synchroniser des pointages STANDARDS (avec matricule)
     * Exemple: Agent pointant pour d'autres agents avec un badge
     */
    @PostMapping("/pointages/sync")
    public ResponseEntity<SyncResponseDto> syncPointagesStandard(
            @Valid @RequestBody PointageBatchRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("📥 Synchronisation STANDARD de {} pointages pour {}",
                 request.getPointages().size(), userDetails.getUsername());

        SyncResponseDto response = offlineSyncService.syncPointagesStandard(
                userDetails.getUsername(),
                request
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Synchroniser des pointages TERRAIN (avec GPS, sans matricule)
     * Exemple: Agent pointant pour lui-même sur le terrain via mobile
     */
    @PostMapping("/pointages/terrain/sync")
    public ResponseEntity<SyncResponseDto> syncPointagesTerrain(
            @Valid @RequestBody PointageTerrainBatchRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "X-User-Privileges", required = false) List<String> privileges) {

        log.info("📥 Synchronisation TERRAIN de {} pointages pour {}",
                 request.getPointages().size(), userDetails.getUsername());

        // Par défaut, on considère que c'est un collaborateur terrain
        if (privileges == null || privileges.isEmpty()) {
            privileges = List.of("AUTH_COLLABORATEUR");
        }

        SyncResponseDto response = offlineSyncService.syncPointagesTerrain(
                userDetails.getUsername(),
                request,
                privileges
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour vérifier le statut de la synchronisation
     */
    @GetMapping("/status/{batchId}")
    public ResponseEntity<SyncStatusDto> getSyncStatus(@PathVariable String batchId) {
        SyncStatusDto status = offlineSyncService.getSyncStatus(batchId);
        return ResponseEntity.ok(status);
    }

    /**
     * Endpoint pour récupérer les échecs de synchronisation
     */
    @GetMapping("/failures")
    public ResponseEntity<List<PointageBatchDto>> getFailedSyncs(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<PointageBatchDto> failures = offlineSyncService.getFailedSyncs(
                userDetails.getUsername()
        );

        return ResponseEntity.ok(failures);
    }

    /**
     * Endpoint pour réessayer les synchronisations échouées
     */
    @PostMapping("/retry/{batchId}")
    public ResponseEntity<SyncResponseDto> retrySyncBatch(@PathVariable String batchId) {
        SyncResponseDto response = offlineSyncService.retrySync(batchId);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de santé pour vérifier la connectivité
     */
    @GetMapping("/health")
    public ResponseEntity<HealthDto> health() {
        return ResponseEntity.ok(
                HealthDto.builder()
                        .status("UP")
                        .message("Offline Sync Service opérationnel")
                        .build()
        );
    }
}
