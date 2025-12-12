package com.seneau.offline_sync_service.controller;

import com.seneau.offline_sync_service.dto.PointageBatchRequestDto;
import com.seneau.offline_sync_service.entity.OfflinePointage;
import com.seneau.offline_sync_service.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pointage")
@RequiredArgsConstructor
@Slf4j
public class SyncController {
    private final SyncService service;

    @PostMapping("/batch")
    public ResponseEntity<?> enregistrerBatch(@RequestBody PointageBatchRequestDto dto) {
        try {
            OfflinePointage saved = service.traiterBatch(dto);
            log.info("Batch reçu et enregistré");
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Erreur enregistrement batch: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Erreur: " + e.getMessage());
        }
    }

    @GetMapping("/ping")
    public String ping() {
        return "Offline Sync Service opérationnel";
    }
}
