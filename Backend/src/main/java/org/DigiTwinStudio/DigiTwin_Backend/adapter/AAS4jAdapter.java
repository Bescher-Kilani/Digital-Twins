package org.DigiTwinStudio.DigiTwin_Backend.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ExportException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.UploadException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
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
     * @return a new instance of {@link AssetAdministrationShell}
     */
    public AssetAdministrationShell createEmptyAAS(String idShort) {
        return new DefaultAssetAdministrationShell.Builder()
                .idShort(idShort)
                .build();
    }

    /**
     * Parses a JSON string into a {@link Submodel} object.
     *
     * @param json the JSON representation of the submodel
     * @return a {@link Submodel} instance parsed from the JSON
     * @throws ExportException if the JSON parsing fails
     */
    public Submodel parseSubmodelFromJson(String json) {
        try {
            return objectMapper.readValue(json, DefaultSubmodel.class);
        } catch (JsonProcessingException e) {
            throw new UploadException("Failed to parse Submodel from JSON", e);
        }
    }

    /**
     * Serializes a given AAS-related object to a pretty-printed JSON string.
     *
     * @param aasObject the AAS object to serialize (e.g., AAS, Submodel)
     * @return the JSON string representation of the object
     * @throws ExportException if serialization fails
     */
    public String serializeToJson(Object aasObject) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(aasObject);
        } catch (JsonProcessingException e) {
            throw new ExportException("Failed to serialize AAS object to JSON", e);
        }
    }
}
