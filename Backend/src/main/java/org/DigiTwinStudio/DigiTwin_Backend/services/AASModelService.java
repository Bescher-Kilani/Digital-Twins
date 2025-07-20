package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.PublishRequestDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ValidationException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.SubmodelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TagRepository;
import org.DigiTwinStudio.DigiTwin_Backend.validation.AASModelValidator;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class AASModelService {

    private final AASModelRepository aasModelRepository;
    private final AASModelValidator aasModelValidator;
    private final TagRepository tagRepository;
    private final AASModelMapper aasModelMapper;
    private final SubmodelMapper submodelMapper;

    @Transactional(readOnly = true)
    public List<AASModelDto> getAllForUser(String userId) {
        List<AASModel> models = aasModelRepository.findByOwnerIdAndDeletedFalse(userId);
        return models.stream().map(aasModelMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AASModelDto getById(String id, String userId) {
        //changed because loading all the users data from database and filtering it in the application code
        // can expose the user data, that is why I added findByIdAndOwnerIdAndDeletedFalse method to AASModelRepository
        AASModel model = getModelOrThrow(id, userId);
        return aasModelMapper.toDto(model);
    }

    public AASModelDto createEmpty(String userId) {
        LocalDateTime now = LocalDateTime.now();

        AssetAdministrationShell shell = new DefaultAssetAdministrationShell();
        shell.setSubmodels(new ArrayList<>());

        AASModel model = AASModel.builder()
                .ownerId(userId)
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .published(false)
                .aas(shell)
                .build();

        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    // TODO: add uploaded file validation
    public AASModelDto saveModel(String id, String userId, AASModelDto aasModelDto) {
        AASModel existingModel = getModelOrThrow(id, userId);

        existingModel.setAas(aasModelDto.getAas());
        existingModel.setUpdatedAt(LocalDateTime.now());

        aasModelValidator.validate(existingModel);

        return aasModelMapper.toDto(aasModelRepository.save(existingModel));

        /*
        AASModel updatedModel = aasModelMapper.fromDto(aasModelDto, userId);

        aasModelValidator.validate(updatedModel);

        updatedModel.setId(id);
        updatedModel.setOwnerId(userId);
        updatedModel.setCreatedAt(existingModel.getCreatedAt());
        updatedModel.setUpdatedAt(LocalDateTime.now());
        updatedModel.setDeleted(existingModel.isDeleted());
        updatedModel.setPublished(existingModel.isPublished());
        updatedModel.setPublishMetadata(existingModel.getPublishMetadata());

        AASModel savedModel = aasModelRepository.save(updatedModel);

        return aasModelMapper.toDto(savedModel);
         */
    }

    public void deleteModel(String id, String userId) {
        AASModel model = getModelOrThrow(id, userId);

        model.setDeleted(true);
        model.setUpdatedAt(LocalDateTime.now());
        aasModelRepository.save(model);
    }

    // TODO: add uploaded file validation
    public AASModelDto publishModel(String id, String userId, PublishRequestDto request) {
        AASModel model = getModelOrThrow(id, userId);

        if (model.isPublished()) {
            throw new ValidationException("Model is already published.");
        }

        validateTagIds(request.getTagIds());

        /*
        // Current check only tells if some tags are invalid, not which ones
        if (!tagRepository.findByIdIn(request.getTagIds()).stream().map(tag -> tag.getId()).toList().containsAll(request.getTagIds())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more tags are invalid");
        }
         */

        model.setPublished(true);
        model.setUpdatedAt(LocalDateTime.now());

        aasModelValidator.validate(model);

        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    public AASModelDto attachSubmodel(String id, SubmodelDto dto, String userId) throws ValidationException {
        AASModel model = getModelOrThrow(id, userId);

        Submodel submodel = submodelMapper.fromDto(dto);

        boolean exists = model.getSubmodels().stream()
                .anyMatch(existing -> existing.getId().equals(submodel.getId()));

        if (exists) {
            throw new ValidationException("Submodel with ID " + submodel.getId() + " already exists.");
        }

        model.getSubmodels().add(submodel);
        model.setUpdatedAt(LocalDateTime.now());

        aasModelValidator.validate(model);

        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    public AASModelDto updateSubmodel(String id, String submodelId, SubmodelDto dto, String userId) throws ValidationException {
        AASModel model = getModelOrThrow(id, userId);

        List<Submodel> submodels = model.getSubmodels();
        Submodel updated = submodelMapper.fromDto(dto);

        boolean replaced = false;
        for (int i = 0; i < submodels.size(); i++) {
            if (Objects.equals(submodels.get(i).getId(), submodelId)) {
                submodels.set(i, updated);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            throw new NotFoundException("Submodel with ID " + submodelId + " not found.");
        }

        model.setUpdatedAt(LocalDateTime.now());
        aasModelValidator.validate(model);

        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    public AASModelDto removeSubmodel(String id, String submodelId, String userId) throws ValidationException {
        AASModel model = getModelOrThrow(id, userId);

        boolean removed = model.getSubmodels().removeIf(submodel ->
                Objects.equals(submodel.getId(), submodelId)
        );

        if (!removed) {
            throw new NotFoundException("Submodel with ID " + submodelId + " not found.");
        }

        model.setUpdatedAt(LocalDateTime.now());

        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    // TODO: check at the end if this method should be public
    public void validateOwnership(AASModel model, String userId) {
        if (!model.getOwnerId().equals(userId)) {
            throw new ValidationException("Access denied: model does not belong to user.");
        }
    }

    private AASModel getModelOrThrow(String id, String userId) {
        AASModel model = aasModelRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Model not found."));
        validateOwnership(model, userId);
        return model;
    }

    // TODO: ask about tag requirements (if they are required or can be more than one)
    private void validateTagIds(List<String> requestedTagIds) {
        if (requestedTagIds == null || requestedTagIds.isEmpty()) {
            throw new ValidationException("At least one tag must be provided to publish a model.");
        }

        List<String> existingTagIds = tagRepository.findByIdIn(requestedTagIds)
                .stream()
                .map(Tag::getId)
                .toList();

        List<String> invalidTagIds = requestedTagIds.stream()
                .filter(tagId -> !existingTagIds.contains(tagId))
                .toList();

        if (!invalidTagIds.isEmpty()) {
            throw new ValidationException("Invalid tag IDs: " + String.join(", ", invalidTagIds));
        }
    }
}
