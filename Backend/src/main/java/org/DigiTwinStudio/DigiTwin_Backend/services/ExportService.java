package org.DigiTwinStudio.DigiTwin_Backend.services;

import io.adminshell.aas.v3.dataformat.aasx.InMemoryFile;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.adapter.AAS4jAdapter;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.domain.MarketplaceEntry;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ExportException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.MarketPlaceEntryRepository;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {
    private final AAS4jAdapter aas4jAdapter;
    private final FileStorageService fileStorageService;
    private final AASModelRepository aasModelRepository;
    private final AASModelMapper aasModelMapper;
    private final MarketPlaceEntryRepository  marketPlaceEntryRepository;

    /**
     * Exports given model to a JSON file
     * @param model to be parsed to JSON
     * @return JSON-format of the model as byte[]
     * @throws ExportException if serialization failed
     */
    public byte[] exportAsJson(AASModel model) {
        DefaultEnvironment environment = aas4jAdapter.aasModelToDefaultEnvironment(model);
        try {
            String jsonString = aas4jAdapter.serializeToJsonString(environment);
            return jsonString.getBytes(StandardCharsets.UTF_8);
        } catch (SerializationException e) {
            log.error("Failed to serialize AAS object to JSON", e);
            throw new ExportException("Failed to serialize AAS object to JSON");
        }
    }

    /**
     * Exports given model to an AASX file
     * @param model to be parsed to AASX
     * @return AASX-format of the model as byte[]
     * @throws ExportException if serialization failed
     */
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

    /**
     * Export a stored model. Retrieves model from repository.
     * @param modelId of stored model to be exported
     * @param format JSON or AASX
     * @return contents of an exported file as byte[]
     */
    public byte[] exportStoredModel(String modelId, ExportFormat format) {
        AASModel model = this.aasModelRepository.findById(modelId).orElseThrow(() -> new NotFoundException("Could not find model with given Id"));
        return switch (format) {
            case JSON -> exportAsJson(model);
            case AASX -> exportAsAasx(model);
            default -> throw new ExportException("Unsupported format");
        };
    }

    /**
     * Export a model that has been edited by a Guest and therefore isnt saved in the repository.
     * @param dto temporary modelDTO
     * @param format JSON or AASX
     * @return content of an exported file as byte[]
     */
    public byte[] exportTransientModel(AASModelDto dto, ExportFormat format) {
        // ToDo: Maybe use different owner Id than "GUEST"
        AASModel model = this.aasModelMapper.fromDto(dto, "GUEST");
        return switch (format) {
            case JSON -> exportAsJson(model);
            case AASX -> exportAsAasx(model);
            default -> throw new ExportException("Unsupported format");
        };
    }

    /**
     * Export a model referenced by a marketplace-entry
     * @param entryId marketplace-entry-id
     * @param format JSON or AASX
     * @return content of an exported file as byte[]
     */
    public byte[] exportMarketplaceModel(String entryId, ExportFormat format) {
        MarketplaceEntry marketplaceEntry = this.marketPlaceEntryRepository.findById(entryId).orElseThrow(() -> new NotFoundException("Could not find entry with given Id"));
        AASModel model = this.aasModelRepository.findById(marketplaceEntry.getId()).orElseThrow(()  -> new NotFoundException("Could not find model with given Id"));
        return switch (format) {
            case JSON -> exportAsJson(model);
            case AASX -> exportAsAasx(model);
            default -> throw new ExportException("Unsupported format");
        };

    }
}
