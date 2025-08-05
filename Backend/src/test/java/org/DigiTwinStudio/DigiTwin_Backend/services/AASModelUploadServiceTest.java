package org.DigiTwinStudio.DigiTwin_Backend.services;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.UploadException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ValidationException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.validation.AASModelValidator;
import org.DigiTwinStudio.DigiTwin_Backend.validation.FileUploadValidator;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AASModelUploadServiceTest {

    @InjectMocks
    private AASModelUploadService service;

    @Mock
    private AASModelValidator aasModelValidator;
    @Mock
    private AASModelMapper aasModelMapper;
    @Mock
    private FileUploadValidator fileUploadValidator;

    @BeforeEach
    public void setUp() {
        AASModelUploadService spyService = Mockito.spy(new AASModelUploadService(
                aasModelValidator,
                aasModelMapper,
                fileUploadValidator
        ));
    }

    // testing uploadAASModel function
    @Test
    void uploadAASModel_success() throws Exception {
        String ownerId = "user-123";
        AASModelDto expectedDto = new AASModelDto();
        DefaultEnvironment environment = createValidEnvironment();

        String json = """
            {
                "assetAdministrationShells": [{"idShort": "exampleShell"}],
                "submodels": [{"idShort": "sub1"}]
            }
        """;

        MultipartFile file = new MockMultipartFile("file", "aas.json", "application/json", json.getBytes());

        AASModelUploadService spyService = Mockito.spy(service);
        doReturn(environment).when(spyService).parseEnvironment(any());

        when(aasModelMapper.toDto(any(AASModel.class))).thenReturn(expectedDto);

        AASModelDto result = spyService.uploadAASModel(file, ownerId);

        assertNotNull(result);
        assertEquals(expectedDto, result);
        verify(fileUploadValidator).validate(file);
        verify(aasModelValidator).validate(any(AASModel.class));
    }

    @Test
    void uploadAASModel_noShells_shouldThrowUploadException() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "aas.json", "application/json", "{}".getBytes());

        DefaultEnvironment env = new DefaultEnvironment();
        env.setAssetAdministrationShells(List.of()); // No shells
        env.setSubmodels(List.of());

        AASModelUploadService spyService = Mockito.spy(service);
        doReturn(env).when(spyService).parseEnvironment(any());

        assertThrows(UploadException.class, () -> spyService.uploadAASModel(file, "owner"));
    }

    @Test
    void uploadAASModel_parseThrowsIOException_shouldThrowUploadException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("stream error"));

        assertThrows(UploadException.class, () -> service.uploadAASModel(file, "owner"));
    }

    @Test
    void uploadAASModel_validationFails_shouldThrowBadRequest() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "aas.json", "application/json", "{}".getBytes());

        DefaultEnvironment env = createValidEnvironment();

        AASModelUploadService spyService = Mockito.spy(service);
        doReturn(env).when(spyService).parseEnvironment(any());

        doThrow(new ValidationException("validation failed") {
            @Override
            public HttpStatus getHttpStatus() {
                return HttpStatus.BAD_REQUEST;
            }
        }).when(aasModelValidator).validate(any());

        assertThrows(BadRequestException.class, () -> spyService.uploadAASModel(file, "owner"));
        verify(aasModelValidator).validate(any());
    }

    private DefaultEnvironment createValidEnvironment() {
        DefaultAssetAdministrationShell shell = new DefaultAssetAdministrationShell();
        shell.setIdShort("exampleShell");

        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setIdShort("submodel1");

        DefaultEnvironment environment = new DefaultEnvironment();
        environment.setAssetAdministrationShells(List.of(shell));
        environment.setSubmodels(List.of(submodel));

        return environment;
    }
}
