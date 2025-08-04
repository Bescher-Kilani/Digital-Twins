package org.DigiTwinStudio.DigiTwin_Backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.SubmodelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TemplateRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.UploadedFileRepository;
import org.DigiTwinStudio.DigiTwin_Backend.validation.SubmodelValidator;

import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubmodelServiceTest {

    @InjectMocks
    private SubmodelService service;

    @Mock
    private SubmodelValidator submodelValidator;
    @Mock
    private UploadedFileRepository uploadedFileRepository;
    @Mock
    private SubmodelMapper submodelMapper;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private AASModelRepository aasModelRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // testing createEmptySubmodelFromTemplate function
    @Test
    void createEmptySubmodelFromTemplate_parsesTemplate_andReturnsDto() throws Exception {
        String templateId = "tpl-1";

        // Valid DefaultSubmodel JSON (AAS4J), idShort included
        JsonNode json = objectMapper.readTree("""
        { "modelType":"Submodel", "id":"sub-1", "idShort":"mySub", "submodelElements":[] }
        """);

        Template template = mock(Template.class);
        when(template.getJson()).thenReturn(json);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        SubmodelDto dto = service.createEmptySubmodelFromTemplate(templateId);

        assertNotNull(dto);
        assertNotNull(dto.getSubmodel());
        assertEquals("mySub", dto.getSubmodel().getIdShort());
    }

    @Test
    void createEmptySubmodelFromTemplate_throwsNotFound_whenTemplateMissing() {
        when(templateRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> service.createEmptySubmodelFromTemplate("missing"));
    }

    @Test
    void createEmptySubmodelFromTemplate_wrapsParseErrors_asBadRequest() throws Exception {
        String templateId = "bad";

        // malformed JSON to force parse failure
        JsonNode badJson = objectMapper.readTree("{\"notSubmodel\": true}");

        Template template = mock(Template.class);
        when(template.getJson()).thenReturn(badJson);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        assertThrows(BadRequestException.class,
                () -> service.createEmptySubmodelFromTemplate(templateId));

        verifyNoInteractions(submodelMapper);
    }

    // testing getSubmodel function
    @Test
    void getSubmodel_returnsDto_whenFoundByIdShort() {
        String modelId = "m1";
        String idShort = "s1";

        DefaultSubmodel sm = new DefaultSubmodel();
        sm.setIdShort(idShort);

        AASModel model = AASModel.builder()
                .id(modelId)
                .ownerId("u1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .published(false)
                .submodels(new ArrayList<>(List.of(sm)))
                .build();

        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(model));

        SubmodelDto mapped = SubmodelDto.builder().submodel(sm).build();
        when(submodelMapper.toDto(sm)).thenReturn(mapped);

        SubmodelDto result = service.getSubmodel(modelId, idShort);

        assertSame(mapped, result);
        verify(submodelMapper).toDto(sm);
    }

    @Test
    void getSubmodel_throwsNotFound_whenModelMissing() {
        when(aasModelRepository.findById("nope")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getSubmodel("nope", "s1"));
    }

    @Test
    void getSubmodel_throwsNotFound_whenSubmodelMissing() {
        String modelId = "m1";
        AASModel model = AASModel.builder()
                .id(modelId)
                .ownerId("u1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .published(false)
                .submodels(new ArrayList<>())
                .build();

        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(model));

        assertThrows(NotFoundException.class, () -> service.getSubmodel(modelId, "s-missing"));
        verify(submodelMapper, never()).toDto(any());
    }
}
