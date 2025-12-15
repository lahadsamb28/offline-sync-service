package com.seneau.offline_sync_service.web.dto;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointageBatchDto implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    private Long agent;
    private LocalDate date;
    private Boolean isTerrain;
    private String statut;
    private String message;
}