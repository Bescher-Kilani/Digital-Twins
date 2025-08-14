package org.DigiTwinStudio.DigiTwin_Backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportedFile;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.TemplateDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.UploadResponseDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Load only GuestController and related web components for testing
@WebMvcTest(GuestController.class)
// Import our custom test security configuration (no authentication required)
@Import(GuestControllerTest.TestSecurityConfig.class)
class GuestControllerTest {

    // Minimal test security configuration – disables CSRF and allows all requests
    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable) // disable CSRF for tests
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // allow all endpoints
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc; // MockMvc for sending HTTP requests in tests

    @Autowired
    private ObjectMapper objectMapper; // JSON serializer/deserializer

    // Mocked dependencies – no real service layer calls during tests
    @MockitoBean
    private SubmodelService submodelService;
    @MockitoBean
    private TemplateService templateService;
    @MockitoBean
    private AASModelService aasModelService;
    @MockitoBean
    private ExportService exportService;
    @MockitoBean
    private PropertyFileUploadService propertyFileUploadService;

    @Test
    void listAvailableSubmodelTemplates_ReturnsList() throws Exception {
        // Prepare mock data
        var t1 = TemplateDto.builder().id("t1").name("Temp 1").descriptions(Map.of("en","desc")).version("1").revision("0").build();
        var t2 = TemplateDto.builder().id("t2").name("Temp 2").version("1").revision("1").build();
        when(templateService.getAvailableTemplates()).thenReturn(List.of(t1, t2));

        // Perform GET request and validate JSON response
        mockMvc.perform(get("/guest/submodels/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("t1"))
                .andExpect(jsonPath("$[1].id").value("t2"));

        // Verify service method was called
        verify(templateService).getAvailableTemplates();
    }

    @Test
    void getNewSubmodel_ReturnsDto() throws Exception {
        var dto = SubmodelDto.builder().build();
        when(submodelService.createEmptySubmodelFromTemplate("tpl-1")).thenReturn(dto);

        mockMvc.perform(get("/guest/submodels/new").param("templateId","tpl-1"))
                .andExpect(status().isOk());

        verify(submodelService).createEmptySubmodelFromTemplate("tpl-1");
    }

    @Test
    void getNewSubmodel_NotFound_Returns404() throws Exception {
        when(submodelService.createEmptySubmodelFromTemplate("bad")).thenThrow(new NotFoundException("x"));

        mockMvc.perform(get("/guest/submodels/new").param("templateId","bad"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addSubmodelToModel_ReturnsUpdatedModel() throws Exception {
        var updated = AASModelDto.builder().id("m1").build();
        when(aasModelService.attachSubmodel(eq("m1"), any(SubmodelDto.class), eq("GUEST"))).thenReturn(updated);

        mockMvc.perform(post("/guest/models/{modelId}/submodels","m1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(SubmodelDto.builder().build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("m1"));

        verify(aasModelService).attachSubmodel(eq("m1"), any(SubmodelDto.class), eq("GUEST"));
    }

    @Test
    void getSubmodel_ReturnsDto() throws Exception {
        when(submodelService.getSubmodel("m1","s1")).thenReturn(SubmodelDto.builder().build());

        mockMvc.perform(get("/guest/models/{mid}/submodels/{sid}","m1","s1"))
                .andExpect(status().isOk());

        verify(submodelService).getSubmodel("m1","s1");
    }

    @Test
    void updateSubmodel_ReturnsUpdatedModel() throws Exception {
        var updated = AASModelDto.builder().id("m1").build();
        when(aasModelService.updateSubmodel(eq("m1"), eq("s1"), any(SubmodelDto.class), eq("GUEST"))).thenReturn(updated);

        mockMvc.perform(put("/guest/models/{mid}/submodels/{sid}","m1","s1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(SubmodelDto.builder().build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("m1"));

        verify(aasModelService).updateSubmodel(eq("m1"), eq("s1"), any(SubmodelDto.class), eq("GUEST"));
    }

    @Test
    void removeSubmodelFromModel_Returns204() throws Exception {
        mockMvc.perform(delete("/guest/models/{mid}/submodels/{sid}","m1","s1"))
                .andExpect(status().isNoContent());
        verify(aasModelService).removeSubmodel("m1","s1","GUEST");
    }

    @Test
    void createModelAsGuest_ReturnsModel() throws Exception {
        var saved = AASModelDto.builder().id("new").build();
        when(aasModelService.createModel(eq("GUEST"), any())).thenReturn(saved);

        mockMvc.perform(post("/guest/models/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(AASModelDto.builder().build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("new"));

        verify(aasModelService).createModel(eq("GUEST"), any());
    }

    @Test
    void saveAASModelAsGuest_ReturnsSaved() throws Exception {
        var saved = AASModelDto.builder().id("m1").build();
        when(aasModelService.saveModel(eq("m1"), eq("GUEST"), any())).thenReturn(saved);

        mockMvc.perform(put("/guest/models/{id}/save","m1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(AASModelDto.builder().build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("m1"));

        verify(aasModelService).saveModel(eq("m1"), eq("GUEST"), any());
    }

    @Test
    void uploadPropertyFileAsGuest_ReturnsResponse() throws Exception {
        // Prepare mock file
        var file = new MockMultipartFile("file","test.txt",MediaType.TEXT_PLAIN_VALUE,"hello".getBytes());
        var resp = UploadResponseDto.builder().fileId("f1").filename("test.txt").contentType("text/plain").size(5).uploadedAt(LocalDateTime.now()).build();
        when(propertyFileUploadService.uploadFile(any(), eq("m1"), eq("GUEST"))).thenReturn(resp);

        mockMvc.perform(multipart("/guest/models/{mid}/upload/property","m1").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value("f1"));

        verify(propertyFileUploadService).uploadFile(any(), eq("m1"), eq("GUEST"));
    }

    @Test
    void deleteFileAsGuest_Returns204() throws Exception {
        mockMvc.perform(delete("/guest/upload/{fid}","f1"))
                .andExpect(status().isNoContent());
        verify(propertyFileUploadService).deleteFile("f1","GUEST");
    }

    @Test
    void exportModel_ReturnsBytesAndHeaders() throws Exception {
        // Prepare mock exported file
        var bytes = "abc".getBytes(StandardCharsets.UTF_8);
        var exported = new ExportedFile(bytes,"file.json","application/json");
        when(exportService.export("m1","name", ExportFormat.JSON,"GUEST")).thenReturn(exported);

        mockMvc.perform(get("/guest/models/{id}/{name}/export/{fmt}","m1","name","JSON"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"file.json\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,"application/json"))
                .andExpect(content().bytes(bytes));

        verify(exportService).export("m1","name",ExportFormat.JSON,"GUEST");
    }
}
