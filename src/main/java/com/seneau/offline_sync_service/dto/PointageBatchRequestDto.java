package com.seneau.offline_sync_service.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.List;

@Data
public class PointageBatchRequestDto {
    private List<PointageOfflineDto> pointages;

    private static final ObjectMapper mapper = new ObjectMapper();

    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur conversion JSON", e);
        }
    }

    public static PointageBatchRequestDto fromJson(String json) {
        try {
            return mapper.readValue(json, PointageBatchRequestDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Erreur parsing JSON payload", e);
        }
    }
}
