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
     * Unpublishes the specified AAS model if it exists, is currently published,
     * and the requesting user is the owner of the model.
     *
     * <p>This method performs the following checks:
     * <ul>
     *     <li>Verifies that the model with the given ID exists.</li>
     *     <li>Checks if the user is the owner of the model; throws {@link ForbiddenException} otherwise.</li>
     *     <li>Ensures the model is currently published; throws {@link BadRequestException} if not.</li>
     * </ul>
     *
     * <p>If all validations pass, the method:
     * <ul>
     *     <li>Marks the model as unpublished.</li>
     *     <li>Clears the publish metadata.</li>
     *     <li>Updates the modification timestamp.</li>
     *     <li>Saves the updated model back to the repository.</li>
     * </ul>
     *
     * @param userId the ID of the user attempting to unpublish the model
     * @param modelId the ID of the model to be unpublished
     * @throws BadRequestException if the model does not exist or is not published
     * @throws ForbiddenException if the user is not the owner of the model
     */
    public void unpublish(String userId, String modelId) throws BadRequestException, ForbiddenException {
        AASModel model = aasModelRepository.findById(modelId)
                .orElseThrow(() -> new BadRequestException("Model not found with id: " + modelId));
        if (!model.getOwnerId().equals(userId)) {
            throw new ForbiddenException("User is not owner of this model.");
        }
        if (!model.isPublished()) {
            throw new BadRequestException("Model is not currently published.");
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
     * Retrieves the published AAS model associated with the given marketplace entry DTO.
     *
     * <p>This method performs the following actions:
     * <ul>
     *     <li>Extracts the model ID from the given {@link MarketplaceEntryDto}.</li>
     *     <li>Fetches the corresponding {@link AASModel} from the repository.</li>
     *     <li>Throws a {@link BadRequestException} if no model is found with the given ID.</li>
     *     <li>Maps the found model to a {@link AASModelDto} using {@code aasModelMapper}.</li>
     * </ul>
     *
     * @param dto the {@link MarketplaceEntryDto} containing the ID of the model to retrieve
     * @return the corresponding {@link AASModelDto}
     * @throws BadRequestException if no model is found with the given ID
     */
    public AASModelDto getPublishedModel(MarketplaceEntryDto dto) throws BadRequestException {
        return this.aasModelMapper.toDto(this.aasModelRepository.findById(dto.getId()).orElseThrow(() -> new BadRequestException("No entry found for id: " + dto.getId())));
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
