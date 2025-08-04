package org.DigiTwinStudio.DigiTwin_Backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;

import org.bson.Document;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * MongoDB write converter for mapping Jackson {@link JsonNode} to {@link org.bson.Document}.
 * <p>
 * Enables automatic conversion of JSON nodes to BSON documents when writing to the database.
 * </p>
 */
@Component
public class JsonNodeWriteConverter implements Converter<JsonNode, Document> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts a Jackson {@link JsonNode} to a BSON {@link Document}.
     *
     * @param source the JsonNode to convert
     * @return the corresponding BSON Document
     */
    @Override
    public Document convert(@NonNull JsonNode source) {
        return objectMapper.convertValue(source, Document.class);
    }
}
