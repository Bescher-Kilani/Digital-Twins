package org.DigiTwinStudio.DigiTwin_Backend.services;

import io.adminshell.aas.v3.dataformat.aasx.InMemoryFile;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.adapter.AAS4jAdapter;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ExportException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {
    private final AAS4jAdapter aas4jAdapter;
    private final FileStorageService fileStorageService;

    public byte[] exportAsJson(AASModel model) {
        DefaultEnvironment environment = aas4jAdapter.aasModelToDefaultEnvironment(model);
        try {
            String jsonString = aas4jAdapter.serializeToJsonString(environment);
            return jsonString.getBytes(StandardCharsets.UTF_8);
        } catch (SerializationException e) {
            log.error("Failed to serialize AAS object to JSON", e);
            return new byte[0];
        }
    }

    public byte[] exportAsAasx(AASModel model) throws ExportException {
        // create aas environment
        DefaultEnvironment environment = aas4jAdapter.aasModelToDefaultEnvironment(model);
        // get file contents and convert to InMemoryFile-objects where the path is empty
        List<InMemoryFile> inMemoryFiles = this.fileStorageService.getFileContentsByModelId(model.getId()).stream()
                .map(content -> new InMemoryFile(content, ""))
                .toList();

        // call aasx serializer
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            this.aas4jAdapter.serializeToAASX((AssetAdministrationShellEnvironment) environment, inMemoryFiles, baos);
        } catch (SerializationException e) {
            log.error("Failed to serialize AAS object to AASX", e);
            throw new ExportException("Failed to serialize AAS object to AASX");
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

}
