package org.DigiTwinStudio.DigiTwin_Backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceEntryDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceSearchRequest;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;
import org.DigiTwinStudio.DigiTwin_Backend.services.MarketPlaceService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarketplaceController.class)
@Import(MarketplaceControllerTest.TestSecurityConfig.class)
class MarketplaceControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(a -> a
                            .requestMatchers("/marketplace/**").authenticated()
                            .anyRequest().permitAll()
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
    private MarketPlaceService marketPlaceService;

    @MockitoBean
    private AASModelService aasModelService;

    // -------- GET /marketplace (auth required)
    @Test
    void listAllEntries_WithAuth_ReturnsEntries() throws Exception {
        // Arrange
        String userId = "user-1";
        MarketplaceEntryDto entry = MarketplaceEntryDto.builder()
                .id("entry-1")
                .name("Test Model")
                .author("Test Author")
                .shortDescription("Test Description")
                .tagIds(Arrays.asList("tag1", "tag2"))
                .publishedAt(LocalDateTime.now())
                .downloadCount(10)
                .build();
        List<MarketplaceEntryDto> entries = Collections.singletonList(entry);
        when(marketPlaceService.listAllEntries()).thenReturn(entries);

        // Act & Assert
        mockMvc.perform(get("/marketplace")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("entry-1"))
                .andExpect(jsonPath("$[0].name").value("Test Model"));

        verify(marketPlaceService).listAllEntries();
    }

    @Test
    void listAllEntries_WithoutAuth_Returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/marketplace"))
                .andExpect(status().isUnauthorized());

        verify(marketPlaceService, never()).listAllEntries();
    }

    @Test
    void listAllEntries_WhenEmpty_ReturnsEmptyList() throws Exception {
        // Arrange
        String userId = "user-1";
        when(marketPlaceService.listAllEntries()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/marketplace")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());

        verify(marketPlaceService).listAllEntries();
    }

    @Test
    void listAllEntries_WithLargeDataset_PerformsWell() throws Exception {
        // Arrange
        String userId = "user-1";
        List<MarketplaceEntryDto> largeList = IntStream.range(0, 1000)
                .mapToObj(i -> MarketplaceEntryDto.builder()
                        .id("entry-" + i)
                        .name("Model " + i)
                        .build())
                .collect(Collectors.toList());
        when(marketPlaceService.listAllEntries()).thenReturn(largeList);

        // Act
        long startTime = System.currentTimeMillis();
        mockMvc.perform(get("/marketplace")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1000));
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertTrue(duration < 5000, "Response took too long: " + duration + "ms");
    }

    // -------- GET /marketplace/{entryId} (auth required)
    @Test
    void getModelByEntryId_WithAuth_ReturnsModel() throws Exception {
        // Arrange
        String userId = "user-1";
        AASModelDto model = AASModelDto.builder()
                .id("model-1")
                .build();
        when(marketPlaceService.getPublishedModel("entry-1")).thenReturn(model);

        // Act & Assert
        mockMvc.perform(get("/marketplace/entry-1")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("model-1"));

        verify(marketPlaceService).getPublishedModel("entry-1");
    }

    @Test
    void getModelByEntryId_WithoutAuth_Returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/marketplace/entry-1"))
                .andExpect(status().isUnauthorized());

        verify(marketPlaceService, never()).getPublishedModel(anyString());
    }

    @Test
    void getModelByEntryId_NotFound_Returns404() throws Exception {
        // Arrange
        String userId = "user-1";
        when(marketPlaceService.getPublishedModel("non-existent"))
                .thenThrow(new NotFoundException("Entry not found"));

        // Act & Assert
        mockMvc.perform(get("/marketplace/non-existent")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isNotFound());

        verify(marketPlaceService).getPublishedModel("non-existent");
    }

    // -------- GET /marketplace/tags (auth required)
    @Test
    void getAllTags_ReturnsTags() throws Exception {
        // Arrange
        String userId = "user-1";
        Tag tag = Tag.builder()
                .id("tag-1")
                .name("Industrial")
                .category("Domain")
                .usageCount(5)
                .build();
        List<Tag> tags = Collections.singletonList(tag);
        when(marketPlaceService.getAllTags()).thenReturn(tags);

        // Act & Assert
        mockMvc.perform(get("/marketplace/tags")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("tag-1"))
                .andExpect(jsonPath("$[0].name").value("Industrial"));

        verify(marketPlaceService).getAllTags();
    }

    @Test
    void getAllTags_WhenEmpty_ReturnsEmptyList() throws Exception {
        // Arrange
        String userId = "user-1";
        when(marketPlaceService.getAllTags()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/marketplace/tags")
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());

        verify(marketPlaceService).getAllTags();
    }

    // -------- POST /marketplace/search (auth required)
    @Test
    void search_WithValidRequest_ReturnsResults() throws Exception {
        // Arrange
        String userId = "user-1";
        MarketplaceSearchRequest request = MarketplaceSearchRequest.builder()
                .searchText("test")
                .build();
        MarketplaceEntryDto entry = MarketplaceEntryDto.builder()
                .id("entry-1")
                .name("Test Model")
                .build();
        List<MarketplaceEntryDto> results = Collections.singletonList(entry);
        when(marketPlaceService.search(any(MarketplaceSearchRequest.class))).thenReturn(results);

        // Act & Assert
        mockMvc.perform(post("/marketplace/search")
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("entry-1"));

        verify(marketPlaceService).search(any(MarketplaceSearchRequest.class));
    }

    @Test
    void search_WithEmptyRequest_ReturnsResults() throws Exception {
        // Arrange
        String userId = "user-1";
        MarketplaceSearchRequest request = MarketplaceSearchRequest.builder().build();
        List<MarketplaceEntryDto> results = Collections.emptyList();
        when(marketPlaceService.search(any(MarketplaceSearchRequest.class))).thenReturn(results);

        // Act & Assert
        mockMvc.perform(post("/marketplace/search")
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(marketPlaceService).search(any(MarketplaceSearchRequest.class));
    }

    @Test
    void search_WithComplexSearchRequest_ReturnsResults() throws Exception {
        // Arrange
        String userId = "user-1";
        MarketplaceSearchRequest request = MarketplaceSearchRequest.builder()
                .searchText("complex search")
                .tagIds(Arrays.asList("tag1", "tag2"))
                .publishedAfter(LocalDateTime.now().minusDays(7))
                .build();
        MarketplaceEntryDto entry = MarketplaceEntryDto.builder()
                .id("entry-1")
                .name("Complex Model")
                .build();
        when(marketPlaceService.search(any(MarketplaceSearchRequest.class)))
                .thenReturn(Collections.singletonList(entry));

        // Act & Assert
        mockMvc.perform(post("/marketplace/search")
                        .with(jwt().jwt(j -> j.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Complex Model"));

        verify(marketPlaceService).search(any(MarketplaceSearchRequest.class));
    }

    // -------- POST /marketplace/{entryId}/add-to-user (auth required)
    @Test
    void addEntryToUser_WithAuth_ReturnsOk() throws Exception {
        // Arrange
        String entryId = "entry-1";
        String userId = "user-123";
        doNothing().when(aasModelService).addEntryModelToUser(entryId, userId);

        // Act & Assert
        mockMvc.perform(post("/marketplace/{entryId}/add-to-user", entryId)
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isOk());

        verify(aasModelService).addEntryModelToUser(entryId, userId);
    }

    @Test
    void addEntryToUser_WithoutAuth_Returns401() throws Exception {
        // Arrange
        String entryId = "entry-1";

        // Act & Assert
        mockMvc.perform(post("/marketplace/{entryId}/add-to-user", entryId))
                .andExpect(status().isUnauthorized());

        verify(aasModelService, never()).addEntryModelToUser(anyString(), anyString());
    }

    @Test
    void addEntryToUser_WhenServiceThrowsException_ReturnsError() throws Exception {
        // Arrange
        String entryId = "entry-1";
        String userId = "user-123";
        doThrow(new RuntimeException("Service error"))
                .when(aasModelService).addEntryModelToUser(entryId, userId);

        // Act & Assert
        mockMvc.perform(post("/marketplace/{entryId}/add-to-user", entryId)
                        .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isInternalServerError());

        verify(aasModelService).addEntryModelToUser(entryId, userId);
    }
}