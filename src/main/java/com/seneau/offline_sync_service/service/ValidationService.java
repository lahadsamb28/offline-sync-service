package com.seneau.offline_sync_service.service;

import com.seneau.offline_sync_service.web.dto.PointageBatchRequestDto;
import com.seneau.offline_sync_service.web.dto.PointageOfflineDto;
import com.seneau.offline_sync_service.web.dto.PointageTerrainBatchRequestDto;
import com.seneau.offline_sync_service.web.dto.PointageTerrainOfflineDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ValidationService {

    private static final int MAX_BATCH_SIZE = 1000;
    private static final int MAX_DAYS_OLD = 30;

    /**
     * Valider un batch de pointages STANDARDS
     */
    public List<String> validateBatchStandard(PointageBatchRequestDto request) {
        List<String> errors = new ArrayList<>();

        if (request.getPointages() == null || request.getPointages().isEmpty()) {
            errors.add("La liste des pointages est vide");
            return errors;
        }

        if (request.getPointages().size() > MAX_BATCH_SIZE) {
            errors.add(String.format("Trop de pointages dans le batch (max: %d)", MAX_BATCH_SIZE));
        }

        for (int i = 0; i < request.getPointages().size(); i++) {
            PointageOfflineDto pointage = request.getPointages().get(i);
            List<String> pointageErrors = validatePointageStandard(pointage, i);
            errors.addAll(pointageErrors);
        }

        return errors;
    }

    /**
     * Valider un batch de pointages TERRAIN
     */
    public List<String> validateBatchTerrain(PointageTerrainBatchRequestDto request) {
        List<String> errors = new ArrayList<>();

        if (request.getPointages() == null || request.getPointages().isEmpty()) {
            errors.add("La liste des pointages terrain est vide");
            return errors;
        }

        if (request.getPointages().size() > MAX_BATCH_SIZE) {
            errors.add(String.format("Trop de pointages dans le batch (max: %d)", MAX_BATCH_SIZE));
        }

        for (int i = 0; i < request.getPointages().size(); i++) {
            PointageTerrainOfflineDto pointage = request.getPointages().get(i);
            List<String> pointageErrors = validatePointageTerrain(pointage, i);
            errors.addAll(pointageErrors);
        }

        return errors;
    }

    /**
     * Valider un pointage STANDARD
     */
    private List<String> validatePointageStandard(PointageOfflineDto pointage, int index) {
        List<String> errors = new ArrayList<>();
        String prefix = String.format("Pointage[%d]: ", index);

        if (pointage.getMatricule() == null || pointage.getMatricule() <= 0) {
            errors.add(prefix + "Matricule invalide");
        }

        if (pointage.getHeurePointage() == null) {
            errors.add(prefix + "Heure de pointage manquante");
        } else {
            errors.addAll(validateHeurePointage(pointage.getHeurePointage(), prefix));
        }

        return errors;
    }

    /**
     * Valider un pointage TERRAIN
     */
    private List<String> validatePointageTerrain(PointageTerrainOfflineDto pointage, int index) {
        List<String> errors = new ArrayList<>();
        String prefix = String.format("Pointage Terrain[%d]: ", index);

        if (pointage.getHeurePointage() == null) {
            errors.add(prefix + "Heure de pointage manquante");
        } else {
            errors.addAll(validateHeurePointage(pointage.getHeurePointage(), prefix));
        }

        // Validation GPS obligatoire pour terrain
        if (pointage.getLatitude() == null) {
            errors.add(prefix + "Latitude obligatoire pour les pointages terrain");
        } else if (pointage.getLatitude() < -90 || pointage.getLatitude() > 90) {
            errors.add(prefix + "Latitude invalide (doit être entre -90 et 90)");
        }

        if (pointage.getLongitude() == null) {
            errors.add(prefix + "Longitude obligatoire pour les pointages terrain");
        } else if (pointage.getLongitude() < -180 || pointage.getLongitude() > 180) {
            errors.add(prefix + "Longitude invalide (doit être entre -180 et 180)");
        }

        return errors;
    }

    /**
     * Valider l'heure de pointage (commun aux 2 types)
     */
    private List<String> validateHeurePointage(LocalDateTime heurePointage, String prefix) {
        List<String> errors = new ArrayList<>();

        // Vérifier que le pointage n'est pas trop ancien
        LocalDateTime oldest = LocalDateTime.now().minusDays(MAX_DAYS_OLD);
        if (heurePointage.isBefore(oldest)) {
            errors.add(prefix + String.format("Pointage trop ancien (max: %d jours)", MAX_DAYS_OLD));
        }

        // Vérifier que le pointage n'est pas dans le futur
        if (heurePointage.isAfter(LocalDateTime.now())) {
            errors.add(prefix + "Pointage dans le futur non autorisé");
        }

        return errors;
    }

    /**
     * Valider des coordonnées GPS
     */
    public boolean isValidCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return latitude >= -90 && latitude <= 90 && 
               longitude >= -180 && longitude <= 180;
    }
}