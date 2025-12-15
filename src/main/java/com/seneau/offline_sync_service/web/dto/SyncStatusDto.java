package com.seneau.offline_sync_service.web.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncStatusDto {
    private String batchId;
    private String status;
    private Integer totalPointages;
    private Integer successCount;
    private Integer failureCount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    
    public Long getDurationSeconds() {
        if (createdAt != null && completedAt != null) {
            return java.time.Duration.between(createdAt, completedAt).getSeconds();
        }
        return null;
    }
}