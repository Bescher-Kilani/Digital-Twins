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
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
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

/**
 * Handles publishing, searching, and managing marketplace entries and tags.
 */
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
     * Publishes a model: sets publish metadata, updates tags, and saves the model as published.
     * Creates MarketplaceEntry
     * @param request publish info including author, description, and tag IDs
     * @param model the model to publish
     * @throws BadRequestException if any tag ID is invalid
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

        // create public marketPlaceEntry
        this.marketPlaceEntryRepository.save(
                MarketplaceEntry.builder()
                .publishedAt(now)
                .author(request.getAuthor())
                .id(model.getId())
                .name(model.getAas().getIdShort())
                .downloadCount(0)
                .shortDescription(request.getShortDescription())
                .tagIds(request.getTagIds())
                .build()
                );
    }

    /**
     * Unpublishes a model, resets publish metadata, and updates tag usage counts.
     * Deletes MarketplaceEntry
     *
     * @param userId user performing the operation
     * @param model the model to unpublish
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

        //remove marketPlaceEntry
        MarketplaceEntry marketplaceEntry = marketPlaceEntryRepository.findById(model.getId()).orElseThrow(() -> new NotFoundException("Marketplace entry with id " + model.getId() + " not found."));
        this.marketPlaceEntryRepository.delete(marketplaceEntry);
    }

    /**
     * Returns all marketplace entries as DTOs.
     *
     * @return all marketplace entries
     */
    public List<MarketplaceEntryDto> listAllEntries() {
        return this.marketPlaceEntryRepository.findAll().stream().map(this.marketplaceMapper::toDto).toList();
    }

    /**
     * Returns the published model associated with a marketplace entry.
     *
     * @param entryId marketplace entry ID
     * @return published model as DTO
     * @throws BadRequestException if no model exists for the given ID
     */
    public AASModelDto getPublishedModel(String entryId) throws BadRequestException {
        return this.aasModelMapper.toDto(this.aasModelRepository.findById(entryId).orElseThrow(() -> new BadRequestException("No entry found for id: " + entryId)));
    }

    /**
     * Validates that all tag IDs exist.
     *
     * @param requestedTagIds tag IDs to check
     * @throws BadRequestException if any tag ID is invalid or none are provided
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
     * Returns all tags in the system.
     *
     * @return all tags
     */
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    /**
     * Increments the download count for a marketplace entry.
     *
     * @param entryId marketplace entry ID
     * @throws BadRequestException if the entry does not exist
     */
    public void incrementDownloadCount(String entryId) throws BadRequestException {
        MarketplaceEntry marketplaceEntry = this.marketPlaceEntryRepository.findById(entryId).orElseThrow(() -> new BadRequestException("No entry found for id: " + entryId));
        marketplaceEntry.setDownloadCount(marketplaceEntry.getDownloadCount() + 1);
        this.marketPlaceEntryRepository.save(marketplaceEntry);
    }

    /**
     * Searches marketplace entries by text, tags, and/or date.
     *
     * <p>
     * If {@code searchText} is provided, uses full-text search and sorts by relevance;
     * otherwise filters by published date and tags and sorts by date (newest first).
     * </p>
     *
     * @param req search parameters
     * @return list of matching marketplace entries
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
