package org.DigiTwinStudio.DigiTwin_Backend.adapter;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.aasx.AASXSerializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.aasx.InMemoryFile;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter for working with AAS4j v3 Asset Administration Shell objects and serialization.
 */
@Component
public class AAS4jAdapter {

    private final JsonSerializer jsonSerializer = new JsonSerializer();
    private final AASXSerializer aasxSerializer = new AASXSerializer();

    /**
     * Serializes an AAS object to a JSON string.
     *
     * @param aasObject the object to serialize
     * @return the JSON string
     * @throws SerializationException if serialization fails
     */
    public String serializeToJsonString(Object aasObject) throws SerializationException {
        return this.jsonSerializer.write(aasObject);
    }

    /**
     * Serializes an {@link Environment} and associated files to the AASX format.
     *
     * @param environment the AAS environment
     * @param files       files to include in the package
     * @param outputStream the output stream to write to
     * @throws SerializationException if serialization fails
     */
    public void serializeToAASX(Environment environment, Collection<InMemoryFile> files, OutputStream outputStream) throws SerializationException {
        try {
            this.aasxSerializer.write(environment, files, outputStream);
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize environment to AASX", e);
        }
    }

    /**
     * Create a new DefaultEnvironment and embed the given AASModels DefaultAAS with its Submodels
     *
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
