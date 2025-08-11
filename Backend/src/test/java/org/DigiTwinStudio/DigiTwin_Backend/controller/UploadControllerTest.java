package org.DigiTwinStudio.DigiTwin_Backend.controller;

import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.UploadResponseDto;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelUploadService;
import org.DigiTwinStudio.DigiTwin_Backend.services.PropertyFileUploadService;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.UploadException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UploadController.class)
@Import(UploadControllerTest.TestSecurityConfig.class)
class UploadControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable);
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AASModelUploadService aasModelUploadService;

    @MockitoBean
    private PropertyFileUploadService propertyFileUploadService;

    @Test
    void uploadModelFile_WithValidJsonFile_ReturnsCreated() throws Exception {
        // Given
        String userId = "user-123";
        String jsonContent = "{\"assetAdministrationShells\": [], \"submodels\": []}";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-model.json",
                MediaType.APPLICATION_JSON_VALUE,
                jsonContent.getBytes()
        );

        AASModelDto expectedDto = AASModelDto.builder()
                .id("model-1")
                .published(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(aasModelUploadService.uploadAASModel(any(), eq(userId)))
                .thenReturn(expectedDto);

        // When & Then
        mockMvc.perform(multipart("/api/upload/model")
                        .file(file)
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("model-1"))
                .andExpect(jsonPath("$.published").value(false));

        verify(aasModelUploadService).uploadAASModel(any(), eq(userId));
    }

    @Test
    void uploadModelFile_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                MediaType.APPLICATION_JSON_VALUE,
                "{}".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/upload/model").file(file))
                .andExpect(status().isUnauthorized());

        verify(aasModelUploadService, never()).uploadAASModel(any(), anyString());
    }

    @Test
    void uploadModelFile_WithInvalidFile_ReturnsBadRequest() throws Exception {
        // Given
        String userId = "user-123";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid.json",
                MediaType.APPLICATION_JSON_VALUE,
                "invalid json".getBytes()
        );

        when(aasModelUploadService.uploadAASModel(any(), eq(userId)))
                .thenThrow(new UploadException("Invalid file format"));

        // When & Then
        mockMvc.perform(multipart("/api/upload/model")
                        .file(file)
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isBadRequest());

        verify(aasModelUploadService).uploadAASModel(any(), eq(userId));
    }

    @Test
    void uploadPropertyFile_WithValidFile_ReturnsOk() throws Exception {
        // Given
        String userId = "user-123";
        String modelId = "model-456";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        UploadResponseDto expectedResponse = UploadResponseDto.builder()
                .fileId("file-789")
                .filename("document.pdf")
                .contentType(MediaType.APPLICATION_PDF_VALUE)
                .size(11L)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(propertyFileUploadService.uploadFile(any(), eq(modelId), eq(userId)))
                .thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(multipart("/api/upload/property")
                        .file(file)
                        .param("modelId", modelId)
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.fileId").value("file-789"))
                .andExpect(jsonPath("$.filename").value("document.pdf"))
                .andExpect(jsonPath("$.contentType").value(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(jsonPath("$.size").value(11));

        verify(propertyFileUploadService).uploadFile(any(), eq(modelId), eq(userId));
    }

    @Test
    void uploadPropertyFile_WithoutModelId_ReturnsBadRequest() throws Exception {
        // Given
        String userId = "user-123";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/upload/property")
                        .file(file)
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isBadRequest());

        verify(propertyFileUploadService, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test
    void uploadPropertyFile_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/upload/property")
                        .file(file)
                        .param("modelId", "model-123"))
                .andExpect(status().isUnauthorized());

        verify(propertyFileUploadService, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test
    void uploadPropertyFile_WithInvalidFile_ReturnsBadRequest() throws Exception {
        // Given
        String userId = "user-123";
        String modelId = "model-456";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid.exe",
                "application/octet-stream",
                "executable content".getBytes()
        );

        when(propertyFileUploadService.uploadFile(any(), eq(modelId), eq(userId)))
                .thenThrow(new UploadException("Invalid file type"));

        // When & Then
        mockMvc.perform(multipart("/api/upload/property")
                        .file(file)
                        .param("modelId", modelId)
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isBadRequest());

        verify(propertyFileUploadService).uploadFile(any(), eq(modelId), eq(userId));
    }

    @Test
    void deleteFile_WithValidFileId_ReturnsNoContent() throws Exception {
        // Given
        String userId = "user-123";
        String fileId = "file-789";

        doNothing().when(propertyFileUploadService).deleteFile(fileId, userId);

        // When & Then
        mockMvc.perform(delete("/api/upload/{fileId}", fileId)
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isNoContent());

        verify(propertyFileUploadService).deleteFile(fileId, userId);
    }

    @Test
    void deleteFile_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/upload/file-123"))
                .andExpect(status().isUnauthorized());

        verify(propertyFileUploadService, never()).deleteFile(anyString(), anyString());
    }

    @Test
    void deleteFile_WithNonExistentFile_ReturnsInternalServerError() throws Exception {
        // Given
        String userId = "user-123";
        String fileId = "non-existent-file";

        doThrow(new RuntimeException("File not found"))
                .when(propertyFileUploadService).deleteFile(fileId, userId);

        // When & Then
        mockMvc.perform(delete("/api/upload/{fileId}", fileId)
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isInternalServerError());

        verify(propertyFileUploadService).deleteFile(fileId, userId);
    }

    @Test
    void uploadModelFile_WithEmptyFile_ReturnsBadRequest() throws Exception {
        // Given
        String userId = "user-123";
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.json",
                MediaType.APPLICATION_JSON_VALUE,
                new byte[0]
        );

        when(aasModelUploadService.uploadAASModel(any(), eq(userId)))
                .thenThrow(new UploadException("File is empty"));

        // When & Then
        mockMvc.perform(multipart("/api/upload/model")
                        .file(emptyFile)
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isBadRequest());

        verify(aasModelUploadService).uploadAASModel(any(), eq(userId));
    }

    @Test
    void uploadPropertyFile_WithImageFile_ReturnsOk() throws Exception {
        // Given
        String userId = "user-123";
        String modelId = "model-456";
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "image.png",
                MediaType.IMAGE_PNG_VALUE,
                "PNG image data".getBytes()
        );

        UploadResponseDto expectedResponse = UploadResponseDto.builder()
                .fileId("image-789")
                .filename("image.png")
                .contentType(MediaType.IMAGE_PNG_VALUE)
                .size(15L)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(propertyFileUploadService.uploadFile(any(), eq(modelId), eq(userId)))
                .thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(multipart("/api/upload/property")
                        .file(imageFile)
                        .param("modelId", modelId)
                        .with(jwt().jwt(builder -> builder.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentType").value(MediaType.IMAGE_PNG_VALUE));

        verify(propertyFileUploadService).uploadFile(any(), eq(modelId), eq(userId));
    }
}