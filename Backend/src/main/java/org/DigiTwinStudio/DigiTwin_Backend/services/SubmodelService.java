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

import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;

import org.springframework.stereotype.Service;

/**
 * Provides submodel validation, creation, and lookup for AAS models.
 */
@Service
@RequiredArgsConstructor
public class SubmodelService {

    private final SubmodelMapper submodelMapper;
    private final TemplateRepository templateRepository;
    private final AASModelRepository aasModelRepository;
    private final JsonDeserializer jsonDeserializer = new JsonDeserializer();

    /**
     * Creates an empty submodel DTO from the specified template.
     *
     * @param templateId the template ID
     * @return a new submodel DTO based on the template
     * @throws NotFoundException if the template is not found
     * @throws ValidationException if the template JSON is invalid
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
     * Retrieves a submodel DTO from an AAS model.
     *
     * @param modelId    the model ID
     * @param submodelId the submodel ID or IdShort
     * @return the submodel DTO
     * @throws NotFoundException if the model or submodel is not found
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
