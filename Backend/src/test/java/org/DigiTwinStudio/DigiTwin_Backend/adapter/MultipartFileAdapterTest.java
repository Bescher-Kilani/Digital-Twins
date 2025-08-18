package org.DigiTwinStudio.DigiTwin_Backend.adapter;

import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultipartFileAdapterTest {

    @Mock
    private UploadedFile uploadedFile;

    private MultipartFileAdapter adapter;

    private final String filename = "test-file.txt";
    private final String contentType = "text/plain";

    @BeforeEach
    void setup() {
        adapter = new MultipartFileAdapter(uploadedFile);
    }

    // --- Constructor Tests ---
    @Test
    void constructor_shouldAcceptNullUploadedFile() {
        // --- Arrange & Act ---
        MultipartFileAdapter nullAdapter = new MultipartFileAdapter(null);

        // --- Assert ---
        assertNotNull(nullAdapter);
    }

    @Test
    void constructor_shouldStoreUploadedFileReference() {
        // --- Arrange ---
        UploadedFile testFile = mock(UploadedFile.class);

        // --- Act ---
        MultipartFileAdapter testAdapter = new MultipartFileAdapter(testFile);

        // --- Assert ---
        assertNotNull(testAdapter);
        // Verify the reference is stored by calling a method that uses it
        when(testFile.getFilename()).thenReturn("test.txt");
        assertEquals("test.txt", testAdapter.getName());
        verify(testFile).getFilename();
    }

    // --- getName() ---
    @Test
    void getName_shouldReturnFilenameFromUploadedFile() {
        // --- Arrange ---
        when(uploadedFile.getFilename()).thenReturn(filename);

        // --- Act ---
        String result = adapter.getName();

        // --- Assert ---
        assertEquals(filename, result);
        verify(uploadedFile).getFilename();
    }

    @Test
    void getName_shouldReturnNullWhenUploadedFileReturnsNull() {
        // --- Arrange ---
        when(uploadedFile.getFilename()).thenReturn(null);

        // --- Act ---
        String result = adapter.getName();

        // --- Assert ---
        assertNull(result);
        verify(uploadedFile).getFilename();
    }

    @Test
    void getName_shouldReturnEmptyStringWhenUploadedFileReturnsEmptyString() {
        // --- Arrange ---
        when(uploadedFile.getFilename()).thenReturn("");

        // --- Act ---
        String result = adapter.getName();

        // --- Assert ---
        assertEquals("", result);
        verify(uploadedFile).getFilename();
    }

    @Test
    void getName_shouldHandleSpecialCharactersInFilename() {
        // --- Arrange ---
        String specialFilename = "tëst fîlé with spåcés & ünicode.txt";
        when(uploadedFile.getFilename()).thenReturn(specialFilename);

        // --- Act ---
        String result = adapter.getName();

        // --- Assert ---
        assertEquals(specialFilename, result);
        verify(uploadedFile).getFilename();
    }

    @Test
    void getName_shouldHandleFilenameWithSpecialCharacters() {
        // --- Arrange ---
        String filenameWithSpecialChars = "file@#$%^&*()_+[]{};':\",./<>?.txt";
        when(uploadedFile.getFilename()).thenReturn(filenameWithSpecialChars);

        // --- Act ---
        String result = adapter.getName();

        // --- Assert ---
        assertEquals(filenameWithSpecialChars, result);
        verify(uploadedFile).getFilename();
    }

    // --- getOriginalFilename() ---
    @Test
    void getOriginalFilename_shouldReturnFilenameFromUploadedFile() {
        // --- Arrange ---
        when(uploadedFile.getFilename()).thenReturn(filename);

        // --- Act ---
        String result = adapter.getOriginalFilename();

        // --- Assert ---
        assertEquals(filename, result);
        verify(uploadedFile).getFilename();
    }

    @Test
    void getOriginalFilename_shouldHandleEmptyString() {
        // --- Arrange ---
        when(uploadedFile.getFilename()).thenReturn("");

        // --- Act ---
        String result = adapter.getOriginalFilename();

        // --- Assert ---
        assertEquals("", result);
        verify(uploadedFile).getFilename();
    }

    @Test
    void getOriginalFilename_shouldHandleUnicodeCharacters() {
        // --- Arrange ---
        String unicodeFilename = "файл测试.документ";
        when(uploadedFile.getFilename()).thenReturn(unicodeFilename);

        // --- Act ---
        String result = adapter.getOriginalFilename();

        // --- Assert ---
        assertEquals(unicodeFilename, result);
        verify(uploadedFile).getFilename();
    }

    // --- getContentType() ---
    @Test
    void getContentType_shouldReturnContentTypeFromUploadedFile() {
        // --- Arrange ---
        when(uploadedFile.getContentType()).thenReturn(contentType);

        // --- Act ---
        String result = adapter.getContentType();

        // --- Assert ---
        assertEquals(contentType, result);
        verify(uploadedFile).getContentType();
    }

    @Test
    void getContentType_shouldReturnNullWhenUploadedFileReturnsNull() {
        // --- Arrange ---
        when(uploadedFile.getContentType()).thenReturn(null);

        // --- Act ---
        String result = adapter.getContentType();

        // --- Assert ---
        assertNull(result);
        verify(uploadedFile).getContentType();
    }

    @Test
    void getContentType_shouldReturnEmptyStringWhenUploadedFileReturnsEmptyString() {
        // --- Arrange ---
        when(uploadedFile.getContentType()).thenReturn("");

        // --- Act ---
        String result = adapter.getContentType();

        // --- Assert ---
        assertEquals("", result);
        verify(uploadedFile).getContentType();
    }

    @Test
    void getContentType_shouldHandleComplexContentTypes() {
        // --- Arrange ---
        String complexContentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        when(uploadedFile.getContentType()).thenReturn(complexContentType);

        // --- Act ---
        String result = adapter.getContentType();

        // --- Assert ---
        assertEquals(complexContentType, result);
        verify(uploadedFile).getContentType();
    }

    // --- isEmpty() ---
    @Test
    void isEmpty_shouldAlwaysReturnFalse() {
        // --- Arrange ---
        // No setup needed as this method always returns false

        // --- Act ---
        boolean result = adapter.isEmpty();

        // --- Assert ---
        assertFalse(result);
        verifyNoInteractions(uploadedFile);
    }

    // --- getSize() ---
    @Test
    void getSize_shouldAlwaysReturnZero() {
        // --- Arrange ---
        // No setup needed as this method always returns 0

        // --- Act ---
        long result = adapter.getSize();

        // --- Assert ---
        assertEquals(0L, result);
        verifyNoInteractions(uploadedFile);
    }

    // --- getBytes() ---
    @Test
    void getBytes_shouldReturnEmptyByteArray() {
        // --- Arrange ---
        // No setup needed as this method always returns empty array

        // --- Act ---
        byte[] result = adapter.getBytes();

        // --- Assert ---
        assertNotNull(result);
        assertEquals(0, result.length);
        verifyNoInteractions(uploadedFile);
    }

    // --- getInputStream() ---
    @Test
    void getInputStream_shouldReturnByteArrayInputStreamWithEmptyContent() throws IOException {
        // --- Arrange ---
        // No setup needed as this method always returns empty stream

        // --- Act ---
        InputStream result = adapter.getInputStream();

        // --- Assert ---
        assertNotNull(result);
        assertEquals(0, result.available());
        verifyNoInteractions(uploadedFile);
    }

    @Test
    void getInputStream_shouldHandleNullUploadedFile() throws IOException {
        // --- Arrange ---
        MultipartFileAdapter nullAdapter = new MultipartFileAdapter(null);

        // --- Act ---
        InputStream result = nullAdapter.getInputStream();

        // --- Assert ---
        assertNotNull(result);
        assertEquals(0, result.available());
    }

    // --- transferTo() ---
    @Test
    void transferTo_shouldThrowUnsupportedOperationException() {
        // --- Arrange ---
        File destinationFile = new File("test-destination.txt");

        // --- Act & Assert ---
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> adapter.transferTo(destinationFile)
        );

        assertEquals("Not supported", exception.getMessage());
        verifyNoInteractions(uploadedFile);
    }

    @Test
    void transferTo_shouldThrowUnsupportedOperationExceptionEvenWithNullFile() {
        // --- Arrange ---
        File destinationFile = null;

        // --- Act & Assert ---
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> adapter.transferTo(destinationFile)
        );

        assertEquals("Not supported", exception.getMessage());
        verifyNoInteractions(uploadedFile);
    }
}