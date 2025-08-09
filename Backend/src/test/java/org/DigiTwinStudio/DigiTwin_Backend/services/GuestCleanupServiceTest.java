package org.DigiTwinStudio.DigiTwin_Backend.services;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.UploadedFileRepository;

import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultFile;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GuestCleanupServiceTest {

    // Mock repositories
    @Mock private AASModelRepository aasModelRepository;
    @Mock private UploadedFileRepository uploadedFileRepository;

    // Service under test
    @InjectMocks
    private GuestCleanupService service;

    // --- 1) No expired models found ---
    @Test
    void deleteExpiredGuestModels_noExpiredModels_doesNothing() {
        // Repository returns an empty list
        when(aasModelRepository.findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Run the method
        service.deleteExpiredGuestModels();

        // Only the repository search method should be called
        verify(aasModelRepository).findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class));
        verifyNoMoreInteractions(aasModelRepository);
        verifyNoInteractions(uploadedFileRepository);
    }

    // --- 2) Files and model are deleted ---
    @Test
    void deleteExpiredGuestModels_deletesFilesAndModel() {
        // One submodel with two file IDs
        var sub = submodelWithFiles("sub-1", "file-1", "file-2");
        var model = model("m1", List.of(sub));

        when(aasModelRepository.findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class)))
                .thenReturn(List.of(model));

        service.deleteExpiredGuestModels();

        // Both files and the model are deleted
        verify(uploadedFileRepository).deleteById("file-1");
        verify(uploadedFileRepository).deleteById("file-2");
        verify(aasModelRepository).deleteById("m1");
    }

    // --- 3) Null/empty submodels: only delete the model ---
    @Test
    void deleteExpiredGuestModels_handlesNullSubmodels_gracefully() {
        var model = model("m1", null); // no submodels

        when(aasModelRepository.findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class)))
                .thenReturn(List.of(model));

        service.deleteExpiredGuestModels();

        // No file deletions, only model deletion
        verifyNoInteractions(uploadedFileRepository);
        verify(aasModelRepository).deleteById("m1");
    }

    // --- 4) Exception while deleting file: model still deleted ---
    @Test
    void deleteExpiredGuestModels_fileDeleteException_doesNotStopModelDeletion() {
        var sub = submodelWithFiles("sub-1", "boom-file");
        var model = model("m1", List.of(sub));

        when(aasModelRepository.findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class)))
                .thenReturn(List.of(model));
        // Simulate exception when deleting the file
        doThrow(new RuntimeException("io error")).when(uploadedFileRepository).deleteById("boom-file");

        assertDoesNotThrow(() -> service.deleteExpiredGuestModels());
        // File deletion attempted, then model deleted
        verify(uploadedFileRepository).deleteById("boom-file");
        verify(aasModelRepository).deleteById("m1");
    }

    // --- 5) Exception while deleting first model: still processes second ---
    @Test
    void deleteExpiredGuestModels_modelDeleteException_isCaught_andContinues() {
        var sub1 = submodelWithFiles("s1", "f1");
        var sub2 = submodelWithFiles("s2", "f2");
        var m1 = model("m1", List.of(sub1));
        var m2 = model("m2", List.of(sub2));

        when(aasModelRepository.findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class)))
                .thenReturn(List.of(m1, m2));

        // First model deletion throws an exception
        doThrow(new RuntimeException("db down")).when(aasModelRepository).deleteById("m1");

        assertDoesNotThrow(() -> service.deleteExpiredGuestModels());

        // Files for both models attempted to delete
        verify(uploadedFileRepository).deleteById("f1");
        verify(uploadedFileRepository).deleteById("f2");

        // deleteById called for both models, even if first failed
        verify(aasModelRepository).deleteById("m1");
        verify(aasModelRepository).deleteById("m2");
    }

    // --- 6) Null file ID is ignored ---
    @Test
    void deleteExpiredGuestModels_ignoresNullFileIds() {
        var sub = new DefaultSubmodel();
        var elements = new ArrayList<SubmodelElement>();

        var f1 = new DefaultFile(); f1.setValue(null);      // null ID → should be ignored
        var f2 = new DefaultFile(); f2.setValue("ok-file"); // valid ID → should be deleted
        elements.add(f1);
        elements.add(f2);
        sub.setSubmodelElements(elements);

        var model = model("m1", List.of(sub));

        when(aasModelRepository.findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class)))
                .thenReturn(List.of(model));

        service.deleteExpiredGuestModels();

        // Capture the deleted file ID and check it’s the correct one
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(uploadedFileRepository, times(1)).deleteById(idCaptor.capture());
        assertEquals("ok-file", idCaptor.getValue());
        verify(aasModelRepository).deleteById("m1");
    }

    // ---------- Helper methods ----------

    // Creates a test AASModel
    private static AASModel model(String id, List<DefaultSubmodel> submodels) {
        return AASModel.builder()
                .id(id)
                .ownerId("GUEST")
                .createdAt(LocalDateTime.now().minusHours(3)) // ensure it's "expired"
                .updatedAt(LocalDateTime.now().minusHours(3))
                .published(false)
                .submodels(submodels)
                .build();
    }

    // Creates a submodel containing file elements with the given IDs
    private static DefaultSubmodel submodelWithFiles(String id, String... fileIds) {
        var sub = new DefaultSubmodel();
        sub.setId(id);
        var elems = new ArrayList<SubmodelElement>();
        for (String fid : fileIds) {
            var f = new DefaultFile();
            f.setValue(fid); // store UploadedFile ID in value
            elems.add(f);
        }
        sub.setSubmodelElements(elems);
        return sub;
    }
}
