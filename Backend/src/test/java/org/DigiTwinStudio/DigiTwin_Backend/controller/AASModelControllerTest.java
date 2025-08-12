package org.DigiTwinStudio.DigiTwin_Backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.PublishRequestDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ConflictException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ForbiddenException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AASModelController.class) // Only load the web layer for AASModelController
@Import(AASModelControllerTest.TestSecurityConfig.class) // Use custom security config for testing
class AASModelControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for tests
                    .authorizeHttpRequests(a -> a
                            .requestMatchers("/models/**").authenticated() // Require authentication for /models/**
                    )
                    .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults())); // Enable JWT auth, return 401 if missing
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc; // MockMvc for simulating HTTP requests

    @Autowired
    private ObjectMapper objectMapper; // For converting objects to/from JSON

    @MockitoBean
    private AASModelService aasModelService; // Mocked service dependency

    // ===== listAASModels =====
    @Test
    void listAASModels_WithAuth_ReturnsUserModels() throws Exception {
        String userId = "user-123";
        var dto1 = AASModelDto.builder().id("m1").published(false).build();
        var dto2 = AASModelDto.builder().id("m2").published(true).build();
        when(aasModelService.getAllModelsForUser(userId)).thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/models").with(jwt().jwt(j -> j.subject(userId)))) // Simulate request with JWT
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("m1"))
                .andExpect(jsonPath("$[1].id").value("m2"));

        verify(aasModelService).getAllModelsForUser(userId); // Verify service call
    }

    @Test
    void listAASModels_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/models")) // No JWT provided
                .andExpect(status().isUnauthorized());
        verify(aasModelService, never()).getAllModelsForUser(anyString());
    }

    // ===== getAASModel =====
    @Test
    void getAASModel_WithAuth_ReturnsModel() throws Exception {
        String userId = "user-123";
        String modelId = "model-1";
        var dto = AASModelDto.builder().id(modelId).published(false).build();
        when(aasModelService.getModelById(modelId, userId)).thenReturn(dto);

        mockMvc.perform(get("/models/{id}", modelId).with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(modelId));

        verify(aasModelService).getModelById(modelId, userId);
    }

    @Test
    void getAASModel_NotFound_Returns404() throws Exception {
        String userId = "user-123";
        when(aasModelService.getModelById("missing", userId)).thenThrow(new NotFoundException("not found"));

        mockMvc.perform(get("/models/missing").with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAASModel_Forbidden_Returns403() throws Exception {
        String userId = "user-123";
        when(aasModelService.getModelById("model-x", userId)).thenThrow(new ForbiddenException("nope"));

        mockMvc.perform(get("/models/model-x").with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isForbidden());
    }

    // ===== createNewModel =====
    @Test
    void createNewModel_WithAuth_ReturnsOk() throws Exception {
        String userId = "user-123";
        var payload = AASModelDto.builder().id(null).published(false).build();
        var saved = AASModelDto.builder().id("new-id").published(false).build();

        when(aasModelService.createModel(eq(userId), any(AASModelDto.class))).thenReturn(saved);

        mockMvc.perform(post("/models/new")
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("new-id"));

        verify(aasModelService).createModel(eq(userId), any(AASModelDto.class));
    }

    @Test
    void createNewModel_BadRequestFromService_Returns400() throws Exception {
        String userId = "user-123";
        when(aasModelService.createModel(eq(userId), any(AASModelDto.class)))
                .thenThrow(new BadRequestException("invalid"));

        mockMvc.perform(post("/models/new")
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(AASModelDto.builder().build())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createNewModel_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(post("/models/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(AASModelDto.builder().build())))
                .andExpect(status().isUnauthorized());
        verify(aasModelService, never()).createModel(anyString(), any());
    }

    // ===== saveAASModel =====
    @Test
    void saveAASModel_WithAuth_ReturnsSavedDto() throws Exception {
        String userId = "user-123";
        String modelId = "m1";
        var payload = AASModelDto.builder().id(modelId).build();
        var saved = AASModelDto.builder().id(modelId).published(true).build();

        when(aasModelService.saveModel(eq(modelId), eq(userId), any(AASModelDto.class))).thenReturn(saved);

        mockMvc.perform(put("/models/{id}/save", modelId)
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(modelId))
                .andExpect(jsonPath("$.published").value(true));

        verify(aasModelService).saveModel(eq(modelId), eq(userId), any(AASModelDto.class));
    }

    @Test
    void saveAASModel_Forbidden_Returns403() throws Exception {
        String userId = "user-123";
        when(aasModelService.saveModel(eq("m1"), eq(userId), any(AASModelDto.class)))
                .thenThrow(new ForbiddenException("nope"));

        mockMvc.perform(put("/models/{id}/save", "m1")
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(AASModelDto.builder().build())))
                .andExpect(status().isForbidden());
    }

    // ===== deleteAASModel =====
    @Test
    void deleteAASModel_WithAuth_Returns204() throws Exception {
        String userId = "user-123";
        doNothing().when(aasModelService).hardDeleteModel("m1", userId);

        mockMvc.perform(delete("/models/{id}/delete", "m1")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isNoContent());

        verify(aasModelService).hardDeleteModel("m1", userId);
    }

    @Test
    void deleteAASModel_NotFound_Returns404() throws Exception {
        String userId = "user-123";
        doThrow(new NotFoundException("missing")).when(aasModelService).hardDeleteModel("missing", userId);

        mockMvc.perform(delete("/models/{id}/delete", "missing")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isNotFound());
    }

    // ===== publishAASModel =====
    @Test
    void publishAASModel_WithAuth_Returns200() throws Exception {
        String userId = "user-123";
        var req = PublishRequestDto.builder().author("Alice").shortDescription("desc").build();

        doNothing().when(aasModelService).publishModel(eq("m1"), eq(userId), any(PublishRequestDto.class));

        mockMvc.perform(post("/models/{id}/publish", "m1")
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isOk());

        verify(aasModelService).publishModel(eq("m1"), eq(userId), any(PublishRequestDto.class));
    }

    @Test
    void publishAASModel_Conflict_Returns409() throws Exception {
        String userId = "user-123";
        doThrow(new ConflictException("already")).when(aasModelService)
                .publishModel(eq("m1"), eq(userId), any(PublishRequestDto.class));

        mockMvc.perform(post("/models/{id}/publish", "m1")
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(PublishRequestDto.builder().build())))
                .andExpect(status().isConflict());
    }

    // ===== unpublishAASModel =====
    @Test
    void unpublishAASModel_WithAuth_Returns200() throws Exception {
        String userId = "user-123";
        doNothing().when(aasModelService).unpublishModel("m1", userId);

        mockMvc.perform(post("/models/{id}/unpublish", "m1")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isOk());

        verify(aasModelService).unpublishModel("m1", userId);
    }

    @Test
    void unpublishAASModel_Forbidden_Returns403() throws Exception {
        String userId = "user-123";
        doThrow(new ForbiddenException("nope")).when(aasModelService).unpublishModel("m1", userId);

        mockMvc.perform(post("/models/{id}/unpublish", "m1")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isForbidden());
    }
}
