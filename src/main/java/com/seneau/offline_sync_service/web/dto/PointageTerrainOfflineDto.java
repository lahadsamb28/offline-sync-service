package com.seneau.offline_sync_service.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointageTerrainOfflineDto implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    @NotNull(message = "L'heure de pointage est obligatoire")
    @PastOrPresent(message = "L'heure de pointage ne peut pas être dans le futur")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime heurePointage;
    
    @NotNull(message = "La latitude est obligatoire pour les pointages terrain")
    @DecimalMin(value = "-90.0", message = "Latitude invalide")
    @DecimalMax(value = "90.0", message = "Latitude invalide")
    private Double latitude;
    
    @NotNull(message = "La longitude est obligatoire pour les pointages terrain")
    @DecimalMin(value = "-180.0", message = "Longitude invalide")
    @DecimalMax(value = "180.0", message = "Longitude invalide")
    private Double longitude;
}