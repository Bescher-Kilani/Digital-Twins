package org.DigiTwinStudio.DigiTwin_Backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ValidationException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.SubmodelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TemplateRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.UploadedFileRepository;
import org.DigiTwinStudio.DigiTwin_Backend.validation.SubmodelValidator;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class SubmodelService {

    private final SubmodelValidator submodelValidator;
    private final UploadedFileRepository uploadedFileRepository;
    private final SubmodelMapper submodelMapper;
    private final TemplateRepository templateRepository;
    private final AASModelRepository aasModelRepository;
    private final JsonDeserializer jsonDeserializer = new JsonDeserializer();

    /**
     * Validate the given SubmodelDto against AAS4J structural rules.
     *
     * @param submodelDto the DTO representing the Submodel to validate
     * @throws ValidationException if validation fails
     */
    public void validate(SubmodelDto submodelDto) {
        DefaultSubmodel submodel = submodelMapper.fromDto(submodelDto);
        try {
            submodelValidator.validate(submodel);
        } catch (Exception e) {
            throw new BadRequestException("Submodel validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check whether a referenced file ID exists in the repository.
     *
     * @param fileId the ID of the uploaded file
     * @return true if the file exists, false otherwise
     */
    public boolean isFileReferenceValid(String fileId) {
        return uploadedFileRepository.findById(fileId).isPresent();
    }

    /**
     * Create an empty SubmodelDto based on the JSON template.
     *
     * returns only the Submodel without any template metadata.
     *
     * @param templateId the ID of the template to load
     * @return a SubmodelDto containing the deserialized Submodel
     * @throws NotFoundException   if the template cannot be found
     * @throws ValidationException if the JSON cannot be converted to a Submodel
     */
    public SubmodelDto createEmptySubmodelFromTemplate(String templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));

        try {
            JsonNode json = template.getJson();
            DefaultSubmodel submodel = jsonDeserializer.read(json, DefaultSubmodel.class);
            return SubmodelDto.builder()
                    .submodel(submodel)
                    .build();
        } catch (Exception e) {
            throw new BadRequestException("Failed to parse template JSON for ID " + templateId, e);
        }
    }

    /**
     * Retrieve an existing SubmodelDto from a given AASModel.
     *
     * @param modelId    the ID of the AASModel
     * @param submodelId the IdShort or Identifier of the Submodel
     * @return the corresponding SubmodelDto
     * @throws NotFoundException if the model or submodel cannot be found
     */
    public SubmodelDto getSubmodel(String modelId, String submodelId) {
        AASModel model = aasModelRepository.findById(modelId)
                .orElseThrow(() -> new NotFoundException("AASModel not found: " + modelId));

        return model.getSubmodels().stream()
                .filter(sm -> submodelId.equals(sm.getIdShort()))
                .findFirst()
                .map(submodelMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Submodel not found: " + submodelId));
    }
}
