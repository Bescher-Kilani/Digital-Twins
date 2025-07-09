package org.DigiTwinStudio.DigiTwin_Backend.adapter;

import io.adminshell.aas.v3.dataformat.SerializationException;
import io.adminshell.aas.v3.dataformat.aasx.AASXSerializer;
import io.adminshell.aas.v3.model.AssetAdministrationShell;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;
import io.adminshell.aas.v3.model.impl.DefaultAssetAdministrationShellEnvironment;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ExportException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;

@Component
public class FaaastAdapter {

    /**
     * Converts the given Asset Administration Shell to AASX format as a byte array.
     *
     * @param aas the AAS model
     * @return byte array of the AASX file
     */
    public byte[] convertToAasx(AssetAdministrationShell aas) {
        AssetAdministrationShellEnvironment env = configureEnvironment(aas);
        return serializeToAasx(env);
    }

    private byte[] serializeToAasx(AssetAdministrationShellEnvironment env) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            AASXSerializer serializer = new AASXSerializer();
            serializer.write(env, Collections.emptyList(), outputStream);
            return outputStream.toByteArray();
        } catch (IOException | SerializationException e) {
            throw new ExportException("Failed to generate AASX file", e);
        }
    }

    private AssetAdministrationShellEnvironment configureEnvironment(AssetAdministrationShell aas) {
        DefaultAssetAdministrationShellEnvironment env = new DefaultAssetAdministrationShellEnvironment();
        env.getAssetAdministrationShells().add(aas);
        return env;
    }
}
