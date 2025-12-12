package com.seneau.offline_sync_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointageOfflineDto {
    private Long matricule;
    private Long dispositifId;
    private LocalDateTime heurePointage;
}
