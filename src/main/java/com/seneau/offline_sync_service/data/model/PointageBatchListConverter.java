package com.seneau.offline_sync_service.data.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seneau.offline_sync_service.web.dto.PointageBatchDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Converter
@Slf4j
public class PointageBatchListConverter implements AttributeConverter<List<PointageBatchDto>, String> {

    private final ObjectMapper objectMapper;

    public PointageBatchListConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(List<PointageBatchDto> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la conversion en JSON", e);
            return "[]";
        }
    }

    @Override
    public List<PointageBatchDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<PointageBatchDto>>() {});
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la conversion depuis JSON", e);
            return new ArrayList<>();
        }
    }
}