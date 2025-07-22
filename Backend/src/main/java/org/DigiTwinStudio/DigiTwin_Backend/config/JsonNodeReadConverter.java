package org.DigiTwinStudio.DigiTwin_Backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class JsonNodeReadConverter implements Converter<Document, JsonNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public JsonNode convert(Document source) {
        return objectMapper.convertValue(source, JsonNode.class);
    }
}
