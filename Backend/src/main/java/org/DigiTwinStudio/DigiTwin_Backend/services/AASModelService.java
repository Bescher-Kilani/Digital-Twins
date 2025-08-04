package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;

import org.DigiTwinStudio.DigiTwin_Backend.adapter.MultipartFileAdapter;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.PublishRequestDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ConflictException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ForbiddenException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.SubmodelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.MarketPlaceEntryRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.UploadedFileRepository;
import org.DigiTwinStudio.DigiTwin_Backend.validation.AASModelValidator;
import org.DigiTwinStudio.DigiTwin_Backend.validation.FileUploadValidator;

import org.eclipse.digitaltwin.aas4j.v3.model.File;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handles AAS model CRUD, file management, and publishing.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AASModelService {

    private final AASModelRepository aasModelRepository;
    private final AASModelValidator aasModelValidator;
    private final UploadedFileRepository uploadedFileRepository;
    private final FileUploadValidator fileUploadValidator;
    private final AASModelMapper aasModelMapper;
    private final SubmodelMapper submodelMapper;
    private final MarketPlaceService marketPlaceService;
    private final MarketPlaceEntryRepository marketPlaceEntryRepository;

    /**
     * Returns all AAS models for a user.
     *
     * @param userId the user ID
     * @return list of {@link AASModelDto} belonging to the user
     */
    @Transactional(readOnly = true)
    public List<AASModelDto> getAllModelsForUser(String userId) {
        List<AASModel> models = aasModelRepository.findByOwnerId(userId);
        return models.stream().map(aasModelMapper::toDto).toList();
    }

    /**
     * Returns a specific AAS model for a user.
     *
     * @param id model ID
     * @param userId user ID
     * @return the {@link AASModelDto}
     * @throws NotFoundException if the model does not exist or is not owned by the user
     */
    @Transactional(readOnly = true)
    public AASModelDto getModelById(String id, String userId) {
        AASModel model = getModelOrThrow(id, userId);
        return aasModelMapper.toDto(model);
    }

    /**
     * Updates and saves an existing AAS model for a user.
     *
     * @param id model ID
     * @param userId user ID
     * @param aasModelDto updated model data
     * @return updated {@link AASModelDto}
     * @throws NotFoundException if the model does not exist or is not owned by the user
     * @throws BadRequestException if the model is invalid
     */
    public AASModelDto saveModel(String id, String userId, AASModelDto aasModelDto) {
        AASModel existingModel = getModelOrThrow(id, userId);

        existingModel.setAas(aasModelDto.getAas());
        existingModel.setUpdatedAt(LocalDateTime.now());

        validateModelWithFiles(existingModel);
        aasModelValidator.validate(existingModel);

        return aasModelMapper.toDto(aasModelRepository.save(existingModel));
    }

    /**
     * Creates a new empty AAS model for a user.
     *
     * @param userId user ID
     * @return the created {@link AASModelDto}
     */
    public AASModelDto createEmptyModel(String userId) {
        AASModel model = buildEmptyModel(userId);
        validateModelWithFiles(model);
        aasModelValidator.validate(model);

        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    /**
     * Creates a new AAS model for a user with given data.
     *
     * @param userId user ID
     * @param aasModelDto model data
     * @return created {@link AASModelDto}
     * @throws BadRequestException if the data is invalid
     */
    public AASModelDto createModel(String userId, AASModelDto aasModelDto) {
        AASModel model = buildModelFromDto(userId, aasModelDto);

        validateModelWithFiles(model);
        aasModelValidator.validate(model);

        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    /**
     * Permanently deletes a model and all referenced files.
     *
     * @param id model ID
     * @param userId user ID
     * @throws NotFoundException if the model does not exist or is not owned by the user
     * @throws BadRequestException if a file or the model could not be deleted
     */
    public void hardDeleteModel(String id, String userId) {
        AASModel model = getModelOrThrow(id, userId);

        // Delete marketplace entry as well if model is published
        if (model.isPublished()) {
            try {
                marketPlaceEntryRepository.deleteById(model.getId());
            } catch (Exception e) {
                throw new BadRequestException("Failed to delete marketplace entry for model: " + model.getId(), e);
            }
        }

        if (model.getSubmodels() != null && !model.getSubmodels().isEmpty()) {
            for (DefaultSubmodel submodel : model.getSubmodels()) {
                for (String fileId : findFileIdsInSubmodel(submodel)) {
                    try {
                        uploadedFileRepository.deleteById(fileId);
                    } catch (Exception e) {
                        throw new BadRequestException("Failed to delete uploaded file: " + fileId, e);
                    }
                }
            }
        }

        try {
            aasModelRepository.deleteById(model.getId());
        } catch (Exception e) {
            throw new BadRequestException("Failed to delete model: " + model.getId(), e);
        }
    }

    /**
     * Publishes a model in the marketplace if valid and not yet published.
     *
     * @param id model ID
     * @param userId user ID
     * @param request publish metadata
     * @throws ConflictException if the model is already published
     * @throws NotFoundException if the model does not exist or is not owned by the user
     * @throws BadRequestException if the model or referenced files are invalid
     */
    public void publishModel(String id, String userId, PublishRequestDto request) throws ConflictException{
        AASModel model = getModelOrThrow(id, userId);

        if (model.isPublished()) {
            throw new ConflictException("Model is already published.");
        }

        validateModelWithFiles(model);
        aasModelValidator.validate(model);
        this.marketPlaceService.publish(request, model);
    }

    /**
     * Unpublishes a published model if the user is the owner.
     *
     * @param modelId model ID
     * @param userId user ID
     * @throws BadRequestException if the model does not exist
     * @throws ConflictException if the model is not published
     * @throws ForbiddenException if the user is not the owner
     */
    public void unpublishModel(String modelId, String userId) throws BadRequestException, ConflictException, ForbiddenException {
        AASModel model = getModelOrThrow(modelId, userId);
        validateModelWithFiles(model);
        if (!model.isPublished()) {
            throw new ConflictException("Model is not published.");
        }
        this.marketPlaceService.unpublish(userId, model);
    }

    /**
     * Creates a new empty model for a user and copies a published marketplace entry into it.
     *
     * @param entryId marketplace entry ID
     * @param userId user ID
     * @throws BadRequestException if the published model does not exist
     */
    public void addEntryModelToUser(String entryId, String userId)  throws BadRequestException {
        AASModel newModel = buildModelFromDto(userId, this.marketPlaceService.getPublishedModel(entryId));
        // remove publish data
        newModel.setPublished(false);
        LocalDateTime now = LocalDateTime.now();
        newModel.setUpdatedAt(now);
        newModel.setCreatedAt(now);
        newModel.setPublishMetadata(null);

        this.aasModelRepository.save(newModel);
        this.marketPlaceService.incrementDownloadCount(entryId);
    }

    /**
     * Adds a submodel to a model if not already present.
     *
     * @param modelId model ID
     * @param dto submodel data
     * @param userId user ID
     * @return updated {@link AASModelDto}
     * @throws ConflictException if submodel with the same ID exists
     * @throws NotFoundException if the model does not exist or is not owned by the user
     * @throws BadRequestException if the model is invalid
     */
    public AASModelDto attachSubmodel(String modelId, SubmodelDto dto, String userId) {
        AASModel model = getModelOrThrow(modelId, userId);
        DefaultSubmodel submodel = submodelMapper.fromDto(dto);

        // to prevent adding the same submodel (by ID) multiple times to one AAS model (necessary?)
        doesSubmodelExist(model, submodel);

        model.getSubmodels().add(submodel);
        model.setUpdatedAt(LocalDateTime.now());

        aasModelValidator.validate(model);

        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    /**
     * Updates a submodel in a model.
     *
     * @param modelId model ID
     * @param submodelId submodel ID
     * @param dto updated submodel data
     * @param userId user ID
     * @return updated {@link AASModelDto}
     * @throws NotFoundException if the model or submodel does not exist or is not owned by the user
     * @throws BadRequestException if the model is invalid
     */
    public AASModelDto updateSubmodel(String modelId, String submodelId, SubmodelDto dto, String userId) {
        AASModel model = getModelOrThrow(modelId, userId);

        replaceSubmodel(model, submodelId, dto);

        model.setUpdatedAt(LocalDateTime.now());
        aasModelValidator.validate(model);

        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    /**
     * Removes a submodel from a model by its ID.
     *
     * @param id model ID
     * @param submodelId submodel ID
     * @param userId user ID
     * @return updated {@link AASModelDto}
     * @throws NotFoundException if the submodel does not exist in the model
     */
    public AASModelDto removeSubmodel(String id, String submodelId, String userId) {
        AASModel model = getModelOrThrow(id, userId);

        if (model.getSubmodels() == null || model.getSubmodels().isEmpty()) {
            throw new NotFoundException("No submodels present in this model.");
        }

        DefaultSubmodel toRemove = null;
        for (DefaultSubmodel submodel : model.getSubmodels()) {
            if (Objects.equals(submodel.getId(), submodelId)) {
                toRemove = submodel;
                break;
            }
        }

        if (toRemove == null) {
            throw new NotFoundException("Submodel with ID " + submodelId + " not found.");
        }

        for (String fileId : findFileIdsInSubmodel(toRemove)) {
            try {
                uploadedFileRepository.deleteById(fileId);
            } catch (Exception e) {
                throw new BadRequestException("Failed to delete uploaded file: " + fileId, e);
            }
        }

        model.getSubmodels().remove(toRemove);
        model.setUpdatedAt(LocalDateTime.now());
        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    /**
     * Validates that all file references in the submodels of the given model exist and are valid.
     *
     * @param model the AAS model to validate
     * @throws NotFoundException if a referenced file does not exist
     * @throws BadRequestException if a referenced file is invalid
     */
    public void validateModelWithFiles(AASModel model) {
        if (model.getSubmodels() != null && !model.getSubmodels().isEmpty()) {
            for (DefaultSubmodel submodel : model.getSubmodels()) {
                for (String fileId : findFileIdsInSubmodel(submodel)) {
                    validateFileReference(fileId);
                }
            }
        }
    }

    private void validateFileReference(String fileId) {
        UploadedFile file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Referenced file not found: " + fileId));
        MultipartFile multipartFile = new MultipartFileAdapter(file);
        fileUploadValidator.validate(multipartFile);
    }

    private AASModel buildModelFromDto(String userId, AASModelDto dto) {
        LocalDateTime now = LocalDateTime.now();
        return AASModel.builder()
                .ownerId(userId)
                .createdAt(now)
                .updatedAt(now)
                .published(false)
                .aas(dto.getAas())
                .submodels(dto.getSubmodels())
                .build();
    }

    private AASModel buildEmptyModel(String userId) {
        LocalDateTime now = LocalDateTime.now();
        DefaultAssetAdministrationShell shell = new DefaultAssetAdministrationShell();
        shell.setSubmodels(new ArrayList<>());
        return AASModel.builder()
                .ownerId(userId)
                .createdAt(now)
                .updatedAt(now)
                .published(false)
                .submodels(new ArrayList<>())
                .aas(shell)
                .build();
    }

    private List<String> findFileIdsInSubmodel(DefaultSubmodel submodel) {
        List<String> fileIds = new ArrayList<>();
        if (submodel.getSubmodelElements() != null && !submodel.getSubmodelElements().isEmpty()) {
            for (SubmodelElement element : submodel.getSubmodelElements()) {
                if (element instanceof File fileElem) {
                    String fileId = fileElem.getValue();
                    if (fileId != null && !fileId.isBlank()) {
                        fileIds.add(fileId);
                    }
                }
            }
        }
        return fileIds;
    }

    private void validateOwnership(AASModel model, String userId) {
        if (!model.getOwnerId().equals(userId)) {
            throw new ForbiddenException("Access denied: model does not belong to user.");
        }
    }

    private void doesSubmodelExist(AASModel model, DefaultSubmodel submodel) {
        boolean exists = model.getSubmodels().stream()
                .anyMatch(existing -> existing.getId().equals(submodel.getId()));

        if (exists) {
            throw new ConflictException("Submodel with ID " + submodel.getId() + " already exists.");
        }
    }

    private void replaceSubmodel(AASModel model, String submodelId, SubmodelDto dto) {
        DefaultSubmodel updated = submodelMapper.fromDto(dto);
        List<DefaultSubmodel> submodels = model.getSubmodels();

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
    }

    private AASModel getModelOrThrow(String id, String userId) {
        AASModel model = aasModelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Model with ID '" + id + "' not found."));
        validateOwnership(model, userId);
        return model;
    }
}
