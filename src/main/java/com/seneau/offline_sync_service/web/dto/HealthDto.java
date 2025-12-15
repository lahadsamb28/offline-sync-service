package com.seneau.offline_sync_service.web.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthDto {
    private String status;
    private String message;
}