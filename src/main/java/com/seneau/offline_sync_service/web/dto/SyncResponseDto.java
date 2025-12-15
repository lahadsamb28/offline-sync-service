package com.seneau.offline_sync_service.web.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncResponseDto {
    
    private String batchId;
    private String status;
    private Integer totalPointages;
    private Integer successCount;
    private Integer failureCount;
    private String message;
    private List<PointageBatchDto> results;
    private List<String> validationErrors;
    private Boolean sentToQueue;
}