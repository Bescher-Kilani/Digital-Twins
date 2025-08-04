package org.DigiTwinStudio.DigiTwin_Backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;

import org.bson.Document;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * MongoDB read converter for mapping {@link org.bson.Document} to Jackson {@link JsonNode}.
 * <p>
 * Allows automatic conversion of BSON documents to JSON nodes when reading from the database.
 * </p>
 */
@Component
public class JsonNodeReadConverter implements Converter<Document, JsonNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts a BSON {@link Document} to a Jackson {@link JsonNode}.
     *
     * @param source the BSON document to convert
     * @return the corresponding JsonNode
     */
    @Override
    public JsonNode convert(@NonNull Document source) {
        return objectMapper.convertValue(source, JsonNode.class);
    }
}
