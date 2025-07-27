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

    @Transactional(readOnly = true)
    public List<AASModelDto> getAllForUser(String userId) {
        List<AASModel> models = aasModelRepository.findByOwnerIdAndDeletedFalse(userId);
        return models.stream().map(aasModelMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AASModelDto getById(String id, String userId) {
        AASModel model = getModelOrThrow(id, userId);
        return aasModelMapper.toDto(model);
    }

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

        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    public AASModelDto saveModel(String id, String userId, AASModelDto aasModelDto) {
        AASModel existingModel = getModelOrThrow(id, userId);

        existingModel.setAas(aasModelDto.getAas());
        existingModel.setUpdatedAt(LocalDateTime.now());

        validateReferencedFiles(existingModel);
        aasModelValidator.validate(existingModel);

        return aasModelMapper.toDto(aasModelRepository.save(existingModel));
    }

    public void deleteModel(String id, String userId) {
        AASModel model = getModelOrThrow(id, userId);

        model.setDeleted(true);
        model.setUpdatedAt(LocalDateTime.now());
        aasModelRepository.save(model);
    }

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

    // TODO: in here should we validate the model or just the submodel?
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

    // TODO: if the submodel didn't change then you shouldn't upload the submodel in theory and give bad request exception
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

    // TODO: check at the end if this method should be public
    public void validateOwnership(AASModel model, String userId) {
        if (!model.getOwnerId().equals(userId)) {
            throw new ForbiddenException("Access denied: model does not belong to user.");
        }
    }

    private AASModel getModelOrThrow(String id, String userId) {
        AASModel model = aasModelRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Model not found."));
        validateOwnership(model, userId);
        return model;
    }


    public void validateReferencedFiles(AASModel model) {
        List<DefaultSubmodel> submodels = model.getSubmodels();

        for (DefaultSubmodel submodel : submodels) {
            List<SubmodelElement> elements = submodel.getSubmodelElements();

            for (SubmodelElement element : elements) {
                if (element instanceof File fileElement) {
                    String fileId = fileElement.getValue();

                    UploadedFile file = uploadedFileRepository.findById(fileId)
                            .orElseThrow(() -> new NotFoundException("Referenced file not found: " + fileId));

                    MultipartFile multipartFile = new MultipartFileAdapter(file);
                    fileUploadValidator.validate(multipartFile);
                }
            }
        }
    }
}
