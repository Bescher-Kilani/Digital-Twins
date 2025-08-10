package org.DigiTwinStudio.DigiTwin_Backend.init;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TagRepository;
import org.DigiTwinStudio.DigiTwin_Backend.services.TemplateService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartUpTest {

    @Mock private TemplateService templateService;
    @Mock private TagRepository tagRepository;
    @Mock private ApplicationReadyEvent applicationReadyEvent;

    @InjectMocks
    private StartUp startUp;

    @BeforeEach
    void setup() {
        // Reset mocks before each test
        reset(templateService, tagRepository, applicationReadyEvent);
    }

    // --- onApplicationEvent() Integration Tests ---
    @Test
    void onApplicationEvent_shouldCallTemplateServiceAndTagInitialization() {
        // --- Arrange ---
        when(tagRepository.count()).thenReturn(0L, 2L); // Before and after initialization

        // --- Act ---
        startUp.onApplicationEvent(applicationReadyEvent);

        // --- Assert ---
        verify(templateService).syncTemplatesFromRepo();
        verify(tagRepository, atLeastOnce()).count();
    }

    @Test
    void onApplicationEvent_shouldContinueWhenTemplateServiceThrowsException() {
        // --- Arrange ---
        doThrow(new RuntimeException("Template sync failed")).when(templateService).syncTemplatesFromRepo();
        when(tagRepository.count()).thenReturn(0L);

        // --- Act & Assert ---
        // Should not throw exception despite template service failure
        assertDoesNotThrow(() -> startUp.onApplicationEvent(applicationReadyEvent));

        verify(templateService).syncTemplatesFromRepo();
        verify(tagRepository, atLeastOnce()).count();
    }

    // --- Tag Repository Interaction Tests ---
    @Test
    void tagInitialization_shouldSaveNewTagsFromRepository() throws Exception {
        // --- Arrange ---
        ClassPathResource mockResource = mock(ClassPathResource.class);
        InputStream inputStream = new ByteArrayInputStream(
                "Industry:Manufacturing\nTechnology:Software\nSustainability:Environment".getBytes());
        when(mockResource.getInputStream()).thenReturn(inputStream);
        when(mockResource.getFilename()).thenReturn("test-tags.txt");

        when(tagRepository.count()).thenReturn(0L, 3L);
        when(tagRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- Act ---
        startUp.initializeTags(mockResource);

        // --- Assert ---
        verify(tagRepository, times(3)).findByNameIgnoreCase(anyString());
        verify(tagRepository, times(3)).save(any(Tag.class));

        // Capture saved tags to verify structure
        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository, times(3)).save(tagCaptor.capture());

        // Verify that tags are being created with proper structure
        List<Tag> savedTags = tagCaptor.getAllValues();
        assertEquals(3, savedTags.size());

        for (Tag savedTag : savedTags) {
            assertNotNull(savedTag.getName());
            assertNotNull(savedTag.getCategory());
            assertEquals(0, savedTag.getUsageCount());
        }

        // Verify specific tags were saved
        assertTrue(savedTags.stream().anyMatch(tag -> "Industry".equals(tag.getName()) && "Manufacturing".equals(tag.getCategory())));
        assertTrue(savedTags.stream().anyMatch(tag -> "Technology".equals(tag.getName()) && "Software".equals(tag.getCategory())));
        assertTrue(savedTags.stream().anyMatch(tag -> "Sustainability".equals(tag.getName()) && "Environment".equals(tag.getCategory())));
    }

    @Test
    void tagInitialization_shouldSkipExistingTags() throws Exception {
        // --- Arrange ---
        Tag existingTag = Tag.builder()
                .id("existing-id")
                .name("ExistingTag")
                .category("Existing")
                .usageCount(5)
                .build();

        // Create a mock ClassPathResource that returns our test data
        ClassPathResource mockResource = mock(ClassPathResource.class);
        InputStream inputStream = new ByteArrayInputStream("ExistingTag:Existing\nNewTag:New".getBytes());
        when(mockResource.getInputStream()).thenReturn(inputStream);
        when(mockResource.getFilename()).thenReturn("test-tags.txt");

        when(tagRepository.count()).thenReturn(1L, 1L); // No new tags added
        when(tagRepository.findByNameIgnoreCase("ExistingTag")).thenReturn(Optional.of(existingTag));
        when(tagRepository.findByNameIgnoreCase("NewTag")).thenReturn(Optional.empty());

        Tag newTag = Tag.builder().name("NewTag").category("New").usageCount(0).build();
        when(tagRepository.save(any(Tag.class))).thenReturn(newTag);

        // --- Act ---
        startUp.initializeTags(mockResource);  // Now package-private, no reflection needed

        // --- Assert ---
        verify(tagRepository).findByNameIgnoreCase("ExistingTag");
        verify(tagRepository).findByNameIgnoreCase("NewTag");

        // Verify that existing tag is not saved again (only new tags are saved)
        verify(tagRepository, never()).save(argThat(tag -> "ExistingTag".equals(tag.getName())));

        // Verify that new tag is saved
        verify(tagRepository, times(1)).save(argThat(tag -> "NewTag".equals(tag.getName())));
    }

    @Test
    void tagInitialization_shouldCreateTagWithCorrectDefaultValues() {
        // --- Arrange ---
        when(tagRepository.count()).thenReturn(0L, 1L);
        when(tagRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- Act ---
        startUp.onApplicationEvent(applicationReadyEvent);

        // --- Assert ---
        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository, atLeastOnce()).save(tagCaptor.capture());

        // Verify that at least one tag was created with correct defaults
        assertTrue(tagCaptor.getAllValues().stream().anyMatch(tag ->
                tag.getUsageCount() == 0 &&
                        tag.getName() != null &&
                        tag.getCategory() != null
        ));
    }

    // --- Error Handling Tests ---
    @Test
    void tagInitialization_shouldHandleTagRepositoryExceptions() {
        // --- Arrange ---
        when(tagRepository.count()).thenThrow(new RuntimeException("Database connection failed"));

        // --- Act & Assert ---
        assertDoesNotThrow(() -> startUp.onApplicationEvent(applicationReadyEvent));

        // Template service should still be called even if tag initialization fails
        verify(templateService).syncTemplatesFromRepo();
    }

    @Test
    void tagInitialization_shouldHandleTagSaveException() {
        // --- Arrange ---
        when(tagRepository.count()).thenReturn(0L);
        when(tagRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenThrow(new RuntimeException("Save failed"));

        // --- Act & Assert ---
        assertDoesNotThrow(() -> startUp.onApplicationEvent(applicationReadyEvent));

        verify(templateService).syncTemplatesFromRepo();
    }

    @Test
    void tagInitialization_shouldHandleTagFindException() {
        // --- Arrange ---
        when(tagRepository.count()).thenReturn(0L);
        when(tagRepository.findByNameIgnoreCase(anyString())).thenThrow(new RuntimeException("Find failed"));

        // --- Act & Assert ---
        assertDoesNotThrow(() -> startUp.onApplicationEvent(applicationReadyEvent));

        verify(templateService).syncTemplatesFromRepo();
    }

    // --- Tag Creation Logic Tests ---
    @Test
    void tagCreation_shouldUseBuilderPattern() {
        // --- Arrange ---
        when(tagRepository.count()).thenReturn(0L, 1L);
        when(tagRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());

        // --- Act ---
        startUp.onApplicationEvent(applicationReadyEvent);

        // --- Assert ---
        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository, atLeastOnce()).save(tagCaptor.capture());

        // Verify tag structure matches expected builder pattern usage
        Tag capturedTag = tagCaptor.getAllValues().getFirst();
        assertNotNull(capturedTag);
        assertNotNull(capturedTag.getName());
        assertNotNull(capturedTag.getCategory());
        assertEquals(0, capturedTag.getUsageCount());
    }

    @Test
    void tagInitialization_shouldHandleCountMethodCalls() {
        // --- Arrange ---
        when(tagRepository.count()).thenReturn(5L, 8L); // 5 existing, 3 new added

        // --- Act ---
        startUp.onApplicationEvent(applicationReadyEvent);

        // --- Assert ---
        // Verify count is called at least twice (before and after)
        verify(tagRepository, atLeast(2)).count();
    }

    // --- Service Integration Tests ---
    @Test
    void onApplicationEvent_shouldExecuteInCorrectOrder() {
        // --- Arrange ---
        when(tagRepository.count()).thenReturn(0L);

        // --- Act ---
        startUp.onApplicationEvent(applicationReadyEvent);

        // --- Assert ---
        // Verify template service is called first, then tag operations
        InOrder inOrder = inOrder(templateService, tagRepository);
        inOrder.verify(templateService).syncTemplatesFromRepo();
        inOrder.verify(tagRepository, atLeastOnce()).count();
    }

    @Test
    void onApplicationEvent_shouldCompleteSuccessfully() {
        // --- Arrange ---
        when(tagRepository.count()).thenReturn(0L, 5L);
        when(tagRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- Act & Assert ---
        assertDoesNotThrow(() -> startUp.onApplicationEvent(applicationReadyEvent));

        verify(templateService).syncTemplatesFromRepo();
        verify(tagRepository, atLeast(2)).count();
    }

    // --- Robustness Tests ---
    @Test
    void startUp_shouldHandleNullApplicationReadyEvent() {
        // --- Arrange ---
        when(tagRepository.count()).thenReturn(0L);

        // --- Act & Assert ---
        assertDoesNotThrow(() -> startUp.onApplicationEvent(null));

        verify(templateService).syncTemplatesFromRepo();
    }

    @Test
    void startUp_shouldBeResilientToPartialFailures() {
        // --- Arrange ---
        when(tagRepository.count())
                .thenReturn(0L) // First call succeeds
                .thenThrow(new RuntimeException("Second count failed")); // Second call fails

        // --- Act & Assert ---
        assertDoesNotThrow(() -> startUp.onApplicationEvent(applicationReadyEvent));

        // Verify both initialization steps are attempted
        verify(templateService).syncTemplatesFromRepo();
        verify(tagRepository, atLeastOnce()).count();
    }
}