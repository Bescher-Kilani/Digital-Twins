package org.DigiTwinStudio.DigiTwin_Backend.services;

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

import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultFile;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AASModelServiceTest {

    @InjectMocks
    private AASModelService service;

    @Mock
    private AASModelRepository aasModelRepository;
    @Mock
    private AASModelValidator aasModelValidator;
    @Mock
    private UploadedFileRepository uploadedFileRepository;
    @Mock
    private FileUploadValidator fileUploadValidator;
    @Mock
    private AASModelMapper aasModelMapper;
    @Mock
    private SubmodelMapper submodelMapper;
    @Mock
    private MarketPlaceService marketPlaceService;
    @Mock
    private MarketPlaceEntryRepository marketPlaceEntryRepository;

    private final String userId = "user-1";
    private final String otherUserId = "user-2";
    private final String modelId = "model-1";

    private AASModel existingModel;
    private AASModelDto existingDto;

    @BeforeEach
    void setUp() {
        existingModel = baseModel(modelId, userId, false, new ArrayList<>(), new DefaultAssetAdministrationShell());
        existingDto = new AASModelDto();

        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(existingModel));
        when(aasModelMapper.toDto(any(AASModel.class))).thenReturn(existingDto);
    }

    // testing getAllModelsForUser function
    @Test
    void getAllModelsForUser_shouldReturnMappedList() {
        when(aasModelRepository.findByOwnerId(userId)).thenReturn(List.of(existingModel));

        List<AASModelDto> result = service.getAllModelsForUser(userId);

        assertEquals(1, result.size());
        verify(aasModelMapper).toDto(existingModel);
    }

    @Test
    void getAllModelsForUser_returnsEmptyList_whenNoModels() {
        when(aasModelRepository.findByOwnerId(userId)).thenReturn(List.of());

        List<AASModelDto> result = service.getAllModelsForUser(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(aasModelMapper, never()).toDto(any());
    }

    // testing getModelById function
    @Test
    void getModelById_returnsDto_whenOwned() {
        AASModelDto dto = service.getModelById(modelId, userId);
        assertSame(existingDto, dto);
    }

    @Test
    void getModelById_throwsNotFound_whenMissing() {
        when(aasModelRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getModelById("missing", userId));
    }

    @Test
    void getModelById_throwsForbidden_whenOwnedByAnotherUser() {
        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(baseModel(modelId, otherUserId, false, new ArrayList<>(), new DefaultAssetAdministrationShell())));
        assertThrows(ForbiddenException.class, () -> service.getModelById(modelId, userId));
    }

    // testing saveModel function
    @Test
    void saveModel_updatesAas_validatesAndSaves() {
        AASModelDto updateDto = new AASModelDto();
        DefaultAssetAdministrationShell shell = new DefaultAssetAdministrationShell();
        updateDto.setAas(shell);

        when(aasModelRepository.save(any(AASModel.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aasModelMapper.toDto(any(AASModel.class))).thenReturn(existingDto);

        AASModelDto result = service.saveModel(modelId, userId, updateDto);

        assertSame(existingDto, result);
        verify(aasModelValidator).validate(any(AASModel.class));
        verify(aasModelRepository).save(argThat(m -> m.getAas() == shell));
    }

    @Test
    void saveModel_throwsForbidden_whenOwnedByAnotherUser() {
        when(aasModelRepository.findById(modelId))
                .thenReturn(Optional.of(baseModel(modelId, otherUserId, false, new ArrayList<>(), new DefaultAssetAdministrationShell())));

        assertThrows(ForbiddenException.class, () -> service.saveModel(modelId, userId, new AASModelDto()));
        verify(aasModelRepository, never()).save(any());
    }

    // testing createModel function
    @Test
    void createModel_buildsFromDto_validatesAndSaves() {
        AASModelDto dto = new AASModelDto();
        dto.setAas(new DefaultAssetAdministrationShell());
        dto.setSubmodels(new ArrayList<>());

        when(aasModelRepository.save(any(AASModel.class))).thenAnswer(inv -> inv.getArgument(0));

        AASModelDto result = service.createModel(userId, dto);

        assertSame(existingDto, result);
        verify(aasModelValidator).validate(any(AASModel.class));
        verify(aasModelRepository).save(any(AASModel.class));
    }

    // testing publishModel function
    @Test
    void publishModel_publishes_whenNotPublished() throws ConflictException {
        PublishRequestDto req = new PublishRequestDto();
        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(existingModel));

        service.publishModel(modelId, userId, req);

        verify(aasModelValidator).validate(existingModel);
        verify(marketPlaceService).publish(req, existingModel);
    }

    @Test
    void publishModel_throwsConflict_whenAlreadyPublished() {
        AASModel published = baseModel(modelId, userId, true, new ArrayList<>(), new DefaultAssetAdministrationShell());
        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(published));

        assertThrows(ConflictException.class, () -> service.publishModel(modelId, userId, new PublishRequestDto()));
        verify(marketPlaceService, never()).publish(any(), any());
    }

    @Test
    void publishModel_throwsForbidden_whenOwnedByAnotherUser() {
        when(aasModelRepository.findById(modelId))
                .thenReturn(Optional.of(baseModel(modelId, otherUserId, false, new ArrayList<>(), new DefaultAssetAdministrationShell())));

        assertThrows(ForbiddenException.class, () -> service.publishModel(modelId, userId, new PublishRequestDto()));
        verify(marketPlaceService, never()).publish(any(), any());
    }

    // testing unpublishModel function
    @Test
    void unpublishModel_callsService_whenPublished() throws Exception {
        AASModel published = baseModel(modelId, userId, true, new ArrayList<>(), new DefaultAssetAdministrationShell());
        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(published));

        service.unpublishModel(modelId, userId);

        verify(marketPlaceService).unpublish(userId, published);
    }

    @Test
    void unpublishModel_throwsConflict_whenNotPublished() {
        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(existingModel));
        assertThrows(ConflictException.class, () -> service.unpublishModel(modelId, userId));
        verify(marketPlaceService, never()).unpublish(anyString(), any());
    }

    @Test
    void unpublishModel_throwsForbidden_whenOwnedByAnotherUser() {
        when(aasModelRepository.findById(modelId))
                .thenReturn(Optional.of(baseModel(modelId, otherUserId, true, new ArrayList<>(), new DefaultAssetAdministrationShell())));

        assertThrows(ForbiddenException.class, () -> service.unpublishModel(modelId, userId));
        verify(marketPlaceService, never()).unpublish(anyString(), any());
    }

    // testing addEntryModelToUser function
    @Test
    void addEntryModelToUser_copiesPublishedEntry_savesAndIncrementsDownload() {
        String entryId = "entry-1";
        AASModelDto dto = new AASModelDto();

        DefaultAssetAdministrationShell shell = new DefaultAssetAdministrationShell();
        shell.setIdShort("TemplateAAS");
        dto.setAas(shell);
        dto.setSubmodels(new ArrayList<>());

        when(marketPlaceService.getPublishedModel(entryId)).thenReturn(dto);

        service.addEntryModelToUser(entryId, userId);

        verify(aasModelRepository).save(any(AASModel.class));
        verify(marketPlaceService).incrementDownloadCount(entryId);
    }

    // testing attachSubmodel function
    @Test
    void attachSubmodel_addsNewSubmodel_andSaves() {
        AASModel modelWithList = baseModel(modelId, userId, false, new ArrayList<>(), new DefaultAssetAdministrationShell());
        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(modelWithList));

        SubmodelDto subDto = new SubmodelDto();
        DefaultSubmodel sub = new DefaultSubmodel();
        sub.setId("sub-1");
        sub.setSubmodelElements(new ArrayList<>());
        when(submodelMapper.fromDto(subDto)).thenReturn(sub);

        when(aasModelRepository.save(any(AASModel.class))).thenAnswer(inv -> inv.getArgument(0));

        AASModelDto result = service.attachSubmodel(modelId, subDto, userId);

        assertSame(existingDto, result);
        verify(aasModelValidator).validate(any(AASModel.class));
        verify(aasModelRepository).save(argThat(m -> m.getSubmodels().stream().anyMatch(s -> "sub-1".equals(s.getId()))));
    }

    @Test
    void attachSubmodel_throwsConflict_whenDuplicateId() {
        DefaultSubmodel sub = new DefaultSubmodel();
        sub.setId("sub-dup");
        AASModel model = baseModel(modelId, userId, false, new ArrayList<>(List.of(sub)), new DefaultAssetAdministrationShell());
        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(model));

        SubmodelDto subDto = new SubmodelDto();
        when(submodelMapper.fromDto(subDto)).thenReturn(copySubmodel("sub-dup"));

        assertThrows(ConflictException.class, () -> service.attachSubmodel(modelId, subDto, userId));
        verify(aasModelRepository, never()).save(any());
    }

    @Test
    void attachSubmodel_throwsForbidden_whenOwnedByAnotherUser() {
        when(aasModelRepository.findById(modelId))
                .thenReturn(Optional.of(baseModel(modelId, otherUserId, false, new ArrayList<>(), new DefaultAssetAdministrationShell())));

        assertThrows(ForbiddenException.class, () -> service.attachSubmodel(modelId, new SubmodelDto(), userId));
        verify(aasModelRepository, never()).save(any());
    }

    // testing updateSubmodel function
    @Test
    void updateSubmodel_replacesExisting_andSaves() {
        DefaultSubmodel existingSub = copySubmodel("sub-1");
        AASModel model = baseModel(modelId, userId, false, new ArrayList<>(List.of(existingSub)), new DefaultAssetAdministrationShell());
        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(model));

        SubmodelDto dto = new SubmodelDto();
        DefaultSubmodel updated = copySubmodel("sub-NEW");
        when(submodelMapper.fromDto(dto)).thenReturn(updated);
        when(aasModelRepository.save(any(AASModel.class))).thenAnswer(inv -> inv.getArgument(0));

        AASModelDto result = service.updateSubmodel(modelId, "sub-1", dto, userId);

        assertSame(existingDto, result);
        assertEquals(1, model.getSubmodels().size());
        assertEquals("sub-NEW", model.getSubmodels().get(0).getId());
    }

    @Test
    void updateSubmodel_throwsForbidden_whenOwnedByAnotherUser() {
        when(aasModelRepository.findById(modelId))
                .thenReturn(Optional.of(baseModel(modelId, otherUserId, false, new ArrayList<>(), new DefaultAssetAdministrationShell())));

        assertThrows(ForbiddenException.class, () -> service.updateSubmodel(modelId, "sub-1", new SubmodelDto(), userId));
        verify(aasModelRepository, never()).save(any());
    }

    @Test
    void updateSubmodel_throwsNotFound_whenMissingTarget() {
        AASModel model = baseModel(modelId, userId, false, new ArrayList<>(), new DefaultAssetAdministrationShell());
        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(model));

        assertThrows(NotFoundException.class, () -> service.updateSubmodel(modelId, "nope", new SubmodelDto(), userId));
    }

    // testing removeSubmodel function
    @Test
    void removeSubmodel_removesAndDeletesFiles_andSaves() {
        DefaultFile fileElem = new DefaultFile();
        fileElem.setValue("file-1");
        List<SubmodelElement> elements = new ArrayList<>();
        elements.add(fileElem);

        DefaultSubmodel sub = copySubmodel("sub-1");
        sub.setSubmodelElements(elements);

        AASModel model = baseModel(modelId, userId, false, new ArrayList<>(List.of(sub)), new DefaultAssetAdministrationShell());
        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(aasModelRepository.save(any(AASModel.class))).thenAnswer(inv -> inv.getArgument(0));

        AASModelDto result = service.removeSubmodel(modelId, "sub-1", userId);

        assertSame(existingDto, result);
        verify(uploadedFileRepository).deleteById("file-1");
        assertTrue(model.getSubmodels().isEmpty());
    }

    @Test
    void removeSubmodel_throwsNotFound_whenSubmodelMissing() {
        DefaultSubmodel another = copySubmodel("sub-2");
        AASModel model = baseModel(modelId, userId, false, new ArrayList<>(List.of(another)), new DefaultAssetAdministrationShell());
        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(model));

        assertThrows(NotFoundException.class, () -> service.removeSubmodel(modelId, "sub-1", userId));
    }

    @Test
    void removeSubmodel_throwsForbidden_whenOwnedByAnotherUser() {
        when(aasModelRepository.findById(modelId))
                .thenReturn(Optional.of(baseModel(modelId, otherUserId, false, new ArrayList<>(), new DefaultAssetAdministrationShell())));

        assertThrows(ForbiddenException.class, () -> service.removeSubmodel(modelId, "sub-1", userId));
        verify(aasModelRepository, never()).save(any());
        verify(uploadedFileRepository, never()).deleteById(anyString());
    }

    // testing hardDeleteModel function
    @Test
    void hardDeleteModel_deletesModelAndFiles_andMarketplaceEntryIfPublished() {
        DefaultFile fileElem = new DefaultFile();
        fileElem.setValue("file-1");
        DefaultSubmodel sub = copySubmodel("sub-1");
        sub.setSubmodelElements(new ArrayList<>(List.of(fileElem)));

        AASModel published = baseModel(modelId, userId, true, new ArrayList<>(List.of(sub)), new DefaultAssetAdministrationShell());
        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(published));

        service.hardDeleteModel(modelId, userId);

        verify(marketPlaceEntryRepository).deleteById(modelId);
        verify(uploadedFileRepository).deleteById("file-1");
        verify(aasModelRepository).deleteById(modelId);
    }

    @Test
    void hardDeleteModel_wrapsExceptions_inBadRequest() {
        when(aasModelRepository.findById(modelId)).thenReturn(Optional.of(existingModel));
        doThrow(new RuntimeException("db down")).when(aasModelRepository).deleteById(modelId);

        var ex = assertThrows(BadRequestException.class, () -> service.hardDeleteModel(modelId, userId));
        assertTrue(ex.getMessage().contains("Failed to delete model"));
    }

    @Test
    void hardDeleteModel_throwsForbidden_whenOwnedByAnotherUser() {
        when(aasModelRepository.findById(modelId))
                .thenReturn(Optional.of(baseModel(modelId, otherUserId, false, new ArrayList<>(), new DefaultAssetAdministrationShell())));

        assertThrows(ForbiddenException.class, () -> service.hardDeleteModel(modelId, userId));
        verify(aasModelRepository, never()).deleteById(anyString());
        verify(marketPlaceEntryRepository, never()).deleteById(anyString());
        verify(uploadedFileRepository, never()).deleteById(anyString());
    }

    // testing validateModelWithFiles function
    @Test
    void validateModelWithFiles_throwsNotFound_whenFileMissing() {
        DefaultFile missingFileElem = new DefaultFile();
        missingFileElem.setValue("missing-file");
        DefaultSubmodel sub = copySubmodel("s");
        sub.setSubmodelElements(new ArrayList<>(List.of(missingFileElem)));

        AASModel model = baseModel(modelId, userId, false, new ArrayList<>(List.of(sub)), new DefaultAssetAdministrationShell());
        when(uploadedFileRepository.findById("missing-file")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.validateModelWithFiles(model));
        verify(fileUploadValidator, never()).validate(any());
    }

    @Test
    void validateModelWithFiles_callsValidator_whenFilePresent() {
        DefaultFile fileElem = new DefaultFile();
        fileElem.setValue("file-ok");
        DefaultSubmodel sub = copySubmodel("s");
        sub.setSubmodelElements(new ArrayList<>(List.of(fileElem)));

        AASModel model = baseModel(modelId, userId, false, new ArrayList<>(List.of(sub)), new DefaultAssetAdministrationShell());
        when(uploadedFileRepository.findById("file-ok")).thenReturn(Optional.of(new UploadedFile()));

        service.validateModelWithFiles(model);

        verify(fileUploadValidator).validate(any());
    }

    private static AASModel baseModel(String id, String owner, boolean published, List<DefaultSubmodel> subs, DefaultAssetAdministrationShell shell) {
        LocalDateTime now = LocalDateTime.now();
        return AASModel.builder()
                .id(id)
                .ownerId(owner)
                .published(published)
                .createdAt(now)
                .updatedAt(now)
                .submodels(subs)
                .aas(shell)
                .build();
    }

    private static DefaultSubmodel copySubmodel(String id) {
        var s = new DefaultSubmodel();
        s.setId(id);
        s.setSubmodelElements(new ArrayList<>());
        return s;
    }
}
