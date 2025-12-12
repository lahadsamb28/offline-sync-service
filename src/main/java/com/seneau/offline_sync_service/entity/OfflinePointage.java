package com.seneau.offline_sync_service.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seneau.offline_sync_service.dto.PointageBatchRequestDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "offline_pointage")
@SequenceGenerator(
        name = "batch_pointage_seq",
        sequenceName = "batch_pointage_seq",
        allocationSize = 10
)
@Builder
public class OfflinePointage {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "batch_pointage_seq")
    private Long id;


    @Lob
    private String payloadJson;
    private LocalDateTime dateReception;

    @Enumerated(EnumType.STRING)
    private EStatut statut;

    private String errorMessage;
    private LocalDateTime publishedAt;

    public static OfflinePointage fromDto(PointageBatchRequestDto dto){
        try{
            return OfflinePointage.builder()
                    .payloadJson(new ObjectMapper().writeValueAsString(dto))
                    .statut(EStatut.PENDING)
                    .dateReception(LocalDateTime.now())
                    .build();
        }catch (Exception e){
            throw new RuntimeException("Erreur conversion DTO -> JSON", e);
        }
    }
    public PointageBatchRequestDto toDto() {
        return PointageBatchRequestDto.fromJson(payloadJson);
    }
}
