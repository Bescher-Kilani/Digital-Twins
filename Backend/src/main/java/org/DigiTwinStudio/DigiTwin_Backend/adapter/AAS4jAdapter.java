package org.DigiTwinStudio.DigiTwin_Backend.adapter;

import com.fasterxml.jackson.databind.JsonNode;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ExportException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.aasx.AASXSerializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.aasx.InMemoryFile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AAS4jAdapter {

    private final JsonDeserializer jsonDeserializer = new JsonDeserializer();
    private final JsonSerializer jsonSerializer = new JsonSerializer();
    private final AASXSerializer aasxSerializer = new AASXSerializer();

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
     * defaultsubmodel, node json
     */
    public DefaultSubmodel parseSubmodelFromJson(JsonNode json) {
        try {
            return jsonDeserializer.read(json,DefaultSubmodel.class);
        } catch (DeserializationException e) {
            throw new RuntimeException(e);
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
            return jsonSerializer.toNode(aasObject);

        } catch (IllegalArgumentException e) {
            throw new ExportException("Failed to serialize AAS object to JSON", e);
        }
    }

    public String serializeToJsonString(Object aasObject) throws SerializationException {
        return this.jsonSerializer.write(aasObject);
    }

    public void serializeToAASX(Environment environment, Collection<InMemoryFile> files, OutputStream outputStream) throws SerializationException {
        try {
            this.aasxSerializer.write(environment, files, outputStream);
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize environment to AASX", e);
        }
    }

    /**
     * create a new DefaultEnvironment and embed the given AASModels DefaultAAS with its Submodels
     * @param model AASModel with DefaultAAS to be embedded
     * @return DefaultEnvironment containing given AASModels DefaultAAS
     */
    public DefaultEnvironment aasModelToDefaultEnvironment(AASModel model) {
        // create Environment
        DefaultEnvironment environment = new DefaultEnvironment();
        // add AssetAdministrationShell
        environment.setAssetAdministrationShells(List.of(model.getAas()));

        if (model.getSubmodels() != null && !model.getSubmodels().isEmpty()) {
            // convert DefaultSubmodels to Submodels and add to Environment
            List<Submodel> submodels = model.getSubmodels().stream()
                    .map(defaultSubmodel -> (Submodel) defaultSubmodel)
                    .collect(Collectors.toList());
            environment.setSubmodels(submodels);
        }

        return environment;
    }
}
