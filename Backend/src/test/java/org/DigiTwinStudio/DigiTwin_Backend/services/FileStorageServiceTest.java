package org.DigiTwinStudio.DigiTwin_Backend.services;

import com.mongodb.client.gridfs.model.GridFSFile;

import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.FileStorageException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.UploadedFileRepository;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.aasx.InMemoryFile;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock private UploadedFileRepository uploadedFileRepository;
    @Mock private GridFsTemplate gridFsTemplate;

    @InjectMocks
    private FileStorageService service;

    // ---------- store() ----------

    @Test
    void store_savesToGridFs_andPersistsMetadata() throws Exception {
        // Given an uploaded MultipartFile
        MultipartFile mf = mock(MultipartFile.class);
        when(mf.getOriginalFilename()).thenReturn("doc.pdf");
        when(mf.getContentType()).thenReturn("application/pdf");
        when(mf.getSize()).thenReturn(42L);
        when(mf.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1,2,3}));

        // GridFS returns an ObjectId for the stored binary
        ObjectId gridId = new ObjectId();
        when(gridFsTemplate.store(any(InputStream.class), eq("doc.pdf"), eq("application/pdf")))
                .thenReturn(gridId);

        // Repository returns the entity it saved
        UploadedFile saved = UploadedFile.builder()
                .id("gen-id")
                .modelId("m1")
                .filename("doc.pdf")
                .contentType("application/pdf")
                .size(42L)
                .ownerId("u1")
                .storagePath(gridId.toHexString())
                .uploadedAt(LocalDateTime.now())
                .build();
        when(uploadedFileRepository.save(any(UploadedFile.class))).thenReturn(saved);

        // When
        UploadedFile out = service.store(mf, "u1", "m1");

        // Then
        assertEquals(saved, out);
        // capture the metadata entity to assert key fields
        ArgumentCaptor<UploadedFile> captor = ArgumentCaptor.forClass(UploadedFile.class);
        verify(uploadedFileRepository).save(captor.capture());
        UploadedFile toSave = captor.getValue();
        assertEquals("m1", toSave.getModelId());
        assertEquals("doc.pdf", toSave.getFilename());
        assertEquals("application/pdf", toSave.getContentType());
        assertEquals(42L, toSave.getSize());
        assertEquals("u1", toSave.getOwnerId());
        assertEquals(gridId.toHexString(), toSave.getStoragePath());
    }

    @Test
    void store_wrapsIOException_asFileStorageException() throws Exception {
        MultipartFile mf = mock(MultipartFile.class);
        when(mf.getInputStream()).thenThrow(new IOException("read fail"));

        assertThrows(FileStorageException.class, () -> service.store(mf, "u1", "m1"));
        verifyNoInteractions(uploadedFileRepository);
    }


    // ---------- delete() ----------

    @Test
    void delete_happyPath_deletesGridFsAndMetadata() {
        // Given metadata exists and the caller is owner
        UploadedFile meta = UploadedFile.builder()
                .id("fid")
                .ownerId("u1")
                .storagePath(new ObjectId().toHexString())
                .build();
        when(uploadedFileRepository.findById("fid")).thenReturn(Optional.of(meta));

        // When
        service.delete("fid", "u1");

        // Then: one delete in GridFS and one in collection
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(gridFsTemplate).delete(queryCaptor.capture());
        verify(uploadedFileRepository).deleteById("fid");
        assertNotNull(queryCaptor.getValue()); // basic sanity check on query
    }

    @Test
    void delete_throws_whenOwnerMismatch() {
        UploadedFile meta = UploadedFile.builder()
                .id("fid")
                .ownerId("other")
                .storagePath(new ObjectId().toHexString())
                .build();
        when(uploadedFileRepository.findById("fid")).thenReturn(Optional.of(meta));

        assertThrows(FileStorageException.class, () -> service.delete("fid", "u1"));
        verify(gridFsTemplate, never()).delete(any());
        verify(uploadedFileRepository, never()).deleteById(anyString());
    }

    @Test
    void delete_wrapsExceptions_fromGridFsOrRepo() {
        UploadedFile meta = UploadedFile.builder()
                .id("fid")
                .ownerId("u1")
                .storagePath(new ObjectId().toHexString())
                .build();
        when(uploadedFileRepository.findById("fid")).thenReturn(Optional.of(meta));
        // Make GridFS delete throw
        doThrow(new RuntimeException("grid fail")).when(gridFsTemplate).delete(any(Query.class));

        assertThrows(FileStorageException.class, () -> service.delete("fid", "u1"));
    }

    // ---------- getInMemoryFilesByModelId() ----------

    @Test
    void getInMemoryFilesByModelId_loadsFiles_fromGridFs() throws Exception {
        // Two metadata rows for the model
        UploadedFile f1 = uf("m1", "file1.pdf", new ObjectId().toHexString());
        UploadedFile f2 = uf("m1", "file2.png", new ObjectId().toHexString());
        when(uploadedFileRepository.findAllByModelId("m1")).thenReturn(List.of(f1, f2));

        // For each storagePath, GridFS returns a file & resource
        GridFSFile g1 = mock(GridFSFile.class);
        GridFSFile g2 = mock(GridFSFile.class);
        GridFsResource r1 = mock(GridFsResource.class);
        GridFsResource r2 = mock(GridFsResource.class);

        when(gridFsTemplate.findOne(argThat(q -> q != null))).thenReturn(g1, g2);
        when(gridFsTemplate.getResource(g1)).thenReturn(r1);
        when(gridFsTemplate.getResource(g2)).thenReturn(r2);
        when(r1.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1,2}));
        when(r2.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{3}));

        // When
        List<InMemoryFile> out = service.getInMemoryFilesByModelId("m1");

        // Then
        assertEquals(2, out.size());
        // path is prefixed with "aasx/"
        assertTrue(out.get(0).getPath().startsWith("aasx/"));
        assertTrue(out.get(1).getPath().startsWith("aasx/"));
        // contents match
        assertArrayEquals(new byte[]{1,2}, out.get(0).getFileContent());
        assertArrayEquals(new byte[]{3}, out.get(1).getFileContent());
    }

    @Test
    void getInMemoryFilesByModelId_skipsMissingGridFsFile() throws Exception {
        UploadedFile f1 = uf("m1", "a.txt", new ObjectId().toHexString());
        when(uploadedFileRepository.findAllByModelId("m1")).thenReturn(List.of(f1));

        // Simulate GridFS lookup miss
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(null);

        // Should not throw; just skip missing one → returns empty list
        List<InMemoryFile> out = service.getInMemoryFilesByModelId("m1");
        assertTrue(out.isEmpty());
    }

    @Test
    void getInMemoryFilesByModelId_wrapsIOExceptions() throws Exception {
        UploadedFile f1 = uf("m1", "a.txt", new ObjectId().toHexString());
        when(uploadedFileRepository.findAllByModelId("m1")).thenReturn(List.of(f1));

        GridFSFile g1 = mock(GridFSFile.class);
        GridFsResource r1 = mock(GridFsResource.class);
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(g1);
        when(gridFsTemplate.getResource(g1)).thenReturn(r1);
        when(r1.getInputStream()).thenThrow(new java.io.IOException("boom"));

        assertThrows(RuntimeException.class, () -> service.getInMemoryFilesByModelId("m1"));
    }

    // ---------- getFileContentsByModelId() ----------

    @Test
    void getFileContentsByModelId_returnsBytes_forEachFile() throws Exception {
        UploadedFile f1 = uf("m1", "a.txt", new ObjectId().toHexString());
        when(uploadedFileRepository.findAllByModelId("m1")).thenReturn(List.of(f1));

        GridFSFile g1 = mock(GridFSFile.class);
        GridFsResource r1 = mock(GridFsResource.class);
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(g1);
        when(gridFsTemplate.getResource(g1)).thenReturn(r1);
        when(r1.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{7,8,9}));

        List<byte[]> out = service.getFileContentsByModelId("m1");
        assertEquals(1, out.size());
        assertArrayEquals(new byte[]{7,8,9}, out.get(0));
    }

    // ---------- getMetadata() ----------

    @Test
    void getMetadata_returns_whenPresent() {
        UploadedFile f = uf("m1", "a.txt", "deadbeefdeadbeefdeadbeef");
        when(uploadedFileRepository.findById("fid")).thenReturn(Optional.of(f));

        UploadedFile out = service.getMetadata("fid");
        assertEquals(f, out);
    }

    @Test
    void getMetadata_throws_whenMissing() {
        when(uploadedFileRepository.findById("fid")).thenReturn(Optional.empty());
        assertThrows(FileStorageException.class, () -> service.getMetadata("fid"));
    }

    // ---------- getFileContent() ----------

    @Test
    void getFileContent_happy_returnsStream() throws Exception {
        UploadedFile f = uf("m1", "a.txt", new ObjectId().toHexString());
        when(uploadedFileRepository.findById("fid")).thenReturn(Optional.of(f));

        GridFSFile gf = mock(GridFSFile.class);
        GridFsResource res = mock(GridFsResource.class);
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(gf);
        when(gridFsTemplate.getResource(gf)).thenReturn(res);
        when(res.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1}));

        InputStream in = service.getFileContent("fid");
        assertEquals(1, in.read());
    }

    @Test
    void getFileContent_throwsWrappedNotFound_whenGridFsMissing() {
        UploadedFile f = uf("m1", "a.txt", new ObjectId().toHexString());
        when(uploadedFileRepository.findById("fid")).thenReturn(Optional.of(f));
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(null);

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> service.getFileContent("fid"));

        assertInstanceOf(NotFoundException.class, ex.getCause());
        assertTrue(ex.getMessage().contains("Failed to load file content"));
    }


    @Test
    void getFileContent_wrapsUnexpectedExceptions() {
        UploadedFile f = uf("m1", "a.txt", "invalid-object-id"); // causes ObjectId ctor to throw
        when(uploadedFileRepository.findById("fid")).thenReturn(Optional.of(f));

        assertThrows(FileStorageException.class, () -> service.getFileContent("fid"));
    }

    // ---------- exists() ----------

    @Test
    void exists_false_whenMetadataMissing() {
        when(uploadedFileRepository.findById("fid")).thenReturn(Optional.empty());
        assertFalse(service.exists("fid"));
    }

    @Test
    void exists_true_whenMetadataAndGridFsPresent() {
        UploadedFile f = uf("m1", "a.txt", new ObjectId().toHexString());
        when(uploadedFileRepository.findById("fid")).thenReturn(Optional.of(f));
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(mock(GridFSFile.class));

        assertTrue(service.exists("fid"));
    }

    @Test
    void exists_wrapsInvalidObjectId_asFileStorageException() {
        UploadedFile f = uf("m1", "a.txt", "not-a-valid-objectid");
        when(uploadedFileRepository.findById("fid")).thenReturn(Optional.of(f));

        assertThrows(FileStorageException.class, () -> service.exists("fid"));
    }

    // ---------- helper ----------

    private static UploadedFile uf(String modelId, String filename, String storagePath) {
        return UploadedFile.builder()
                .id("fid")
                .modelId(modelId)
                .filename(filename)
                .contentType("application/octet-stream")
                .size(1L)
                .ownerId("u1")
                .storagePath(storagePath)
                .uploadedAt(LocalDateTime.now())
                .build();
    }
}
