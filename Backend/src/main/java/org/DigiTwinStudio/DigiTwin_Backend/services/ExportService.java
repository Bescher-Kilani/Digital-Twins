package org.DigiTwinStudio.DigiTwin_Backend.services;

import io.adminshell.aas.v3.dataformat.aasx.InMemoryFile;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.adapter.AAS4jAdapter;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {
    private final AAS4jAdapter aas4jAdapter;
    private final FileStorageService fileStorageService;

    public byte[] exportAsJson(AASModel model) {
        DefaultEnvironment environment = aasModeltoDefaultEnvironment(model);
        try {
            String jsonString = aas4jAdapter.serializeToJsonString(environment);
            return jsonString.getBytes(StandardCharsets.UTF_8);
        } catch (SerializationException e) {
            log.error("Failed to serialize AAS object to JSON", e);
            return new byte[0];
        }
    }
    public byte[] exportAsAASX(AASModel model) {
        DefaultEnvironment environment = aasModeltoDefaultEnvironment(model);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // get files
        List<InMemoryFile> inMemoryFiles = this.fileStorageService.getUploadedFilesFromModel(model);
        try {
            this.aas4jAdapter.serializeToAASX((AssetAdministrationShellEnvironment) environment, inMemoryFiles, baos);
        } catch (io.adminshell.aas.v3.dataformat.SerializationException | java.io.IOException e) {
            log.error("Failed to serialize AAS object to AASX", e);
        }
        return baos.toByteArray();
    }
    public byte[] exportStoredModel(String modelId, ExportFormat format) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public byte[] exportTransientModel(AASModelDto dto, ExportFormat format) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public byte[] exportMarketplaceModel(String entryId, ExportFormat format) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private DefaultEnvironment aasModeltoDefaultEnvironment(AASModel model) {
        // create Environment
        DefaultEnvironment environment = new DefaultEnvironment();
        // add AssetAdministrationShell
        environment.setAssetAdministrationShells(List.of(model.getAas()));

        // convert DefaultSubmodels to Submodels and add to Environment
        List<Submodel> submodels = model.getSubmodels().stream()
                .map(defaultSubmodel -> (Submodel) defaultSubmodel)
                .collect(Collectors.toList());
        environment.setSubmodels(submodels);

        return environment;
    }
}
