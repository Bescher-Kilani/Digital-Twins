package org.DigiTwinStudio.DigiTwin_Backend.services;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.adapter.AAS4jAdapter;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportedFile;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.domain.MarketplaceEntry;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ExportException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ForbiddenException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.MarketPlaceEntryRepository;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.aasx.InMemoryFile;
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
        DefaultEnvironment environment = aas4jAdapter.aasModelToDefaultEnvironment(model);

        // NEU: Korrekte Pfade mit InMemoryFile-Objekten
        List<InMemoryFile> inMemoryFiles = fileStorageService.getInMemoryFilesByModelId(model.getId());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] result;
        try {
            this.aas4jAdapter.serializeToAASX(environment, inMemoryFiles, baos);
            result = baos.toByteArray();
            log.info("AASX-Export erfolgreich – Dateigröße: {} Bytes", result.length);
        } catch (SerializationException e) {
            log.error("Fehler beim Export nach AASX", e);
            throw new ExportException("Failed to serialize AAS object to AASX");
        }

        return result;
    }


    /**
     * Export a stored model. Retrieves model from repository.
     * @param modelId of stored model to be exported
     * @param format JSON or AASX
     * @return contents of an exported file as byte[]
     */
    public byte[] exportStoredModel(String modelId, ExportFormat format, String userId) {
        AASModel model = this.aasModelRepository.findById(modelId).orElseThrow(() -> new NotFoundException("Could not find model with given Id"));

        if (!model.getOwnerId().equals(userId)) {
            throw new ForbiddenException("Access denied: model does not belong to user.");
        }

        return switch (format) {
            case JSON -> exportAsJson(model);
            case AASX -> exportAsAasx(model);
            default -> throw new ExportException("Unsupported format");
        };
    }

    /**
     * Export a model that has been edited by a Guest and therefore is not saved in the repository.
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

    public ExportedFile export(String id, String name, ExportFormat format, String userId) {
        byte[] content = exportStoredModel(id, format, userId);

        String fileExtension = switch (format) {
            case JSON -> "json";
            case AASX -> "aasx";
            default -> throw new ExportException("Unsupported format");
        };

        String contentType = switch (format) {
            case JSON -> "application/json";
            case AASX -> "application/asset-administration-shell-package";
        };

        String filename = name + "." + fileExtension;
        return new ExportedFile(content, filename, contentType);
    }



}
