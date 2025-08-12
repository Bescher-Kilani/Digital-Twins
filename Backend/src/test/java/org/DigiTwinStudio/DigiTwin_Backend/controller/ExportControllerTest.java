package org.DigiTwinStudio.DigiTwin_Backend.controller;

import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportedFile;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ExportException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ForbiddenException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.services.ExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExportController.class)
@Import(ExportControllerTest.TestSecurityConfig.class)
class ExportControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(a -> a
                            .requestMatchers("/models/**").authenticated()
                            .anyRequest().permitAll()
                    )
                    .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExportService exportService;

    // -------- GET /models/{id}/{name}/export/{format} (auth required)
    @Test
    void exportModel_WithValidJsonFormat_ReturnsOkWithJsonFile() throws Exception {
        // Arrange
        String userId = "user-123";
        String modelId = "model-456";
        String fileName = "test-model";
        String jsonContent = "{\"assetAdministrationShells\": [], \"submodels\": []}";
        ExportedFile exportedFile = new ExportedFile(
                jsonContent.getBytes(),
                "test-model.json",
                "application/json"
        );

        when(exportService.export(eq(modelId), eq(fileName), eq(ExportFormat.JSON), eq(userId)))
                .thenReturn(exportedFile);

        // Act & Assert
        mockMvc.perform(get("/models/{id}/{name}/export/{format}", modelId, fileName, "JSON")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test-model.json\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json"))
                .andExpect(content().bytes(jsonContent.getBytes()));

        verify(exportService).export(eq(modelId), eq(fileName), eq(ExportFormat.JSON), eq(userId));
    }

    @Test
    void exportModel_WithValidAasxFormat_ReturnsOkWithAasxFile() throws Exception {
        // Arrange
        String userId = "user-123";
        String modelId = "model-456";
        String fileName = "test-model";
        byte[] aasxContent = "AASX binary content".getBytes();
        ExportedFile exportedFile = new ExportedFile(
                aasxContent,
                "test-model.aasx",
                "application/asset-administration-shell-package"
        );

        when(exportService.export(eq(modelId), eq(fileName), eq(ExportFormat.AASX), eq(userId)))
                .thenReturn(exportedFile);

        // Act & Assert
        mockMvc.perform(get("/models/{id}/{name}/export/{format}", modelId, fileName, "AASX")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test-model.aasx\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/asset-administration-shell-package"))
                .andExpect(content().bytes(aasxContent));

        verify(exportService).export(eq(modelId), eq(fileName), eq(ExportFormat.AASX), eq(userId));
    }

    @Test
    void exportModel_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        // Arrange
        String modelId = "model-123";
        String fileName = "test-model";

        // Act & Assert
        mockMvc.perform(get("/models/{id}/{name}/export/{format}", modelId, fileName, "JSON"))
                .andExpect(status().isUnauthorized());

        verify(exportService, never()).export(anyString(), anyString(), any(ExportFormat.class), anyString());
    }

    @Test
    void exportModel_WithNonExistentModel_ReturnsNotFound() throws Exception {
        // Arrange
        String userId = "user-123";
        String modelId = "non-existent-model";
        String fileName = "test-model";

        when(exportService.export(eq(modelId), eq(fileName), eq(ExportFormat.JSON), eq(userId)))
                .thenThrow(new NotFoundException("Could not find model with given Id"));

        // Act & Assert
        mockMvc.perform(get("/models/{id}/{name}/export/{format}", modelId, fileName, "JSON")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isNotFound());

        verify(exportService).export(eq(modelId), eq(fileName), eq(ExportFormat.JSON), eq(userId));
    }

    @Test
    void exportModel_WithUnauthorizedAccess_ReturnsForbidden() throws Exception {
        // Arrange
        String userId = "user-123";
        String modelId = "model-456";
        String fileName = "test-model";

        when(exportService.export(eq(modelId), eq(fileName), eq(ExportFormat.JSON), eq(userId)))
                .thenThrow(new ForbiddenException("Access denied: model does not belong to user."));

        // Act & Assert
        mockMvc.perform(get("/models/{id}/{name}/export/{format}", modelId, fileName, "JSON")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isForbidden());

        verify(exportService).export(eq(modelId), eq(fileName), eq(ExportFormat.JSON), eq(userId));
    }

    @Test
    void exportModel_WithExportException_ReturnsInternalServerError() throws Exception {
        // Arrange
        String userId = "user-123";
        String modelId = "model-456";
        String fileName = "test-model";

        when(exportService.export(eq(modelId), eq(fileName), eq(ExportFormat.AASX), eq(userId)))
                .thenThrow(new ExportException("Failed to serialize AAS object to AASX"));

        // Act & Assert
        mockMvc.perform(get("/models/{id}/{name}/export/{format}", modelId, fileName, "AASX")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isInternalServerError());

        verify(exportService).export(eq(modelId), eq(fileName), eq(ExportFormat.AASX), eq(userId));
    }

    @Test
    void exportModel_WithInvalidFormat_ReturnsBadRequest() throws Exception {
        // Arrange
        String userId = "user-123";
        String modelId = "model-456";
        String fileName = "test-model";

        // Act & Assert
        mockMvc.perform(get("/models/{id}/{name}/export/{format}", modelId, fileName, "INVALID_FORMAT")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isBadRequest());

        verify(exportService, never()).export(anyString(), anyString(), any(ExportFormat.class), anyString());
    }

    @Test
    void exportModel_WithSpecialCharactersInFileName_ReturnsOkWithEscapedFileName() throws Exception {
        // Arrange
        String userId = "user-123";
        String modelId = "model-456";
        String fileName = "test model with spaces";
        String jsonContent = "{\"assetAdministrationShells\": []}";
        ExportedFile exportedFile = new ExportedFile(
                jsonContent.getBytes(),
                "test model with spaces.json",
                "application/json"
        );

        when(exportService.export(eq(modelId), eq(fileName), eq(ExportFormat.JSON), eq(userId)))
                .thenReturn(exportedFile);

        // Act & Assert
        mockMvc.perform(get("/models/{id}/{name}/export/{format}", modelId, fileName, "JSON")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test model with spaces.json\""));

        verify(exportService).export(eq(modelId), eq(fileName), eq(ExportFormat.JSON), eq(userId));
    }

    @Test
    void exportModel_WithEmptyContent_ReturnsOkWithEmptyFile() throws Exception {
        // Arrange
        String userId = "user-123";
        String modelId = "model-456";
        String fileName = "empty-model";
        ExportedFile exportedFile = new ExportedFile(
                new byte[0],
                "empty-model.json",
                "application/json"
        );

        when(exportService.export(eq(modelId), eq(fileName), eq(ExportFormat.JSON), eq(userId)))
                .thenReturn(exportedFile);

        // Act & Assert
        mockMvc.perform(get("/models/{id}/{name}/export/{format}", modelId, fileName, "JSON")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[0]));

        verify(exportService).export(eq(modelId), eq(fileName), eq(ExportFormat.JSON), eq(userId));
    }

    @Test
    void exportModel_WithLargeFile_ReturnsOkWithFullContent() throws Exception {
        // Arrange
        String userId = "user-123";
        String modelId = "model-456";
        String fileName = "large-model";
        // Simulate a large file (1MB)
        byte[] largeContent = new byte[1024 * 1024];
        java.util.Arrays.fill(largeContent, (byte) 'A');
        ExportedFile exportedFile = new ExportedFile(
                largeContent,
                "large-model.aasx",
                "application/asset-administration-shell-package"
        );

        when(exportService.export(eq(modelId), eq(fileName), eq(ExportFormat.AASX), eq(userId)))
                .thenReturn(exportedFile);

        // Act & Assert
        mockMvc.perform(get("/models/{id}/{name}/export/{format}", modelId, fileName, "AASX")
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(content().bytes(largeContent));

        verify(exportService).export(eq(modelId), eq(fileName), eq(ExportFormat.AASX), eq(userId));
    }
}