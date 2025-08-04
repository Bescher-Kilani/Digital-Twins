package org.DigiTwinStudio.DigiTwin_Backend.config;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.*;

import java.io.IOException;

/**
 * Jackson deserializer for polymorphic {@link SubmodelElement} objects.
 * <p>
 * Chooses the correct AAS4j implementation based on the {@code modelType} property in the JSON.
 * Throws {@link IOException} if the type is unknown.
 * </p>
 */
public class SubmodelElementDeserializer extends JsonDeserializer<SubmodelElement> {

    /**
     * Deserializes a JSON object into the appropriate {@link SubmodelElement} implementation,
     * based on the {@code modelType} property in the JSON.
     *
     * @param p     the JSON parser
     * @param ctxt  the deserialization context
     * @return the deserialized {@link SubmodelElement} instance
     * @throws IOException if the {@code modelType} is missing or unrecognized
     */
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
