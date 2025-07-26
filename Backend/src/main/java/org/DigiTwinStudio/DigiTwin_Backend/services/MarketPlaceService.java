package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.MarketplaceEntry;
import org.DigiTwinStudio.DigiTwin_Backend.domain.PublishMetadata;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceEntryDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.PublishRequestDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.MarketplaceMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.MarketPlaceEntryRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TagRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

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
}
