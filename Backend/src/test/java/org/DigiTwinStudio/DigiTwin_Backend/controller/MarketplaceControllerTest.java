package org.DigiTwinStudio.DigiTwin_Backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceEntryDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceSearchRequest;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelService;
import org.DigiTwinStudio.DigiTwin_Backend.services.MarketPlaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
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
            http.authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable);
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

    @Test
    void listAllEntries_ReturnsEntries() throws Exception {
        // Given
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

        // When & Then
        mockMvc.perform(get("/marketplace"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("entry-1"))
                .andExpect(jsonPath("$[0].name").value("Test Model"));

        verify(marketPlaceService).listAllEntries();
    }

    @Test
    void getModelByEntryId_ReturnsModel() throws Exception {
        // Given
        AASModelDto model = AASModelDto.builder()
                .id("model-1")
                .build();
        when(marketPlaceService.getPublishedModel("entry-1")).thenReturn(model);

        // When & Then
        mockMvc.perform(get("/marketplace/entry-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("model-1"));

        verify(marketPlaceService).getPublishedModel("entry-1");
    }

    @Test
    void getAllTags_ReturnsTags() throws Exception {
        // Given
        Tag tag = Tag.builder()
                .id("tag-1")
                .name("Industrial")
                .category("Domain")
                .usageCount(5)
                .build();

        List<Tag> tags = Collections.singletonList(tag);
        when(marketPlaceService.getAllTags()).thenReturn(tags);

        // When & Then
        mockMvc.perform(get("/marketplace/tags"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("tag-1"))
                .andExpect(jsonPath("$[0].name").value("Industrial"));

        verify(marketPlaceService).getAllTags();
    }

    @Test
    void search_WithValidRequest_ReturnsResults() throws Exception {
        // Given
        MarketplaceSearchRequest request = MarketplaceSearchRequest.builder()
                .searchText("test")
                .build();

        MarketplaceEntryDto entry = MarketplaceEntryDto.builder()
                .id("entry-1")
                .name("Test Model")
                .build();

        List<MarketplaceEntryDto> results = Collections.singletonList(entry);
        when(marketPlaceService.search(any(MarketplaceSearchRequest.class))).thenReturn(results);

        // When & Then
        mockMvc.perform(post("/marketplace/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("entry-1"));

        verify(marketPlaceService).search(any(MarketplaceSearchRequest.class));
    }

    @Test
    void addEntryToUser_WithValidEntryId_ReturnsOk() throws Exception {
        // Given
        String entryId = "entry-1";
        String userId = "user-123";

        // Create a mock JWT
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getSubject()).thenReturn(userId);

        doNothing().when(aasModelService).addEntryModelToUser(entryId, userId);

        // When & Then
        mockMvc.perform(post("/marketplace/{entryId}/add-to-user", entryId)
                        .with(jwt().jwt(mockJwt)))
                .andExpect(status().isOk());

        verify(aasModelService).addEntryModelToUser(entryId, userId);
    }

    @Test
    void listAllEntries_WhenEmpty_ReturnsEmptyList() throws Exception {
        // Given
        when(marketPlaceService.listAllEntries()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/marketplace"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());

        verify(marketPlaceService).listAllEntries();
    }

    @Test
    void getModelByEntryId_WithNonExistentId_ThrowsException() throws Exception {
        // Given
        when(marketPlaceService.getPublishedModel("non-existent"))
                .thenThrow(new RuntimeException("Entry not found"));

        // When & Then
        mockMvc.perform(get("/marketplace/non-existent"))
                .andExpect(status().isInternalServerError());

        verify(marketPlaceService).getPublishedModel("non-existent");
    }

    @Test
    void getAllTags_WhenEmpty_ReturnsEmptyList() throws Exception {
        // Given
        when(marketPlaceService.getAllTags()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/marketplace/tags"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());

        verify(marketPlaceService).getAllTags();
    }

    @Test
    void search_WithEmptyRequest_ReturnsResults() throws Exception {
        // Given
        MarketplaceSearchRequest request = MarketplaceSearchRequest.builder().build();

        List<MarketplaceEntryDto> results = Collections.emptyList();
        when(marketPlaceService.search(any(MarketplaceSearchRequest.class))).thenReturn(results);

        // When & Then
        mockMvc.perform(post("/marketplace/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(marketPlaceService).search(any(MarketplaceSearchRequest.class));
    }

    @Test
    void search_WithComplexSearchRequest_ReturnsResults() throws Exception {
        // Given
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

        // When & Then
        mockMvc.perform(post("/marketplace/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Complex Model"));

        verify(marketPlaceService).search(any(MarketplaceSearchRequest.class));
    }

    @Test
    void addEntryToUser_WhenServiceThrowsException_ReturnsError() throws Exception {
        // Given
        String entryId = "entry-1";
        String userId = "user-123";

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getSubject()).thenReturn(userId);

        doThrow(new RuntimeException("Service error"))
                .when(aasModelService).addEntryModelToUser(entryId, userId);

        // When & Then
        mockMvc.perform(post("/marketplace/{entryId}/add-to-user", entryId)
                        .with(jwt().jwt(mockJwt)))
                .andExpect(status().isInternalServerError());

        verify(aasModelService).addEntryModelToUser(entryId, userId);
    }

    // Performance/Load testing considerations
    @Test
    void listAllEntries_WithLargeDataset_PerformsWell() throws Exception {
        // Given - simulate large dataset
        List<MarketplaceEntryDto> largeList = IntStream.range(0, 1000)
                .mapToObj(i -> MarketplaceEntryDto.builder()
                        .id("entry-" + i)
                        .name("Model " + i)
                        .build())
                .collect(Collectors.toList());

        when(marketPlaceService.listAllEntries()).thenReturn(largeList);

        // When & Then
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/marketplace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1000));

        long duration = System.currentTimeMillis() - startTime;
        // Assert reasonable response time (adjust the threshold as needed)
        assertTrue(duration < 5000, "Response took too long: " + duration + "ms");
    }
}