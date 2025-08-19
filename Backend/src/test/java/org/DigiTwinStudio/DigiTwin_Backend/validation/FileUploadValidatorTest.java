package org.DigiTwinStudio.DigiTwin_Backend.validation;

import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileUploadValidatorTest {

    @InjectMocks
    private FileUploadValidator validator;

    // testing validate() function for file presence
    @Test
    void validate_throwsBadRequest_whenFileIsNull() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(null));
        assertEquals("Uploaded file must not be empty", ex.getMessage());
    }

    @Test
    void validate_throwsBadRequest_whenFileIsEmpty() {
        // Given: empty file
        MultipartFile file = createMockFile("test.pdf", "application/pdf", 0L, true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertEquals("Uploaded file must not be empty", ex.getMessage());
    }

    // testing validate() function for file size
    @Test
    void validate_succeeds_withValidSizeFile() {
        // Given: file within size limit (5MB)
        MultipartFile file = createMockFile("test.pdf", "application/pdf", 5 * 1024 * 1024L, false);

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void validate_succeeds_withMaxSizeFile() {
        // Given: file exactly at size limit (10MB)
        MultipartFile file = createMockFile("test.pdf", "application/pdf", 10 * 1024 * 1024L, false);

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void validate_throwsBadRequest_whenFileSizeExceedsLimit() {
        // Given: file exceeding 10MB limit (11MB)
        long fileSize = 11 * 1024 * 1024L; // 11MB
        MultipartFile file = createMockFile("test.pdf", "application/pdf", fileSize, false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertTrue(ex.getMessage().contains("File size"));
        assertTrue(ex.getMessage().contains("exceeds maximum of 10.00 MB"));
        assertTrue(ex.getMessage().contains("11.00 MB"));
    }

    @Test
    void validate_throwsBadRequest_whenFileSizeSlightlyExceedsLimit() {
        // Given: file just over limit (10MB + 1 byte)
        long fileSize = 10 * 1024 * 1024L + 1;
        MultipartFile file = createMockFile("test.pdf", "application/pdf", fileSize, false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertTrue(ex.getMessage().contains("exceeds maximum"));
    }

    // testing validate() function for MIME type determination
    @Test
    void validate_throwsBadRequest_whenContentTypeIsNull() {
        // Given: file with null content type
        MultipartFile file = createMockFile("test.pdf", null, 1024L, false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertEquals("Could not determine file MIME type", ex.getMessage());
    }

    // testing validate() functions for filename validation
    @Test
    void validate_throwsBadRequest_whenFilenameIsNull() {
        // Given: file with null filename
        MultipartFile file = createMockFile(null, "application/pdf", 1024L, false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertEquals("Original filename must not be blank and must contain an extension", ex.getMessage());
    }

    @Test
    void validate_throwsBadRequest_whenFilenameIsBlank() {
        // Given: file with blank filename
        MultipartFile file = createMockFile("   ", "application/pdf", 1024L, false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertEquals("Original filename must not be blank and must contain an extension", ex.getMessage());
    }

    @Test
    void validate_throwsBadRequest_whenFilenameHasNoExtension() {
        // Given: file with no extension
        MultipartFile file = createMockFile("testfile", "application/pdf", 1024L, false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertEquals("Original filename must not be blank and must contain an extension", ex.getMessage());
    }

    @Test
    void validate_throwsBadRequest_whenFilenameEndsWithDot() {
        // Given: file ending with dot but no extension
        MultipartFile file = createMockFile("testfile.", "application/pdf", 1024L, false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertEquals("Unsupported file extension: .", ex.getMessage());
    }

    // testing validate() function for supported extensions
    @Test
    void validate_succeeds_withValidPdfFile() {
        // Given: valid PDF file
        MultipartFile file = createValidFile("document.pdf", "application/pdf");

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void validate_succeeds_withValidPngFile() {
        // Given: valid PNG file
        MultipartFile file = createValidFile("image.png", "image/png");

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void validate_succeeds_withValidJsonFile() {
        // Given: valid JSON file
        MultipartFile file = createValidFile("data.json", "application/json");

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void validate_succeeds_withValidAasxFile() {
        // Given: valid AASX file
        MultipartFile file = createValidFile("model.aasx", "application/aasx+zip");

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void validate_handlesCaseInsensitiveExtensions() {
        // Given: file with uppercase extension
        MultipartFile file = createValidFile("document.PDF", "application/pdf");

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void validate_handlesMultipleDots() {
        // Given: filename with multiple dots
        MultipartFile file = createValidFile("my.document.with.dots.pdf", "application/pdf");

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void validate_throwsBadRequest_whenExtensionUnsupported() {
        // Given: file with unsupported extension
        MultipartFile file = createValidFile("document.txt", "text/plain");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertEquals("Unsupported file extension: .txt", ex.getMessage());
    }

    @Test
    void validate_throwsBadRequest_whenExtensionUnsupportedUppercase() {
        // Given: file with unsupported uppercase extension
        MultipartFile file = createValidFile("document.DOCX", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertEquals("Unsupported file extension: .docx", ex.getMessage());
    }

    // testing validate() functions for MIME type matching
    @Test
    void validate_throwsBadRequest_whenMimeTypeMismatch() {
        // Given: PDF extension but wrong MIME type
        MultipartFile file = createValidFile("document.pdf", "text/plain");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertEquals("File content type 'text/plain' does not match expected MIME type for '.pdf'", ex.getMessage());
    }

    @Test
    void validate_throwsBadRequest_whenPngMimeTypeMismatch() {
        // Given: PNG extension but wrong MIME type
        MultipartFile file = createValidFile("image.png", "image/jpeg");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertEquals("File content type 'image/jpeg' does not match expected MIME type for '.png'", ex.getMessage());
    }

    @Test
    void validate_throwsBadRequest_whenJsonMimeTypeMismatch() {
        // Given: JSON extension but wrong MIME type
        MultipartFile file = createValidFile("data.json", "text/plain");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertEquals("File content type 'text/plain' does not match expected MIME type for '.json'", ex.getMessage());
    }

    @Test
    void validate_throwsBadRequest_whenAasxMimeTypeMismatch() {
        // Given: AASX extension but wrong MIME type
        MultipartFile file = createValidFile("model.aasx", "application/zip");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertEquals("File content type 'application/zip' does not match expected MIME type for '.aasx'", ex.getMessage());
    }

    // testing comprehensive integration
    @Test
    void validate_validatesCompleteWorkflow_withValidFile() {
        // Given: completely valid file
        MultipartFile file = createMockFile("my.test.document.pdf", "application/pdf", 5 * 1024 * 1024L, false);

        assertDoesNotThrow(() -> validator.validate(file));

        // Verify all checks were performed
        verify(file).isEmpty();
        verify(file).getSize();
        verify(file).getContentType();
        verify(file).getOriginalFilename();
    }

    @Test
    void validate_failsEarly_whenFileIsEmpty() {
        // Given: empty file with otherwise valid properties
        MultipartFile file = createMockFile("document.pdf", "application/pdf", 0L, true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validate(file));
        assertEquals("Uploaded file must not be empty", ex.getMessage());

        // Should not check size/content type if file is empty
        verify(file, never()).getSize();
        verify(file, never()).getContentType();
    }

    @Test
    void validate_failsEarly_whenSizeExceeded() {
        // Given: oversized file with valid extension/MIME type
        MultipartFile file = createMockFile("document.pdf", "application/pdf", 15 * 1024 * 1024L, false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validate(file));
        assertTrue(ex.getMessage().contains("exceeds maximum"));

        verify(file).isEmpty();
        verify(file).getSize();
        verify(file, never()).getContentType();
        verify(file, never()).getOriginalFilename();
        verifyNoMoreInteractions(file);
    }

    private static MultipartFile createMockFile(String filename, String contentType, long size, boolean isEmpty) {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.getOriginalFilename()).thenReturn(filename);
        lenient().when(file.getContentType()).thenReturn(contentType);
        lenient().when(file.getSize()).thenReturn(size);
        lenient().when(file.isEmpty()).thenReturn(isEmpty);
        return file;
    }

    private static MultipartFile createValidFile(String filename, String contentType) {
        return createMockFile(filename, contentType, 1024L, false); // 1KB file
    }
}
