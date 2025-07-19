package org.DigiTwinStudio.DigiTwin_Backend.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ExportException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.UploadException;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.springframework.stereotype.Component;

@Component
public class AAS4jAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates an empty Asset Administration Shell (AAS) with the given idShort.
     *
     * @param idShort the short identifier for the AAS
     * @return a new instance of {@link DefaultAssetAdministrationShell}
     */
    public DefaultAssetAdministrationShell createEmptyAAS(String idShort) {
        return new DefaultAssetAdministrationShell.Builder()
                .idShort(idShort)
                .build();
    }

    /**
     * Parses a JSON JsonNode into a {@link DefaultSubmodel} object.
     *
     * @param json the JSON representation of the submodel
     * @return a {@link DefaultSubmodel} instance parsed from the JSON
     * @throws ExportException if the JSON parsing fails
     */
    public DefaultSubmodel parseSubmodelFromJson(JsonNode json) {
        try {
            return objectMapper.treeToValue(json, DefaultSubmodel.class);
        } catch (JsonProcessingException e) {
            throw new UploadException("Failed to parse Submodel from JSON", e);
        }
    }

    /**
     * Serializes a given AAS-related object to a pretty-printed JSON JsonNode.
     *
     * @param aasObject the AAS object to serialize (e.g., AAS, Submodel)
     * @return the JSON JsonNode representation of the object
     * @throws ExportException if serialization fails
     */
    public JsonNode serializeToJson(Object aasObject) {
        try {
            return objectMapper.valueToTree(aasObject);
        } catch (IllegalArgumentException e) {
            throw new ExportException("Failed to serialize AAS object to JSON", e);
        }
    }
}
