package org.DigiTwinStudio.DigiTwin_Backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.TemplateDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.integration.SMTRepoClient;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.TemplateMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TemplateRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock private TemplateRepository templateRepository;
    @Mock private SMTRepoClient smtRepoClient;
    @Mock private TemplateMapper templateMapper;

    @InjectMocks
    private TemplateService templateService;

    private Template template1;
    private Template template2;
    private TemplateDto templateDto1;
    private TemplateDto templateDto2;
    private final String templateId = "test-template-id";
    private final String templateName = "TestTemplate";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() throws Exception {
        // Create sample JSON content
        JsonNode jsonContent = objectMapper.readTree("{\"test\": \"content\"}");

        template1 = Template.builder()
                .id(templateId)
                .name(templateName)
                .descriptions(Map.of("en", "Test description", "de", "Test Beschreibung"))
                .version("1")
                .revision("0")
                .json(jsonContent)
                .pulledAt(LocalDateTime.now())
                .build();

        template2 = Template.builder()
                .id("template-2")
                .name("Template2")
                .descriptions(Map.of("en", "Second template"))
                .version("2")
                .revision("1")
                .json(jsonContent)
                .pulledAt(LocalDateTime.now())
                .build();

        templateDto1 = new TemplateDto();
        templateDto2 = new TemplateDto();
    }

    // --- getAvailableTemplates() ---
    @Test
    void getAvailableTemplates_shouldReturnAllTemplatesAsDtos() {
        // --- Arrange ---
        when(templateRepository.findAll()).thenReturn(List.of(template1, template2));
        when(templateMapper.toDto(template1)).thenReturn(templateDto1);
        when(templateMapper.toDto(template2)).thenReturn(templateDto2);

        // --- Act ---
        List<TemplateDto> result = templateService.getAvailableTemplates();

        // --- Assert ---
        assertEquals(2, result.size());
        assertTrue(result.contains(templateDto1));
        assertTrue(result.contains(templateDto2));

        verify(templateRepository).findAll();
        verify(templateMapper).toDto(template1);
        verify(templateMapper).toDto(template2);
    }

    @Test
    void getAvailableTemplates_shouldReturnEmptyListWhenNoTemplates() {
        // --- Arrange ---
        when(templateRepository.findAll()).thenReturn(Collections.emptyList());

        // --- Act ---
        List<TemplateDto> result = templateService.getAvailableTemplates();

        // --- Assert ---
        assertTrue(result.isEmpty());
        verify(templateRepository).findAll();
        verifyNoInteractions(templateMapper);
    }

    // --- syncTemplatesFromRepo() ---
    @Test
    void syncTemplatesFromRepo_shouldSaveNewTemplate() {
        // --- Arrange ---
        when(smtRepoClient.fetchTemplates()).thenReturn(List.of(template1));
        when(templateRepository.findByName(templateName)).thenReturn(Optional.empty());
        when(templateRepository.count()).thenReturn(1L);

        // --- Act ---
        templateService.syncTemplatesFromRepo();

        // --- Assert ---
        verify(smtRepoClient).fetchTemplates();
        verify(templateRepository).findByName(templateName);
        verify(templateRepository).save(template1);
        verify(templateRepository).count();
    }

    @Test
    void syncTemplatesFromRepo_shouldUpdateTemplateWithNewerVersion() {
        // --- Arrange ---
        Template oldTemplate = Template.builder()
                .id("old-id")
                .name(templateName)
                .version("1")
                .revision("0")
                .build();

        Template newTemplate = Template.builder()
                .id("new-id")
                .name(templateName)
                .version("2")  // Higher version
                .revision("0")
                .build();

        when(smtRepoClient.fetchTemplates()).thenReturn(List.of(newTemplate));
        when(templateRepository.findByName(templateName)).thenReturn(Optional.of(oldTemplate));
        when(templateRepository.count()).thenReturn(1L);

        // --- Act ---
        templateService.syncTemplatesFromRepo();

        // --- Assert ---
        verify(templateRepository).delete(oldTemplate);
        verify(templateRepository).save(newTemplate);
    }

    @Test
    void syncTemplatesFromRepo_shouldUpdateTemplateWithNewerRevision() {
        // --- Arrange ---
        Template oldTemplate = Template.builder()
                .id("old-id")
                .name(templateName)
                .version("1")
                .revision("0")
                .build();

        Template newTemplate = Template.builder()
                .id("new-id")
                .name(templateName)
                .version("1")  // Same version
                .revision("1") // Higher revision
                .build();

        when(smtRepoClient.fetchTemplates()).thenReturn(List.of(newTemplate));
        when(templateRepository.findByName(templateName)).thenReturn(Optional.of(oldTemplate));
        when(templateRepository.count()).thenReturn(1L);

        // --- Act ---
        templateService.syncTemplatesFromRepo();

        // --- Assert ---
        verify(templateRepository).delete(oldTemplate);
        verify(templateRepository).save(newTemplate);
    }

    @Test
    void syncTemplatesFromRepo_shouldSkipWhenLocalTemplateIsNewer() {
        // --- Arrange ---
        Template localTemplate = Template.builder()
                .id("local-id")
                .name(templateName)
                .version("2")
                .revision("0")
                .build();

        Template fetchedTemplate = Template.builder()
                .id("fetched-id")
                .name(templateName)
                .version("1")  // Lower version
                .revision("0")
                .build();

        when(smtRepoClient.fetchTemplates()).thenReturn(List.of(fetchedTemplate));
        when(templateRepository.findByName(templateName)).thenReturn(Optional.of(localTemplate));
        when(templateRepository.count()).thenReturn(1L);

        // --- Act ---
        templateService.syncTemplatesFromRepo();

        // --- Assert ---
        verify(templateRepository, never()).delete(any());
        verify(templateRepository, never()).save(fetchedTemplate);
        verify(templateRepository).count();
    }

    @Test
    void syncTemplatesFromRepo_shouldSkipWhenSameVersionAndRevision() {
        // --- Arrange ---
        Template localTemplate = Template.builder()
                .id("local-id")
                .name(templateName)
                .version("1")
                .revision("0")
                .build();

        Template fetchedTemplate = Template.builder()
                .id("fetched-id")
                .name(templateName)
                .version("1")  // Same version
                .revision("0") // Same revision
                .build();

        when(smtRepoClient.fetchTemplates()).thenReturn(List.of(fetchedTemplate));
        when(templateRepository.findByName(templateName)).thenReturn(Optional.of(localTemplate));
        when(templateRepository.count()).thenReturn(1L);

        // --- Act ---
        templateService.syncTemplatesFromRepo();

        // --- Assert ---
        verify(templateRepository, never()).delete(any());
        verify(templateRepository, never()).save(fetchedTemplate);
    }

    @Test
    void syncTemplatesFromRepo_shouldHandleEmptyFetchedTemplates() {
        // --- Arrange ---
        when(smtRepoClient.fetchTemplates()).thenReturn(Collections.emptyList());

        // --- Act ---
        templateService.syncTemplatesFromRepo();

        // --- Assert ---
        verify(smtRepoClient).fetchTemplates();
        verify(templateRepository, never()).save(any());
        verify(templateRepository, never()).findByName(any());
        verify(templateRepository, never()).delete(any());
        verify(templateRepository, never()).count();
    }

    @Test
    void syncTemplatesFromRepo_shouldHandleMultipleTemplates() {
        // --- Arrange ---
        Template newTemplate1 = Template.builder().name("New1").version("1").revision("0").build();
        Template newTemplate2 = Template.builder().name("New2").version("1").revision("0").build();

        when(smtRepoClient.fetchTemplates()).thenReturn(List.of(newTemplate1, newTemplate2));
        when(templateRepository.findByName("New1")).thenReturn(Optional.empty());
        when(templateRepository.findByName("New2")).thenReturn(Optional.empty());
        when(templateRepository.count()).thenReturn(2L);

        // --- Act ---
        templateService.syncTemplatesFromRepo();

        // --- Assert ---
        verify(templateRepository).save(newTemplate1);
        verify(templateRepository).save(newTemplate2);
    }

    // --- getTemplateById() ---
    @Test
    void getTemplateById_shouldReturnTemplateDto() {
        // --- Arrange ---
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template1));
        when(templateMapper.toDto(template1)).thenReturn(templateDto1);

        // --- Act ---
        TemplateDto result = templateService.getTemplateById(templateId);

        // --- Assert ---
        assertEquals(templateDto1, result);
        verify(templateRepository).findById(templateId);
        verify(templateMapper).toDto(template1);
    }

    @Test
    void getTemplateById_shouldThrowNotFoundExceptionWhenTemplateNotExists() {
        // --- Arrange ---
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // --- Act & Assert ---
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                templateService.getTemplateById(templateId));

        assertTrue(exception.getMessage().contains("Template not found: " + templateId));
        verify(templateRepository).findById(templateId);
        verifyNoInteractions(templateMapper);
    }

    // --- resolveTemplate() ---
    @Test
    void resolveTemplate_shouldReturnTemplate() {
        // --- Arrange ---
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template1));

        // --- Act ---
        Template result = templateService.resolveTemplate(templateId);

        // --- Assert ---
        assertEquals(template1, result);
        verify(templateRepository).findById(templateId);
    }

    @Test
    void resolveTemplate_shouldThrowNotFoundExceptionWhenTemplateNotExists() {
        // --- Arrange ---
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // --- Act & Assert ---
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                templateService.resolveTemplate(templateId));

        assertTrue(exception.getMessage().contains("Template not found: " + templateId));
        verify(templateRepository).findById(templateId);
    }

    // --- Edge Cases and Error Handling ---
    @Test
    void syncTemplatesFromRepo_shouldHandleInvalidVersionNumbers() {
        // --- Arrange ---
        Template localTemplate = Template.builder()
                .name(templateName)
                .version("invalid")
                .revision("0")
                .build();

        Template fetchedTemplate = Template.builder()
                .name(templateName)
                .version("1")
                .revision("0")
                .build();

        when(smtRepoClient.fetchTemplates()).thenReturn(List.of(fetchedTemplate));
        when(templateRepository.findByName(templateName)).thenReturn(Optional.of(localTemplate));

        // --- Act & Assert ---
        assertThrows(NumberFormatException.class, () ->
                templateService.syncTemplatesFromRepo());
    }

    @Test
    void syncTemplatesFromRepo_shouldHandleInvalidRevisionNumbers() {
        // --- Arrange ---
        Template localTemplate = Template.builder()
                .name(templateName)
                .version("1")
                .revision("invalid")
                .build();

        Template fetchedTemplate = Template.builder()
                .name(templateName)
                .version("1")
                .revision("0")
                .build();

        when(smtRepoClient.fetchTemplates()).thenReturn(List.of(fetchedTemplate));
        when(templateRepository.findByName(templateName)).thenReturn(Optional.of(localTemplate));

        // --- Act & Assert ---
        assertThrows(NumberFormatException.class, () ->
                templateService.syncTemplatesFromRepo());
    }
}