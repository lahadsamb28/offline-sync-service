package com.seneau.offline_sync_service.web.dto;

import com.seneau.offline_sync_service.data.model.TypePointageSync;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Message envoyé dans RabbitMQ pour la synchronisation
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncMessageDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String batchId;
    private String email;
    private TypePointageSync typePointage; // STANDARD ou TERRAIN

    // Pour les pointages standards
    private List<PointageOfflineDto> pointages;

    // Pour les pointages terrain
    private List<PointageTerrainOfflineDto> pointagesTerrain;

    private List<String> privileges;
    private LocalDateTime createdAt;
    private Integer retryCount;

    @Builder.Default
    private Integer maxRetries = 3;
}