package org.DigiTwinStudio.DigiTwin_Backend.performance;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.FileStorageException;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.UploadedFileRepository;
import org.DigiTwinStudio.DigiTwin_Backend.services.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class FileStorageServicePerformanceTest {

    private static final Duration FAST_1S = Duration.ofSeconds(1);   // WK60: quick ops
    private static final Duration FAST_3S = Duration.ofSeconds(3);   // WK60: typical file store
    private static final Duration MAX_15S = Duration.ofSeconds(15);  // WK80: bulk ops
    private static final int USERS = 20;                             // WK90-lite

    private static final String OWNER = "user-1";
    private static final String MODEL_ID = "model-1";
    private static final String FILE_ID = "file-123";
    private static final String GRIDFS_ID = new ObjectId().toHexString();

    @InjectMocks
    private FileStorageService fileStorageService;

    @Mock private UploadedFileRepository uploadedFileRepository;
    @Mock private GridFsTemplate gridFsTemplate;
    @Mock private GridFSFile gridFSFile;
    @Mock private GridFsResource gridFsResource;

    // testing WK60: store typical 5MB file ≤ 3s
    @Test
    void store_under3s() throws Exception {
        byte[] content = new byte[5 * 1024 * 1024]; // 5 MB
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", content);

        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString()))
                .thenReturn(new ObjectId());
        when(uploadedFileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertWithin(FAST_3S,
                () -> {
                    UploadedFile saved = fileStorageService.store(file, OWNER, MODEL_ID);
                    assertNotNull(saved);
                    assertEquals("doc.pdf", saved.getFilename());
                },
                "store(5MB file)");
    }

    // testing WK60: getFileContent first byte ≤ 1s
    @Test
    void getFileContent_fast() throws Exception {
        UploadedFile meta = UploadedFile.builder()
                .id(FILE_ID).ownerId(OWNER).filename("doc.pdf").storagePath(GRIDFS_ID).build();

        when(uploadedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(meta));
        when(gridFsTemplate.findOne(any())).thenReturn(gridFSFile);
        when(gridFsTemplate.getResource(gridFSFile)).thenReturn(gridFsResource);
        when(gridFsResource.getInputStream()).thenReturn(new ByteArrayInputStream("hello".getBytes()));

        assertWithin(FAST_1S,
                () -> {
                    InputStream in = fileStorageService.getFileContent(FILE_ID);
                    assertNotNull(in);
                    try {
                        assertEquals('h', in.read());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                "getFileContent");
    }

    // testing WK60: delete ≤ 1s
    @Test
    void delete_fast() {
        UploadedFile meta = UploadedFile.builder()
                .id(FILE_ID).ownerId(OWNER).filename("doc.pdf").storagePath(GRIDFS_ID).build();

        when(uploadedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(meta));
        doNothing().when(gridFsTemplate).delete(any());
        doNothing().when(uploadedFileRepository).deleteById(FILE_ID);

        assertWithin(FAST_1S,
                () -> fileStorageService.delete(FILE_ID, OWNER),
                "delete(file)");
    }

    // testing WK60: delete fails if not owner (≤ 1s fast rejection)
    @Test
    void delete_wrongOwner_fastFail() {
        UploadedFile meta = UploadedFile.builder()
                .id(FILE_ID).ownerId("someone-else").storagePath(GRIDFS_ID).build();

        when(uploadedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(meta));

        assertWithin(FAST_1S,
                () -> assertThrows(FileStorageException.class,
                        () -> fileStorageService.delete(FILE_ID, OWNER)),
                "delete(wrong owner) fast fail");
    }

    // testing WK80: bulk retrieval of 50 files ≤ 15s
    @Test
    void bulkRetrieval_under15s() throws Exception {
        List<UploadedFile> metas = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            metas.add(UploadedFile.builder()
                    .id("f-" + i).ownerId(OWNER).modelId(MODEL_ID)
                    .filename("doc-" + i + ".pdf").storagePath(GRIDFS_ID).build());
        }

        when(uploadedFileRepository.findAllByModelId(MODEL_ID)).thenReturn(metas);
        when(gridFsTemplate.findOne(any())).thenReturn(gridFSFile);
        when(gridFsTemplate.getResource(gridFSFile)).thenReturn(gridFsResource);
        when(gridFsResource.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));

        assertWithin(MAX_15S,
                () -> {
                    List<byte[]> contents = fileStorageService.getFileContentsByModelId(MODEL_ID);
                    assertEquals(50, contents.size());
                },
                "getFileContents(50 files)");
    }

    // testing WK90-lite: 20 concurrent store/get/delete ops, p90 ≤ 3s
    @Test
    void concurrentOps_p90Under3s() throws Exception {
        // Stubs for store
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString()))
                .thenReturn(new ObjectId());
        when(uploadedFileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Stubs for get
        UploadedFile meta = UploadedFile.builder()
                .id(FILE_ID).ownerId(OWNER).filename("doc.pdf").storagePath(GRIDFS_ID).build();
        when(uploadedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(meta));
        when(gridFsTemplate.findOne(any())).thenReturn(gridFSFile);
        when(gridFsTemplate.getResource(gridFSFile)).thenReturn(gridFsResource);
        when(gridFsResource.getInputStream()).thenReturn(new ByteArrayInputStream("abc".getBytes()));

        // Stubs for delete
        doNothing().when(gridFsTemplate).delete(any());
        doNothing().when(uploadedFileRepository).deleteById(FILE_ID);

        ExecutorService pool = Executors.newFixedThreadPool(USERS);
        try {
            List<Callable<Long>> tasks = new ArrayList<>();
            MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[1024]);

            for (int i = 0; i < USERS; i++) {
                final int idx = i;
                tasks.add(() -> {
                    long t0 = System.nanoTime();
                    if (idx % 3 == 0) {
                        fileStorageService.store(file, OWNER, MODEL_ID);
                    } else if (idx % 3 == 1) {
                        fileStorageService.getFileContent(FILE_ID);
                    } else {
                        fileStorageService.delete(FILE_ID, OWNER);
                    }
                    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                });
            }

            List<Future<Long>> futures = pool.invokeAll(tasks);
            List<Long> times = new ArrayList<>();
            for (Future<Long> f : futures) times.add(f.get());

            assertTrue(times.stream().allMatch(ms -> ms <= 3000), "some ops exceeded 3s");
            times.sort(Long::compareTo);
            long p90 = times.get((int)Math.ceil(times.size() * 0.9) - 1);
            log.info("concurrent ops p90={} ms, times={}", p90, times);
            assertTrue(p90 <= 3000, "p90 exceeded 3s");
        } finally {
            pool.shutdownNow();
        }
    }

    // helper method
    private void assertWithin(Duration limit, Runnable action, String label) {
        Instant t0 = Instant.now();
        action.run();
        long ms = Duration.between(t0, Instant.now()).toMillis();
        log.info("{} took {} ms (limit {} ms)", label, ms, limit.toMillis());
        assertTrue(ms <= limit.toMillis(),
                label + " exceeded " + limit.toMillis() + " ms");
    }
}
