package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.PublishRequestDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.SubmodelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TagRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.UploadedFileRepository;
import org.DigiTwinStudio.DigiTwin_Backend.validation.AASModelValidator;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AASModelService {

    private final AASModelRepository aasModelRepository;
    private final AASModelValidator aasModelValidator;
    private final TagRepository tagRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final AASModelMapper aasModelMapper;
    private final SubmodelMapper submodelMapper; //let's see if it is needed

    @Transactional(readOnly = true)
    public List<AASModelDto> getAllForUser(String userId) {
        List<AASModel> models = aasModelRepository.findByOwnerIdAndDeletedFalse(userId);
        return models.stream().map(aasModelMapper::toDto).toList();
    }

    // TODO: still needs a consistent error handling
    @Transactional(readOnly = true)
    public AASModelDto getById(String id, String userId) throws ResponseStatusException{
        //changed because loading all the users data from database and filtering it in the application code
        // can expose the user data, that is why I added findByIdAndOwnerIdAndDeletedFalse method to AASModelRepository

        // Changed to specify Error
        AASModel model = getModelOrThrow(id, userId);
        return this.aasModelMapper.toDto(model);
    }

    public AASModelDto createEmpty(String userId) {
        AASModel aasModel = AASModel.builder()
                .ownerId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(false)
                .published(false)
                // Not sure if initializing 'aas' and 'submodels' is strictly necessary here.
                // Will verify if leaving them null causes NullPointerException in downstream logic.
                .aas(new DefaultAssetAdministrationShell())
                .submodels(new ArrayList<>())
                .build();

        AASModel savedModel = aasModelRepository.save(aasModel);
        return aasModelMapper.toDto(savedModel);
    }
    // ToDo: implement
    public AASModelDto updateModel(String id, String userId, AASModelDto dto) {
        throw new NotImplementedException();
    }
    public AASModelDto saveModel(String id, String userId, AASModelDto aasModelDto) throws ValidationException, ResponseStatusException {
        AASModel existingModel = aasModelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Model not found."));

        validateOwnership(existingModel, userId);

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
    }

    public void deleteModel(String id, String userId) throws ResponseStatusException {
        AASModel model = getModelOrThrow(id, userId);
        model.setDeleted(true);
        model.setUpdatedAt(LocalDateTime.now());
        aasModelRepository.save(model);
    }

    public void validateOwnership(AASModel model, String userId) {
        if (!model.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for this model");
        }
    }

    public AASModelDto publishModel(String id, String userId, PublishRequestDto request) throws ValidationException, ResponseStatusException {
        AASModel model = getModelOrThrow(id, userId);

        if (model.isPublished()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Model is already published");
        }

        // TODO: Rewrite to include specific invalid tag IDs in the error message
        // Current check only tells if some tags are invalid, not which ones
        if (!tagRepository.findByIdIn(request.getTagIds()).stream().map(tag -> tag.getId()).toList().containsAll(request.getTagIds())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more tags are invalid");
        }

        model.setPublished(true);
        model.setUpdatedAt(LocalDateTime.now());

        aasModelValidator.validate(model);
        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    public AASModelDto attachSubmodel(String modelId, SubmodelDto dto, String userId) throws ValidationException {
        AASModel model = getModelOrThrow(modelId, userId);
        DefaultSubmodel submodel = submodelMapper.fromDto(dto);

        model.getSubmodels().add(submodel);
        model.setUpdatedAt(LocalDateTime.now());

        aasModelValidator.validate(model);
        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    public AASModelDto updateSubmodel(String modelId, String submodelId, SubmodelDto dto, String userId) throws ValidationException {
        AASModel model = getModelOrThrow(modelId, userId);
        List<DefaultSubmodel> submodels = model.getSubmodels();

        boolean replaced = false;
        for (int i = 0; i < submodels.size(); i++) {
            if (submodels.get(i).getId().equals(submodelId)) {
                submodels.set(i, submodelMapper.fromDto(dto));
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submodel not found.");
        }

        model.setUpdatedAt(LocalDateTime.now());
        aasModelValidator.validate(model);
        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    public AASModelDto removeSubmodel(String modelId, String submodelId, String userId) throws ValidationException {
        AASModel model = getModelOrThrow(modelId, userId);
        boolean removed = model.getSubmodels().removeIf(submodel -> submodel.getId().equals(submodelId));

        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submodel not found.");
        }

        model.setUpdatedAt(LocalDateTime.now());
        //check if it is really necessary
        aasModelValidator.validate(model);
        return aasModelMapper.toDto(aasModelRepository.save(model));
    }

    private AASModel getModelOrThrow(String id, String userId) throws ResponseStatusException {
        Optional<AASModel> optionalModel = aasModelRepository.findByIdAndDeletedFalse(id);

        if (optionalModel.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Model not found");
        }

        AASModel model = optionalModel.get();

        if (!model.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return model;
    }
}
