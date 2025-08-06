package org.DigiTwinStudio.DigiTwin_Backend.services;

import org.DigiTwinStudio.DigiTwin_Backend.domain.*;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.*;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ForbiddenException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.MarketplaceMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.MarketPlaceEntryRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TagRepository;

import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class MarketPlaceServiceTest {

    @Mock private MarketPlaceEntryRepository entryRepo;
    @Mock private AASModelRepository modelRepo;
    @Mock private TagRepository tagRepo;
    @Mock private MarketplaceMapper marketplaceMapper;
    @Mock private AASModelMapper aasModelMapper;
    @Mock private MongoTemplate mongoTemplate;

    @InjectMocks
    private MarketPlaceService service;

    private AASModel model;
    private PublishRequestDto publishRequest;
    private Tag tag;
    private final String authorId = "test-author";
    private final String userId = "test-user-id";
    private final String tagId = "test-tag-id";
    private final String modelId = "test-model-id";

    @BeforeEach
    void setup() {
        String tagName = "test-tag-name";
        tag = Tag.builder()
                .id(tagId)
                .name(tagName)
                .category("test-category")
                .usageCount(0)
                .build();

        publishRequest = PublishRequestDto.builder()
                .author(authorId)
                .shortDescription("test-short-description")
                .tagIds(List.of(tagId))
                .build();

        model = AASModel.builder()
                .id(modelId)
                .ownerId(userId)
                .aas(new DefaultAssetAdministrationShell.Builder()
                        .id("test-aas")
                        .idShort("TestModel")
                        .build())
                .submodels(List.of(new  DefaultSubmodel.Builder()
                        .id("test-submodel")
                        .build()))
                .published(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- Publish ---
    @Test
    void publish_shouldUpdateModelAndSaveMarketplaceEntry() {
        when(tagRepo.findByIdIn(List.of(tagId))).thenReturn(List.of(tag)); // valid tag exists
        when(entryRepo.save(any())).thenReturn(null); // return value not important here

        service.publish(publishRequest, model);

        // Verify model saved with published = true and correct metadata
        verify(modelRepo).save(argThat(savedModel ->
                savedModel.isPublished() &&
                        savedModel.getPublishMetadata() != null &&
                        authorId.equals(savedModel.getPublishMetadata().getAuthor())
        ));

        // Verify marketplace entry is saved
        verify(entryRepo).save(any(MarketplaceEntry.class));

        // Verify tag usageCount incremented and tags saved
        verify(tagRepo).saveAll(argThat(tags -> {
            List<Tag> tagList = new ArrayList<>();
            tags.forEach(tagList::add);
            return tagList.size() == 1 && tagList.getFirst().getUsageCount() == 1;
        }));
    }

    @Test
    void publish_shouldThrowWhenTagIdInvalid() {
        when(tagRepo.findByIdIn(List.of(tagId))).thenReturn(List.of()); // no tags found → invalid

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.publish(publishRequest, model));
        assertTrue(ex.getMessage().contains("Invalid tag IDs"));
    }

    // --- Unpublish ---
    @Test
    void unpublish_shouldResetMetadataAndDeleteEntry() {
        // --- Arrange ---

        // Simulate a model that is already published, including full PublishMetadata
        model.setPublished(true);
        model.setPublishMetadata(PublishMetadata.builder()
                .author("someAuthor")
                .shortDescription("Test description")
                .tagIds(List.of(tagId))
                .publishedAt(LocalDateTime.now())
                .build()
        );

        // Assume the tag already has a usage count of 5
        tag.setUsageCount(5);

        // Mock the repository to return the tag when looked up
        when(tagRepo.findByIdIn(List.of(tagId))).thenReturn(List.of(tag));

        // Mock the repository to return a MarketplaceEntry for the given model ID
        MarketplaceEntry entry = MarketplaceEntry.builder().id(modelId).build();
        when(entryRepo.findById(modelId)).thenReturn(Optional.of(entry));

        // --- Act ---

        service.unpublish(userId, model);

        // --- Assert ---

        // Verify that the model was saved with "unpublished" status and metadata removed
        verify(modelRepo).save(argThat(m ->
                !m.isPublished() &&
                        m.getPublishMetadata() == null
        ));

        // Verify that the corresponding marketplace entry was deleted
        verify(entryRepo).delete(entry);

        // Verify that the tag's usage count was decremented and saved
        verify(tagRepo).saveAll(argThat(tags -> {
            List<Tag> tagList = new ArrayList<>();
            tags.forEach(tagList::add);
            return tagList.size() == 1 && tagList.getFirst().getUsageCount() == 4;
        }));
    }

    @Test
    void unpublish_shouldThrowWhenUserIsNotOwner() {
        // --- Arrange ---

        // Set the model's owner to a different user than the one performing the operation
        model.setOwnerId("other-user");

        // --- Act & Assert ---

        // Expect a ForbiddenException because the user is not the model's owner
        assertThrows(ForbiddenException.class, () ->
                service.unpublish(userId, model)
        );
    }

    @Test
    void unpublish_shouldThrowWhenMarketplaceEntryIsMissing() {
        // --- Arrange ---

        // The model is published (required to trigger marketplace entry lookup)
        model.setPublished(true);
        model.setOwnerId(userId); // Ensure user is the owner to bypass ownership check

        // Simulate the marketplace entry being absent
        when(entryRepo.findById(modelId)).thenReturn(Optional.empty());

        // --- Act & Assert ---

        // Expect a NotFoundException because the model has no marketplace entry
        assertThrows(NotFoundException.class, () ->
                service.unpublish(userId, model)
        );
    }

    // --- List All Entries ---
    @Test
    void listAllEntries_shouldReturnMappedDtos() {
        // --- Arrange ---

        // Prepare a fake MarketplaceEntry from the repository
        MarketplaceEntry entry = MarketplaceEntry.builder().id("id1").build();

        // Expected DTO that the mapper should produce
        MarketplaceEntryDto dto = new MarketplaceEntryDto();

        // Mock repository and mapper behavior
        when(entryRepo.findAll()).thenReturn(List.of(entry));
        when(marketplaceMapper.toDto(entry)).thenReturn(dto);

        // --- Act ---

        List<MarketplaceEntryDto> result = service.listAllEntries();

        // --- Assert ---

        // Ensure one DTO is returned
        assertEquals(1, result.size());

        // Verify that mapping occurred as expected
        verify(marketplaceMapper).toDto(entry);
    }

    // --- Get Published Model ---
    @Test
    void getPublishedModel_shouldReturnDto() {
        // --- Arrange ---

        // Simulate the repository returning a model for the given ID
        when(modelRepo.findById("entryId")).thenReturn(Optional.of(model));

        // Simulate the mapping of the model to its corresponding DTO
        AASModelDto dto = new AASModelDto();
        when(aasModelMapper.toDto(model)).thenReturn(dto);

        // --- Act ---

        AASModelDto result = service.getPublishedModel("entryId");

        // --- Assert ---

        // Ensure the result is the expected DTO
        assertEquals(dto, result);
    }

    @Test
    void getPublishedModel_shouldThrowIfNotFound() {
        // --- Arrange ---

        // Simulate that no model exists for the given ID
        when(modelRepo.findById("entryId")).thenReturn(Optional.empty());

        // --- Act & Assert ---

        // Expect a BadRequestException when the model is not found
        assertThrows(BadRequestException.class, () ->
                service.getPublishedModel("entryId")
        );
    }

    // --- Increment Download Count ---
    @Test
    void incrementDownloadCount_shouldIncreaseAndSave() {
        // --- Arrange ---

        // Create a MarketplaceEntry with a starting download count of 5
        MarketplaceEntry entry = MarketplaceEntry.builder()
                .id("entryId")
                .downloadCount(5)
                .build();

        // Mock the repository to return the entry when searched by ID
        when(entryRepo.findById("entryId")).thenReturn(Optional.of(entry));

        // --- Act ---

        service.incrementDownloadCount("entryId");

        // --- Assert ---

        // The download count should now be incremented to 6
        assertEquals(6, entry.getDownloadCount());

        // The updated entry should be saved back to the repository
        verify(entryRepo).save(entry);
    }

    @Test
    void incrementDownloadCount_shouldThrowIfNotFound() {
        // --- Arrange ---

        // Simulate the repository returning nothing for the given ID
        when(entryRepo.findById("missing")).thenReturn(Optional.empty());

        // --- Act & Assert ---

        // Expect a BadRequestException because the entry does not exist
        assertThrows(BadRequestException.class, () ->
                service.incrementDownloadCount("missing")
        );
    }

    // --- Get All Tags ---
    @Test
    void getAllTags_shouldReturnAllTags() {
        // --- Arrange ---

        // Prepare a mock list of tags (in this case, just one for simplicity)
        List<Tag> tags = List.of(tag);

        // Mock repository behavior to return the tag list
        when(tagRepo.findAll()).thenReturn(tags);

        // --- Act ---

        List<Tag> result = service.getAllTags();

        // --- Assert ---

        // Ensure the service returns the exact list from the repository
        assertEquals(tags, result);
    }

    // --- Search (Basic Test Only) ---
    @Test
    void search_withoutText_shouldUseBasicQuery() {
        // --- Arrange ---

        // Create a search request with tag filters and date filters, but no text
        MarketplaceSearchRequest req = new MarketplaceSearchRequest();
        req.setTagIds(List.of("t1"));
        req.setPublishedAfter(LocalDateTime.now().minusDays(10));

        // Simulate MongoTemplate returning an empty list
        when(mongoTemplate.find(any(Query.class), eq(MarketplaceEntry.class)))
                .thenReturn(Collections.emptyList());

        // --- Act ---

        List<MarketplaceEntryDto> results = service.search(req);

        // --- Assert ---

        // The Result should be an empty list since no entries matched
        assertTrue(results.isEmpty());
    }
}

