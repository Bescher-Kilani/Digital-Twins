package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.MarketplaceEntry;
import org.DigiTwinStudio.DigiTwin_Backend.domain.PublishMetadata;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceEntryDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceSearchRequest;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.PublishRequestDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ForbiddenException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.MarketplaceMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.MarketPlaceEntryRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TagRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
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
    private final MongoTemplate mongoTemplate;

    /**
     * Publishes the given {@link AASModel} by setting its publication metadata, updating its state,
     * and incrementing the usage count of associated tags.
     *
     * <p>This method performs the following actions:
     * <ul>
     *     <li>Validates the list of tag IDs provided in the {@link PublishRequestDto}.</li>
     *     <li>Builds a {@link PublishMetadata} object using the request data and current timestamp.</li>
     *     <li>Updates the model's publish metadata, sets its published flag to {@code true}, and updates the timestamp.</li>
     *     <li>Persists the updated model using {@code aasModelRepository}.</li>
     *     <li>Retrieves all tags associated with the model and increments their {@code usageCount} by 1.</li>
     *     <li>Persists the updated tag usage counts using {@code tagRepository}.</li>
     * </ul>
     *
     * @param request the DTO containing the data required to publish the model, including author, short description, and tag IDs
     * @param model the {@link AASModel} instance to be published
     * @throws BadRequestException if the provided tag IDs are invalid
     */
    public void publish(PublishRequestDto request, AASModel model) throws BadRequestException {
        // update Metadata
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

        // save model changes in the database
        aasModelRepository.save(model);

        //update tag counters
        List<Tag> tagsToUpdate = tagRepository.findByIdIn(request.getTagIds());

        tagsToUpdate.forEach(tag -> {
            tag.setUsageCount(tag.getUsageCount() + 1);
        });
        tagRepository.saveAll(tagsToUpdate);

    }

    /**
     * Unpublishes the given AAS model by marking it as unpublished,
     * clearing its publish metadata, updating the timestamp, and saving it.
     * Also decrements the usage count of all associated tags.
     *
     * <p>This method assumes that any necessary validation (e.g., user ownership,
     * model published status) has already been performed before calling.
     *
     * @param userId the ID of the user requesting the unpublish operation
     * @param model the {@link AASModel} to unpublish and update
     * @throws ForbiddenException if the user does not own the model
     */
    public void unpublish(String userId, AASModel model) {
        if (!Objects.equals(userId, model.getOwnerId())) {
            throw new ForbiddenException("Current user does not have permission to unpublish model.");
        }

        // Decrement usage count for each tag (if any)
        if (model.getPublishMetadata() != null && model.getPublishMetadata().getTagIds() != null) {
            List<String> tagIds = model.getPublishMetadata().getTagIds();
            List<Tag> tagsToUpdate = tagRepository.findByIdIn(tagIds);

            tagsToUpdate.forEach(tag -> {
                int currentCount = tag.getUsageCount();
                tag.setUsageCount(Math.max(currentCount - 1, 0)); // Avoid negative values
            });

            tagRepository.saveAll(tagsToUpdate);
        }

        // Unpublish model
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
     * Retrieves all tags from the database.
     *
     * @return a list of all {@link Tag} entities currently stored
     */
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    /**
     * Increments the download count for the marketplace entry with the given ID and persists the change.
     *
     * <p>If no entry exists for the provided {@code entryId}, a {@link BadRequestException} is thrown.</p>
     *
     * <p><b>Note:</b> This implementation performs a read-modify-write cycle; under high concurrency it may suffer
     * from lost updates unless the underlying persistence layer provides appropriate locking or versioning
     * (e.g., optimistic locking or an atomic increment at the database level).</p>
     *
     * @param entryId the identifier of the marketplace entry whose download count should be incremented
     * @throws BadRequestException if no entry exists for the provided {@code entryId}
     */
    public void incrementDownloadCount(String entryId) throws BadRequestException {
        MarketplaceEntry marketplaceEntry = this.marketPlaceEntryRepository.findById(entryId).orElseThrow(() -> new BadRequestException("No entry found for id: " + entryId));
        marketplaceEntry.setDownloadCount(marketplaceEntry.getDownloadCount() + 1);
        this.marketPlaceEntryRepository.save(marketplaceEntry);
    }

    /**
     * Searches marketplace entries with optional full-text, date, and tag filters.
     * <p>
     * If {@code searchText} is set, performs a MongoDB $text search (requires a text index)
     * and sorts by relevance score. Without text, filters by {@code publishedAfter} and/or {@code tagIds}
     * and sorts by {@code publishedAt} (newest first).
     * </p>
     *
     * Examples:
     * <ul>
     *   <li>Text only: searchText = "spring"</li>
     *   <li>Date only: publishedAfter = 2024-01-01T00:00:00</li>
     *   <li>Tags only: tagIds = ["java", "backend"]</li>
     *   <li>Combined: searchText = "spring", publishedAfter = ..., tagIds = [...]</li>
     * </ul>
     *
     * @param req search parameters
     * @return matching marketplace entry DTOs
     */
    public List<MarketplaceEntryDto> search(MarketplaceSearchRequest req) {
        boolean hasText = req.getSearchText() != null && !req.getSearchText().isBlank();
        boolean hasTags = req.getTagIds() != null && !req.getTagIds().isEmpty();
        boolean hasDate = req.getPublishedAfter() != null;

        if (hasText) { // textQuery
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching(req.getSearchText());
            TextQuery textQuery = TextQuery.queryText(textCriteria).sortByScore(); // sort by relevance

            if (hasDate) {
                textQuery.addCriteria(Criteria.where("publishedAt").gt(req.getPublishedAfter())); // add a time query
            }
            if (hasTags) {
                textQuery.addCriteria(Criteria.where("tagIds").in(req.getTagIds())); // add a tag query
            }

            return mongoTemplate.find(textQuery, MarketplaceEntry.class).stream().map(marketplaceMapper::toDto).toList();
        } else {    // no text, but time and tag queries
            Query query = new Query();

            if (hasTags) {
                query.addCriteria(Criteria.where("publishedAt").gt(req.getPublishedAfter()));
            }
            if (hasTags) {
                query.addCriteria(Criteria.where("tagIds").in(req.getTagIds()));
            }

            query.with(Sort.by(Sort.Order.desc("publishedAt"))); // sort by date, newest first
            return mongoTemplate.find(query, MarketplaceEntry.class).stream().map(marketplaceMapper::toDto).toList();
        }
    }



}
