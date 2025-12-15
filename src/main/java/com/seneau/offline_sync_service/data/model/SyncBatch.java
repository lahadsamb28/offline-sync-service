package com.seneau.offline_sync_service.data.model;

import com.seneau.offline_sync_service.web.dto.PointageBatchDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sync_batches", indexes = {
    @Index(name = "idx_batch_id", columnList = "batchId"),
    @Index(name = "idx_email_status", columnList = "email, status"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 36)
    private String batchId;

    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false, length = 20)
    private String typePointage; // STANDARD ou TERRAIN

    @Column(nullable = false)
    private Integer totalPointages;

    @Column
    private Integer successCount;

    @Column
    private Integer failureCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime completedAt;

    @Convert(converter = PointageBatchListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<PointageBatchDto> results;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (successCount == null) {
            successCount = 0;
        }
        if (failureCount == null) {
            failureCount = 0;
        }
    }
    
    public Long getDurationSeconds() {
        if (createdAt != null && completedAt != null) {
            return java.time.Duration.between(createdAt, completedAt).getSeconds();
        }
        return null;
    }
}