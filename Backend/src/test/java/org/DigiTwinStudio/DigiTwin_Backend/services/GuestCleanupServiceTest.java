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

    @Mock private AASModelRepository aasModelRepository;
    @Mock private UploadedFileRepository uploadedFileRepository;

    @InjectMocks
    private GuestCleanupService service;

    // --- 1) Nichts zu löschen ---
    @Test
    void deleteExpiredGuestModels_noExpiredModels_doesNothing() {
        when(aasModelRepository.findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class)))
                .thenReturn(List.of());

        service.deleteExpiredGuestModels();

        verify(aasModelRepository).findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class));
        verifyNoMoreInteractions(aasModelRepository);
        verifyNoInteractions(uploadedFileRepository);
    }

    // --- 2) Dateien und Model werden gelöscht ---
    @Test
    void deleteExpiredGuestModels_deletesFilesAndModel() {
        var sub = submodelWithFiles("sub-1", "file-1", "file-2");
        var model = model("m1", List.of(sub));

        when(aasModelRepository.findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class)))
                .thenReturn(List.of(model));

        service.deleteExpiredGuestModels();

        verify(uploadedFileRepository).deleteById("file-1");
        verify(uploadedFileRepository).deleteById("file-2");
        verify(aasModelRepository).deleteById("m1");
    }

    // --- 3) Null/leer-Submodels: nur Model löschen ---
    @Test
    void deleteExpiredGuestModels_handlesNullSubmodels_gracefully() {
        var model = model("m1", /*submodels*/ null);

        when(aasModelRepository.findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class)))
                .thenReturn(List.of(model));

        service.deleteExpiredGuestModels();

        verifyNoInteractions(uploadedFileRepository);
        verify(aasModelRepository).deleteById("m1");
    }

    // --- 4) Exception beim Datei-Löschen: Model wird trotzdem gelöscht ---
    @Test
    void deleteExpiredGuestModels_fileDeleteException_doesNotStopModelDeletion() {
        var sub = submodelWithFiles("sub-1", "boom-file");
        var model = model("m1", List.of(sub));

        when(aasModelRepository.findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class)))
                .thenReturn(List.of(model));
        doThrow(new RuntimeException("io error")).when(uploadedFileRepository).deleteById("boom-file");

        assertDoesNotThrow(() -> service.deleteExpiredGuestModels());
        verify(uploadedFileRepository).deleteById("boom-file");
        verify(aasModelRepository).deleteById("m1");
    }

    // --- 5) Exception beim Model-Löschen: wird geloggt, Lauf geht weiter ---
    @Test
    void deleteExpiredGuestModels_modelDeleteException_isCaught_andContinues() {
        var sub1 = submodelWithFiles("s1", "f1");
        var sub2 = submodelWithFiles("s2", "f2");
        var m1 = model("m1", List.of(sub1));
        var m2 = model("m2", List.of(sub2));

        when(aasModelRepository.findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class)))
                .thenReturn(List.of(m1, m2));

        // erstes Model wirft Exception beim Löschen
        doThrow(new RuntimeException("db down")).when(aasModelRepository).deleteById("m1");

        assertDoesNotThrow(() -> service.deleteExpiredGuestModels());

        // Dateien beider Modelle versucht zu löschen
        verify(uploadedFileRepository).deleteById("f1");
        verify(uploadedFileRepository).deleteById("f2");

        // deleteById auf beiden, m1 wirft, m2 klappt
        verify(aasModelRepository).deleteById("m1");
        verify(aasModelRepository).deleteById("m2");
    }

    // --- 6) Null-FileId wird ignoriert ---
    @Test
    void deleteExpiredGuestModels_ignoresNullFileIds() {
        var sub = new DefaultSubmodel();
        var elements = new ArrayList<SubmodelElement>();

        var f1 = new DefaultFile(); f1.setValue(null);      // null ID → soll ignoriert werden
        var f2 = new DefaultFile(); f2.setValue("ok-file"); // gültig → soll gelöscht werden
        elements.add(f1);
        elements.add(f2);
        sub.setSubmodelElements(elements);

        var model = model("m1", List.of(sub));

        when(aasModelRepository.findByOwnerIdAndCreatedAtBefore(eq("GUEST"), any(LocalDateTime.class)))
                .thenReturn(List.of(model));

        service.deleteExpiredGuestModels();

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(uploadedFileRepository, times(1)).deleteById(idCaptor.capture());
        assertEquals("ok-file", idCaptor.getValue());
        verify(aasModelRepository).deleteById("m1");
    }

    // ---------- Helpers ----------

    private static AASModel model(String id, List<DefaultSubmodel> submodels) {
        return AASModel.builder()
                .id(id)
                .ownerId("GUEST")
                .createdAt(LocalDateTime.now().minusHours(3))
                .updatedAt(LocalDateTime.now().minusHours(3))
                .published(false)
                .submodels(submodels)
                .build();
    }

    private static DefaultSubmodel submodelWithFiles(String id, String... fileIds) {
        var sub = new DefaultSubmodel();
        sub.setId(id);
        var elems = new ArrayList<SubmodelElement>();
        for (String fid : fileIds) {
            var f = new DefaultFile();
            f.setValue(fid);            // hier steckt deine UploadedFile-ID
            elems.add(f);
        }
        sub.setSubmodelElements(elems);
        return sub;
    }
}
