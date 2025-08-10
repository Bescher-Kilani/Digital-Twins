package org.DigiTwinStudio.DigiTwin_Backend.services;

import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.UploadResponseDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.FileStorageException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ValidationException;
import org.DigiTwinStudio.DigiTwin_Backend.validation.FileUploadValidator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyFileUploadServiceTest {

    @Mock private FileStorageService fileStorageService;
    @Mock private FileUploadValidator fileUploadValidator;

    @InjectMocks
    private PropertyFileUploadService service;

    // ---------- uploadFile ----------

    @Test
    void uploadFile_validates_then_stores_and_maps_response() {
        // Given a MultipartFile (real mock file is fine here)
        MultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{1,2,3});

        String modelId = "m1";
        String userId  = "u1";

        // Stored entity returned by FileStorageService
        LocalDateTime uploadedAt = LocalDateTime.now();
        UploadedFile saved = UploadedFile.builder()
                .id("file-123")
                .modelId(modelId)
                .filename("doc.pdf")
                .contentType("application/pdf")
                .size(3L)
                .ownerId(userId)
                .uploadedAt(uploadedAt)
                .storagePath("gridfs-abc")
                .build();

        when(fileStorageService.store(file, userId, modelId)).thenReturn(saved);

        // When
        UploadResponseDto out = service.uploadFile(file, modelId, userId);

        // Then: mapping is correct
        assertEquals("file-123", out.getFileId());
        assertEquals("doc.pdf", out.getFilename());
        assertEquals("application/pdf", out.getContentType());
        assertEquals(3L, out.getSize());
        assertEquals(uploadedAt, out.getUploadedAt());

        // And: order is validate -> store
        InOrder order = inOrder(fileUploadValidator, fileStorageService);
        order.verify(fileUploadValidator).validate(file);
        order.verify(fileStorageService).store(file, userId, modelId);
        order.verifyNoMoreInteractions();
    }

    @Test
    void uploadFile_throws_when_validator_fails() {
        MultipartFile file = new MockMultipartFile(
                "file", "bad.xyz", "application/octet-stream", new byte[]{});

        // Make the validator fail
        doThrow(new BadRequestException("nope"))
                .when(fileUploadValidator).validate(file);

        assertThrows(ValidationException.class, () -> service.uploadFile(file, "m1", "u1"));

        // Ensure storage is NOT called if validation fails
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadFile_bubbles_up_storage_errors() {
        MultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{1,2,3});

        // Validation ok
        doNothing().when(fileUploadValidator).validate(file);

        // Storage throws
        when(fileStorageService.store(file, "u1", "m1"))
                .thenThrow(new FileStorageException("boom"));

        assertThrows(FileStorageException.class, () -> service.uploadFile(file, "m1", "u1"));
    }

    // ---------- deleteFile ----------

    @Test
    void deleteFile_delegates_to_storage() {
        service.deleteFile("f1", "u1");
        verify(fileStorageService).delete("f1", "u1");
    }

    @Test
    void deleteFile_bubbles_up_storage_exception() {
        doThrow(new FileStorageException("cannot")).when(fileStorageService).delete("f1", "u1");
        assertThrows(FileStorageException.class, () -> service.deleteFile("f1", "u1"));
    }
}
