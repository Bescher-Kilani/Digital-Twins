package org.DigiTwinStudio.DigiTwin_Backend.config;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.*;

import java.io.IOException;
public class SubmodelElementDeserializer extends JsonDeserializer<SubmodelElement> {

    @Override
    public SubmodelElement deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String modelType = node.get("modelType").asText();

        ObjectMapper mapper = (ObjectMapper) p.getCodec();

        return switch (modelType) {
            case "Property" -> mapper.treeToValue(node, DefaultProperty.class);
            case "MultiLanguageProperty" -> mapper.treeToValue(node, DefaultMultiLanguageProperty.class);
            case "File" -> mapper.treeToValue(node, DefaultFile.class);
            case "SubmodelElementList" -> mapper.treeToValue(node, DefaultSubmodelElementList.class);
            case "SubmodelElementCollection" -> mapper.treeToValue(node, DefaultSubmodelElementCollection.class);
            default -> throw new IOException("Unknown modelType: " + modelType);
        };
    }



}


