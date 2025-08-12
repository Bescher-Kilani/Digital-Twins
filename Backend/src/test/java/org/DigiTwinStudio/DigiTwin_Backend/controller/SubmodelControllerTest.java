package org.DigiTwinStudio.DigiTwin_Backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.TemplateDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ConflictException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ForbiddenException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;
import org.DigiTwinStudio.DigiTwin_Backend.services.SubmodelService;
import org.DigiTwinStudio.DigiTwin_Backend.services.TemplateService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubmodelController.class)
@Import(SubmodelControllerTest.TestSecurityConfig.class)
class SubmodelControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(a -> a
                            .requestMatchers("/models/**").authenticated() // protect model-scoped endpoints
                            .anyRequest().permitAll() // templates/new are public
                    )
                    .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubmodelService submodelService;

    @MockitoBean
    private TemplateService templateService;

    @MockitoBean
    private AASModelService aasModelService;

    // -------- /submodels/templates (public)
    @Test
    void listAvailableSubmodelTemplates_ReturnsList() throws Exception {
        var t1 = TemplateDto.builder().id("t1").name("Template 1").descriptions(Map.of("en","desc")).version("1").revision("0").build();
        var t2 = TemplateDto.builder().id("t2").name("Template 2").version("1").revision("1").build();
        when(templateService.getAvailableTemplates()).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/submodels/templates"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("t1"))
                .andExpect(jsonPath("$[1].id").value("t2"));

        verify(templateService).getAvailableTemplates();
    }

    // -------- /submodels/new?templateId=... (public)
    @Test
    void getNewSubmodel_WithTemplateId_ReturnsSubmodel() throws Exception {
        var dto = SubmodelDto.builder().build();
        when(submodelService.createEmptySubmodelFromTemplate("tpl-1")).thenReturn(dto);

        mockMvc.perform(get("/submodels/new").param("templateId", "tpl-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(submodelService).createEmptySubmodelFromTemplate("tpl-1");
    }

    @Test
    void getNewSubmodel_TemplateNotFound_Returns404() throws Exception {
        when(submodelService.createEmptySubmodelFromTemplate("missing"))
                .thenThrow(new NotFoundException("Template not found"));

        mockMvc.perform(get("/submodels/new").param("templateId", "missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getNewSubmodel_BadTemplate_Returns400() throws Exception {
        when(submodelService.createEmptySubmodelFromTemplate("bad"))
                .thenThrow(new BadRequestException("parse error"));

        mockMvc.perform(get("/submodels/new").param("templateId", "bad"))
                .andExpect(status().isBadRequest());
    }

    // -------- POST /models/{modelId}/submodels (auth required)
    @Test
    void addSubmodelToModel_WithAuth_ReturnsUpdatedModel() throws Exception {
        String userId = "user-1";
        String modelId = "m1";
        var payload = SubmodelDto.builder().build();
        var updated = AASModelDto.builder().id(modelId).build();
        when(aasModelService.attachSubmodel(eq(modelId), any(SubmodelDto.class), eq(userId))).thenReturn(updated);

        mockMvc.perform(post("/models/{modelId}/submodels", modelId)
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(modelId));

        verify(aasModelService).attachSubmodel(eq(modelId), any(SubmodelDto.class), eq(userId));
    }

    @Test
    void addSubmodelToModel_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(post("/models/{modelId}/submodels", "m1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(SubmodelDto.builder().build())))
                .andExpect(status().isUnauthorized());
        verify(aasModelService, never()).attachSubmodel(anyString(), any(), anyString());
    }

    @Test
    void addSubmodelToModel_Forbidden_Returns403() throws Exception {
        String userId = "user-1";
        doThrow(new ForbiddenException("no access"))
                .when(aasModelService).attachSubmodel(eq("m1"), any(SubmodelDto.class), eq(userId));

        mockMvc.perform(post("/models/{modelId}/submodels", "m1")
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(SubmodelDto.builder().build())))
                .andExpect(status().isForbidden());
    }

    @Test
    void addSubmodelToModel_Conflict_Returns409() throws Exception {
        String userId = "user-1";
        doThrow(new ConflictException("duplicate"))
                .when(aasModelService).attachSubmodel(eq("m1"), any(SubmodelDto.class), eq(userId));

        mockMvc.perform(post("/models/{modelId}/submodels", "m1")
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(SubmodelDto.builder().build())))
                .andExpect(status().isConflict());
    }

    // -------- GET /models/{modelId}/submodels/{submodelId} (public per controller)
    @Test
    void getSubmodel_ReturnsDto() throws Exception {
        var dto = SubmodelDto.builder().build();
        when(submodelService.getSubmodel("m1", "s1")).thenReturn(dto);

        mockMvc.perform(get("/models/{modelId}/submodels/{submodelId}", "m1", "s1")
                        .with(jwt().jwt(j -> j.subject("user-1"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));


        verify(submodelService).getSubmodel("m1", "s1");
    }

    @Test
    void getSubmodel_NotFound_Returns404() throws Exception {
        when(submodelService.getSubmodel("m1", "missing")).thenThrow(new NotFoundException("submodel not found"));

        mockMvc.perform(get("/models/{modelId}/submodels/{submodelId}", "m1", "missing")
                    .with(jwt().jwt(j -> j.subject("user-1"))))
                .andExpect(status().isNotFound());
    }

    // -------- PUT /models/{modelId}/submodels/{submodelId} (auth required)
    @Test
    void updateSubmodel_WithAuth_ReturnsUpdatedModel() throws Exception {
        String userId = "user-1";
        var payload = SubmodelDto.builder().build();
        var updated = AASModelDto.builder().id("m1").build();
        when(aasModelService.updateSubmodel(eq("m1"), eq("s1"), any(SubmodelDto.class), eq(userId))).thenReturn(updated);

        mockMvc.perform(put("/models/{modelId}/submodels/{submodelId}", "m1", "s1")
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("m1"));

        verify(aasModelService).updateSubmodel(eq("m1"), eq("s1"), any(SubmodelDto.class), eq(userId));
    }

    @Test
    void updateSubmodel_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(put("/models/{modelId}/submodels/{submodelId}", "m1", "s1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(SubmodelDto.builder().build())))
                .andExpect(status().isUnauthorized());
        verify(aasModelService, never()).updateSubmodel(anyString(), anyString(), any(), anyString());
    }

    @Test
    void updateSubmodel_Forbidden_Returns403() throws Exception {
        String userId = "user-1";
        doThrow(new ForbiddenException("no access"))
                .when(aasModelService).updateSubmodel(eq("m1"), eq("s1"), any(SubmodelDto.class), eq(userId));

        mockMvc.perform(put("/models/{modelId}/submodels/{submodelId}", "m1", "s1")
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(SubmodelDto.builder().build())))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSubmodel_Conflict_Returns409() throws Exception {
        String userId = "user-1";
        doThrow(new ConflictException("duplicate"))
                .when(aasModelService).updateSubmodel(eq("m1"), eq("s1"), any(SubmodelDto.class), eq(userId));

        mockMvc.perform(put("/models/{modelId}/submodels/{submodelId}", "m1", "s1")
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(SubmodelDto.builder().build())))
                .andExpect(status().isConflict());
    }

    // -------- DELETE /models/{modelId}/submodels/{submodelId} (auth required)
    @Test
    void removeSubmodelFromModel_WithAuth_Returns204() throws Exception {
        String userId = "user-1";

        var updated = AASModelDto.builder().id("m1").build();
        when(aasModelService.removeSubmodel("m1", "s1", userId)).thenReturn(updated);

        mockMvc.perform(delete("/models/{modelId}/submodels/{submodelId}", "m1", "s1")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isNoContent());

        verify(aasModelService).removeSubmodel("m1", "s1", userId);
    }


    @Test
    void removeSubmodelFromModel_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(delete("/models/{modelId}/submodels/{submodelId}", "m1", "s1"))
                .andExpect(status().isUnauthorized());
        verify(aasModelService, never()).removeSubmodel(anyString(), anyString(), anyString());
    }

    @Test
    void removeSubmodelFromModel_Forbidden_Returns403() throws Exception {
        String userId = "user-1";
        doThrow(new ForbiddenException("no access")).when(aasModelService).removeSubmodel("m1", "s1", userId);

        mockMvc.perform(delete("/models/{modelId}/submodels/{submodelId}", "m1", "s1")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeSubmodelFromModel_NotFound_Returns404() throws Exception {
        String userId = "user-1";
        doThrow(new NotFoundException("not found")).when(aasModelService).removeSubmodel("m1", "missing", userId);

        mockMvc.perform(delete("/models/{modelId}/submodels/{submodelId}", "m1", "missing")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isNotFound());
    }
}
