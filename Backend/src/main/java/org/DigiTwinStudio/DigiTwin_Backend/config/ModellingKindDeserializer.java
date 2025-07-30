package org.DigiTwinStudio.DigiTwin_Backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.ModellingKind;

import java.io.IOException;

public class ModellingKindDeserializer extends JsonDeserializer<ModellingKind> {

    @Override
    public ModellingKind deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();

        if (value == null) {
            return null;
        }

        return switch (value.trim().toUpperCase()) {
            case "TEMPLATE" -> ModellingKind.TEMPLATE;
            case "INSTANCE" -> ModellingKind.INSTANCE;
            default ->
                    throw new IOException("Unsupported ModellingKind: '" + value + "'. Expected 'TEMPLATE' or 'INSTANCE'.");
        };
    }
}
