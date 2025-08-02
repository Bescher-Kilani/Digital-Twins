package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;

import org.DigiTwinStudio.DigiTwin_Backend.adapter.MultipartFileAdapter;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.PublishMetadata;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
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
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TagRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.UploadedFileRepository;

import org.DigiTwinStudio.DigiTwin_Backend.validation.AASModelValidator;
import org.DigiTwinStudio.DigiTwin_Backend.validation.FileUploadValidator;

import org.eclipse.digitaltwin.aas4j.v3.model.File;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final UploadedFileRepository uploadedFileRepository;
    private final FileUploadValidator fileUploadValidator;
    private final AASModelMapper aasModelMapper;
    private final SubmodelMapper submodelMapper;
    private final MarketPlaceService marketPlaceService;

    /**
     * Retrieves all AAS models belonging to a user that are not marked as deleted.
     *
     * @param userId the unique ID of the user whose models are to be retrieved
     * @return a list of {@link AASModelDto} for the user
     */
    @Transactional(readOnly = true)
    public List<AASModelDto> getAllForUser(String userId) {
        List<AASModel> models = aasModelRepository.findByOwnerIdAndDeletedFalse(userId);
        return models.stream().map(aasModelMapper::toDto).toList();
    }

    /**
     * Retrieves an AAS model by its ID for a specific user.
     *
     * @param id the ID of the model
     * @param userId the ID of the user requesting the model
     * @return the {@link AASModelDto} corresponding to the given ID
     * @throws NotFoundException if the model does not exist or does not belong to the user
     */
    @Transactional(readOnly = true)
    public AASModelDto getById(String id, String userId) {
        AASModel model = getModelOrThrow(id, userId);
        return aasModelMapper.toDto(model);
    }

    /**
     * Creates a new, empty AAS model for the specified user.
     * The created model contains no submodels and is initialized with default metadata.
     *
     * @param userId the ID of the user creating the model
     * @return the newly created {@link AASModelDto}
     */
    public AASModelDto createEmpty(String userId) {
        LocalDateTime now = LocalDateTime.now();

        DefaultAssetAdministrationShell shell = new DefaultAssetAdministrationShell();
        shell.setSubmodels(new ArrayList<>());

        AASModel model = AASModel.builder()
                .ownerId(userId)
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .published(false)
                .aas(shell)
                .build();
        validateReferencedFiles(model);
        aasModelValidator.validate(model);

        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    /**
     * Updates and saves an existing AAS model for a user.
     * Overwrites the current model's data with the values from the provided DTO.
     *
     * @param id the ID of the model to be updated
     * @param userId the ID of the user performing the update
     * @param aasModelDto the updated model data
     * @return the updated {@link AASModelDto}
     * @throws NotFoundException if the model does not exist or does not belong to the user
     * @throws BadRequestException if the updated model is invalid
     */
    public AASModelDto saveModel(String id, String userId, AASModelDto aasModelDto) {
        AASModel existingModel = getModelOrThrow(id, userId);

        existingModel.setAas(aasModelDto.getAas());
        existingModel.setUpdatedAt(LocalDateTime.now());

        validateReferencedFiles(existingModel);
        aasModelValidator.validate(existingModel);

        return aasModelMapper.toDto(aasModelRepository.save(existingModel));
    }

    /**
     * Creates a new AAS model for a user using the provided model data.
     *
     * @param userId the ID of the user creating the model
     * @param aasModelDto the data for the new model
     * @return the newly created {@link AASModelDto}
     * @throws BadRequestException if the provided model data is invalid
     */
    public AASModelDto createModel(String userId, AASModelDto aasModelDto) {
        LocalDateTime now = LocalDateTime.now();

        AASModel model = AASModel.builder()
                .ownerId(userId)
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .published(false)
                .aas(aasModelDto.getAas())
                .submodels(aasModelDto.getSubmodels())
                .build();

        validateReferencedFiles(model);
        aasModelValidator.validate(model);

        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    /**
     * Soft-deletes a model (marks as deleted) for the specified user.
     *
     * @param id the ID of the model to delete
     * @param userId the ID of the user performing the deletion
     * @throws NotFoundException if the model does not exist or does not belong to the user
     */
    public void deleteModel(String id, String userId) {
        AASModel model = getModelOrThrow(id, userId);

        model.setDeleted(true);
        model.setUpdatedAt(LocalDateTime.now());
        aasModelRepository.save(model);
    }

    /**
     * Publishes an AAS model in the marketplace if it is valid and not already published.
     * The method validates referenced files and overall model structure before publishing.
     *
     * @param id the ID of the model to publish
     * @param userId the ID of the user publishing the model
     * @param request additional metadata for publishing (tags, description, etc.)
     * @throws ConflictException if the model is already published
     * @throws NotFoundException if the model does not exist or does not belong to the user
     * @throws BadRequestException if the model or its referenced files are invalid
     */
    public void publishModel(String id, String userId, PublishRequestDto request) throws ConflictException{
        AASModel model = getModelOrThrow(id, userId);

        if (model.isPublished()) {
            throw new ConflictException("Model is already published.");
        }

        validateReferencedFiles(model);
        aasModelValidator.validate(model);
        this.marketPlaceService.publish(request, model);
    }

    /**
     * Unpublishes the AAS model identified by the given ID if it exists,
     * is currently published, and the requesting user is the owner.
     *
     * <p>This method performs the following validations:
     * <ul>
     *     <li>Retrieves the model using {@code getModelOrThrow} which may throw {@link BadRequestException}.</li>
     *     <li>Checks if the requesting user is the owner of the model; throws {@link ForbiddenException} if not.</li>
     *     <li>Verifies the model is currently published; throws {@link ConflictException} otherwise.</li>
     * </ul>
     *
     * <p>If all validations pass, it delegates the unpublishing operation to {@code marketPlaceService.unpublish}.
     *
     * @param modelId the ID of the model to unpublish
     * @param userId the ID of the user requesting to unpublish the model
     * @throws BadRequestException if the model does not exist or cannot be retrieved
     * @throws ConflictException if the model is not published
     * @throws ForbiddenException if the user is not the owner of the model
     */
    public void unpublishModel(String modelId, String userId) throws BadRequestException, ConflictException, ForbiddenException {
        AASModel model = getModelOrThrow(modelId, userId);
        if (!model.getOwnerId().equals(userId)) {
            throw new ForbiddenException("User is not owner of this model.");
        }
        if (!model.isPublished()) {
            throw new ConflictException("Model is not published.");
        }
        this.marketPlaceService.unpublish(userId, model);
    }

    /**
     * Creates a new empty AAS model for the specified user and attaches the published model
     * from the given marketplace entry to it.
     *
     * <p>This method performs the following actions:
     * <ul>
     *     <li>Creates a new empty AAS model for the given {@code userId}.</li>
     *     <li>Retrieves the published model associated with the specified {@code entryId}.</li>
     *     <li>Saves the published model into the newly created user model.</li>
     * </ul>
     *
     * @param entryId the ID of the marketplace entry containing the published model to be copied
     * @param userId the ID of the user to whom the model should be added
     * @throws BadRequestException if the published model for the given entry ID does not exist
     */
    public void addEntryModelToUser(String entryId, String userId)  throws BadRequestException {
        String newModelId = this.createEmpty(userId).getId();
        this.saveModel(newModelId, userId, this.marketPlaceService.getPublishedModel(entryId));
    }

    /**
     * Attaches a submodel to an existing AAS model if it does not already exist.
     * Validates the updated model before saving.
     *
     * @param modelId the ID of the target model
     * @param dto the submodel data to attach
     * @param userId the ID of the user performing the operation
     * @return the updated {@link AASModelDto}
     * @throws ConflictException if a submodel with the same ID already exists in the model
     * @throws NotFoundException if the model does not exist or does not belong to the user
     * @throws BadRequestException if the updated model is invalid
     */
    public AASModelDto attachSubmodel(String modelId, SubmodelDto dto, String userId) {
        AASModel model = getModelOrThrow(modelId, userId);
        DefaultSubmodel submodel = submodelMapper.fromDto(dto);

        // to prevent adding the same submodel (by ID) multiple times to one AAS model (necessary?)
        boolean exists = model.getSubmodels().stream()
                .anyMatch(existing -> existing.getId().equals(submodel.getId()));

        if (exists) {
            throw new ConflictException("Submodel with ID " + submodel.getId() + " already exists.");
        }

        model.getSubmodels().add(submodel);
        model.setUpdatedAt(LocalDateTime.now());

        aasModelValidator.validate(model);

        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    /**
     * Updates an existing submodel within a model, if it exists.
     * Validates the entire model after the update.
     *
     * @param modelId the ID of the model
     * @param submodelId the ID of the submodel to update
     * @param dto the updated submodel data
     * @param userId the ID of the user performing the update
     * @return the updated {@link AASModelDto}
     * @throws NotFoundException if the model or submodel does not exist or does not belong to the user
     * @throws BadRequestException if the updated model is invalid
     */
    public AASModelDto updateSubmodel(String modelId, String submodelId, SubmodelDto dto, String userId) {
        AASModel model = getModelOrThrow(modelId, userId);
        List<DefaultSubmodel> submodels = model.getSubmodels();
        DefaultSubmodel updated = submodelMapper.fromDto(dto);

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

    /**
     * Removes a submodel from an AAS model by its ID.
     *
     * @param id the ID of the model
     * @param submodelId the ID of the submodel to remove
     * @param userId the ID of the user performing the removal
     * @return the updated {@link AASModelDto}
     * @throws NotFoundException if the submodel does not exist in the model
     */
    public AASModelDto removeSubmodel(String id, String submodelId, String userId) {
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

    /**
     * Validates that all files referenced by submodels in this model exist and are valid uploads.
     * Throws a NotFoundException or ValidationException if a file is missing or invalid.
     *
     * @param model the AAS model whose referenced files are to be validated
     * @throws NotFoundException if a referenced file does not exist
     * @throws BadRequestException if a referenced file is invalid
     */
    public void validateReferencedFiles(AASModel model) {

        List<DefaultSubmodel> submodels = model.getSubmodels();

        if (submodels != null && !submodels.isEmpty()) {
            for (DefaultSubmodel submodel : submodels) {
                List<SubmodelElement> elements = submodel.getSubmodelElements();

                for (SubmodelElement element : elements) {
                    if (element instanceof File fileElement) {
                        String fileId = fileElement.getValue();

                        if (fileId == null || fileId.isBlank()) {
                            continue;
                        }

                        UploadedFile file = uploadedFileRepository.findById(fileId)
                                .orElseThrow(() -> new NotFoundException("Referenced file not found: " + fileId));

                        MultipartFile multipartFile = new MultipartFileAdapter(file);
                        fileUploadValidator.validate(multipartFile);
                    }
                }
            }
        }
    }

    private void validateOwnership(AASModel model, String userId) {
        if (!model.getOwnerId().equals(userId)) {
            throw new ForbiddenException("Access denied: model does not belong to user.");
        }
    }

    private AASModel getModelOrThrow(String id, String userId) {
        AASModel model = aasModelRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Model with ID '" + id + "' not found."));
        validateOwnership(model, userId);
        return model;
    }
}
