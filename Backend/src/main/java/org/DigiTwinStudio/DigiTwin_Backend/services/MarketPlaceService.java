package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.MarketplaceEntry;
import org.DigiTwinStudio.DigiTwin_Backend.domain.PublishMetadata;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceEntryDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.PublishRequestDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ForbiddenException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.MarketplaceMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.MarketPlaceEntryRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TagRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketPlaceService {
    private final MarketPlaceEntryRepository marketPlaceEntryRepository;
    private final AASModelRepository aasModelRepository;
    private final TagRepository tagRepository;
    private final MarketplaceMapper marketplaceMapper;
    private final AASModelMapper aasModelMapper;

    /**
     * Publishes the given {@link AASModel} by setting its publication metadata and updating its state.
     *
     * <p>This method performs the following actions:
     * <ul>
     *     <li>Validates the list of tag IDs provided in the {@link PublishRequestDto}.</li>
     *     <li>Builds a {@link PublishMetadata} object using the request data and current timestamp.</li>
     *     <li>Updates the model's publish metadata, sets its published flag to {@code true}, and updates the timestamp.</li>
     *     <li>Persists the updated model using {@code aasModelRepository}.</li>
     * </ul>
     *
     * @param request the DTO containing the data required to publish the model, including author, short description, and tag IDs.
     * @param model the {@link AASModel} instance to be published.
     * @throws BadRequestException if the provided tag IDs are invalid.
     */
    public void publish(PublishRequestDto request, AASModel model) throws BadRequestException {

        validateTagIds(request.getTagIds());
        LocalDateTime now = LocalDateTime.now();
        PublishMetadata metadata = PublishMetadata.builder()
                .publishedAt(now)
                .author(request.getAuthor())
                .shortDescription(request.getShortDescription())
                .tagIds(request.getTagIds())
                .build();
        model.setPublishMetadata(metadata);
        model.setPublished(true);
        model.setUpdatedAt(now);
        aasModelRepository.save(model);
    }

    /**
     * Unpublishes the given AAS model by marking it as unpublished,
     * clearing its publish metadata, updating the timestamp, and saving it.
     *
     * <p>This method assumes that any necessary validation (e.g., user ownership,
     * model published status) has already been performed before calling.
     *
     * @param userId the ID of the user requesting the unpublish operation (not used directly here)
     * @param model the {@link AASModel} to unpublish and update
     */
    public void unpublish(String userId, AASModel model) {
        if (!Objects.equals(userId, model.getOwnerId())) {
            throw new ForbiddenException("Current user does not have permission to unpublish model.");
        }
        model.setPublished(false);
        model.setPublishMetadata(null);
        model.setUpdatedAt(LocalDateTime.now());
        aasModelRepository.save(model);
    }

    /**
     * Retrieves all marketplace entries from the repository and converts them to DTOs.
     *
     * <p>This method performs the following actions:
     * <ul>
     *     <li>Fetches all {@link MarketplaceEntry} entities from the {@code marketPlaceEntryRepository}.</li>
     *     <li>Maps each entry to a {@link MarketplaceEntryDto} using the {@code marketplaceMapper}.</li>
     *     <li>Returns the complete list of DTOs.</li>
     * </ul>
     *
     * @return a list of all {@link MarketplaceEntryDto} objects in the system
     */
    public List<MarketplaceEntryDto> listAllEntries() {
        return this.marketPlaceEntryRepository.findAll().stream().map(this.marketplaceMapper::toDto).toList();
    }

    /**
     * Retrieves the published AAS model associated with the given marketplace entry ID.
     *
     * <p>This method performs the following actions:
     * <ul>
     *     <li>Uses the provided {@code entryId} to look up the corresponding {@link AASModel}.</li>
     *     <li>Throws a {@link BadRequestException} if no model is found with the given ID.</li>
     *     <li>Maps the found model to a {@link AASModelDto} using {@code aasModelMapper}.</li>
     * </ul>
     *
     * @param entryId the ID of the marketplace entry whose model should be retrieved
     * @return the corresponding {@link AASModelDto}
     * @throws BadRequestException if no model is found with the given ID
     */
    public AASModelDto getPublishedModel(String entryId) throws BadRequestException {
        return this.aasModelMapper.toDto(this.aasModelRepository.findById(entryId).orElseThrow(() -> new BadRequestException("No entry found for id: " + entryId)));
    }

    /**
     * Searches for marketplace entries that contain all the specified tag IDs.
     *
     * <p>This method performs the following actions:
     * <ul>
     *     <li>Validates that the provided tag IDs are non-null, non-empty, and exist in the system.</li>
     *     <li>Fetches all marketplace entries from the repository.</li>
     *     <li>Filters entries whose own tag list includes all the provided tag IDs.</li>
     *     <li>Maps the filtered entries to {@link MarketplaceEntryDto} objects and returns the result.</li>
     * </ul>
     *
     * @param tagIds the list of tag IDs to filter marketplace entries by
     * @return a list of {@link MarketplaceEntryDto} that contain all specified tag IDs
     * @throws BadRequestException if the tag list is null, empty, or contains invalid tag IDs
     */
    public List<MarketplaceEntryDto> searchByTags(List<String> tagIds) throws BadRequestException {
        validateTagIds(tagIds);

        // Find all marketplaceEntries that contain all provided tag IDs
        List<MarketplaceEntry> matchingEntries = this.marketPlaceEntryRepository.findAll().stream()
                .filter(entry -> new HashSet<>(entry.getTagIds()).containsAll(tagIds))
                .toList();
        return matchingEntries.stream().map(this.marketplaceMapper::toDto).toList();
    }

    /**
     * Validates the list of tag IDs to ensure they exist in the system.
     *
     * <p>This method performs the following checks:
     * <ul>
     *     <li>Throws a {@link BadRequestException} if the provided list is {@code null} or empty.</li>
     *     <li>Checks the existence of each tag ID against the database via {@code tagRepository}.</li>
     *     <li>Throws a {@link BadRequestException} if any of the provided tag IDs are not found.</li>
     * </ul>
     *
     * @param requestedTagIds the list of tag IDs to validate.
     * @throws BadRequestException if no tags are provided or if any tag ID is invalid.
     */
    private void validateTagIds(List<String> requestedTagIds) {
        if (requestedTagIds == null || requestedTagIds.isEmpty()) {
            throw new BadRequestException("At least one tag must be provided to publish a model.");
        }

        List<String> existingTagIds = tagRepository.findByIdIn(requestedTagIds)
                .stream()
                .map(Tag::getId)
                .toList();

        List<String> invalidTagIds = requestedTagIds.stream()
                .filter(tagId -> !existingTagIds.contains(tagId))
                .toList();

        if (!invalidTagIds.isEmpty()) {
            throw new BadRequestException("Invalid tag IDs: " + String.join(", ", invalidTagIds));
        }
    }

    /**
     * Searches for marketplace entries that have at least one tag belonging to the specified category.
     *
     * <p>This method performs the following steps:
     * <ul>
     *     <li>Validates that the provided category is not null or empty.</li>
     *     <li>Retrieves all tags associated with the given category (case-insensitive).</li>
     *     <li>If no tags are found for the category, returns an empty list.</li>
     *     <li>Finds all marketplace entries that contain at least one tag from the retrieved tags.</li>
     *     <li>Maps the filtered marketplace entries to {@link MarketplaceEntryDto} objects and returns the result.</li>
     * </ul>
     *
     * @param category the category name to filter marketplace entries by (case-insensitive)
     * @return a list of {@link MarketplaceEntryDto} objects whose tags belong to the specified category
     * @throws BadRequestException if the category is null or empty
     */
    public List<MarketplaceEntryDto> searchByCategory(String category) throws BadRequestException {
        if (category == null || category.isBlank()) {
            throw new BadRequestException("Category must not be null or empty");
        }

        // Find all tags for the given category (case-insensitive)
        List<Tag> tagsInCategory = tagRepository.findByCategoryIgnoreCase(category);

        if (tagsInCategory.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> tagIdsInCategory = tagsInCategory.stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());

        // Filter entries that contain at least one tag from this category
        List<MarketplaceEntry> matchingEntries = marketPlaceEntryRepository.findAll().stream()
                .filter(entry -> entry.getTagIds().stream().anyMatch(tagIdsInCategory::contains))
                .toList();

        return matchingEntries.stream()
                .map(marketplaceMapper::toDto)
                .toList();
    }

    /**
     * Retrieves all tags from the database.
     *
     * @return a list of all {@link Tag} entities currently stored
     */
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

}
