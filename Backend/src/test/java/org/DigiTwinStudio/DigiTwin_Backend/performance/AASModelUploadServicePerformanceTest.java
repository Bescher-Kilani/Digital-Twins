package org.DigiTwinStudio.DigiTwin_Backend.performance;

import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.BadRequestException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.services.AASModelUploadService;
import org.DigiTwinStudio.DigiTwin_Backend.validation.AASModelValidator;
import org.DigiTwinStudio.DigiTwin_Backend.validation.FileUploadValidator;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Performance tests for AASModelUploadService
 * - WK60: fast success (~≤3s) & fast rejection (≤1–3s)
 * - WK80: import completes within ≤15s (small & large env)
 * - WK90-lite: 20 concurrent uploads with p90 ≤ 3s
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
class AASModelUploadServicePerformanceTest {

    // NFR thresholds
    private static final Duration FAIL_FAST_1S = Duration.ofSeconds(1);
    private static final Duration FAST_3S = Duration.ofSeconds(3);
    private static final Duration MAX_IMPORT_15S = Duration.ofSeconds(15);
    private static final String OWNER = "owner-1";

    @Spy @InjectMocks
    private AASModelUploadService uploadService;

    @Mock private AASModelValidator aasModelValidator;
    @Mock private AASModelMapper aasModelMapper;
    @Mock private FileUploadValidator fileUploadValidator;

    // testing WK80: import ≤ 15s (small env)
    @Test
    void importJson_under15s() throws Exception {
        MultipartFile json = new MockMultipartFile(
                "file", "env.json", "application/json",
                minimalEnvJson().getBytes(StandardCharsets.UTF_8)
        );
        doReturn(sampleEnvironment(3)).when(uploadService).parseEnvironment(any());

        doNothing().when(fileUploadValidator).validate(any());
        doNothing().when(aasModelValidator).validate(any());
        when(aasModelMapper.toDto(any())).thenReturn(new AASModelDto());

        assertWithin(MAX_IMPORT_15S,
                () -> assertNotNull(uploadService.uploadAASModel(json, OWNER)),
                "uploadAASModel(JSON small)");
    }

    // testing WK80: import ≤ 15s (large env)
    @Test
    void largeValidJson_under15s() throws Exception {
        byte[] big = new byte[5 * 1024 * 1024]; // simulate big payload (I/O is stubbed anyway)
        MultipartFile json = new MockMultipartFile("file", "big-env.json", "application/json", big);

        // Heavier environment (e.g., 300 submodels)
        doReturn(sampleEnvironment(300)).when(uploadService).parseEnvironment(any());
        doNothing().when(fileUploadValidator).validate(any());
        doNothing().when(aasModelValidator).validate(any());
        when(aasModelMapper.toDto(any())).thenReturn(new AASModelDto());

        assertWithin(MAX_IMPORT_15S,
                () -> assertNotNull(uploadService.uploadAASModel(json, OWNER)),
                "uploadAASModel(JSON large)");
    }

    // testing WK60: fast success ≤ 3s
    @Test
    void smallValidJson_fast() throws Exception {
        MultipartFile json = new MockMultipartFile(
                "file", "env.json", "application/json",
                minimalEnvJson().getBytes(StandardCharsets.UTF_8)
        );
        doReturn(sampleEnvironment(2)).when(uploadService).parseEnvironment(any());
        doNothing().when(fileUploadValidator).validate(any());
        doNothing().when(aasModelValidator).validate(any());
        when(aasModelMapper.toDto(any())).thenReturn(new AASModelDto());

        assertWithin(FAST_3S,
                () -> assertNotNull(uploadService.uploadAASModel(json, OWNER)),
                "uploadAASModel(JSON) fast success");
    }

    // testing WK60: too-large rejected fast ≤ 1s
    @Test
    void tooLargeFile_rejectedFast() {
        MultipartFile huge = new MockMultipartFile(
                "file", "huge.bin", "application/octet-stream", new byte[20 * 1024 * 1024]
        );
        // FileUploadValidator throws ValidationException -> service wraps into BadRequestException
        doThrow(new BadRequestException("too large")).when(fileUploadValidator).validate(any());

        assertWithin(FAIL_FAST_1S,
                () -> assertThrows(BadRequestException.class,
                        () -> uploadService.uploadAASModel(huge, OWNER)),
                "reject too-large file fast");
    }

    // testing WK60: invalid-type rejected fast ≤ 3s
    @Test
    void invalidFile_rejectedFast() {
        MultipartFile bad = new MockMultipartFile(
                "file", "bad.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8)
        );
        doThrow(new BadRequestException("invalid type")).when(fileUploadValidator).validate(any());

        assertWithin(FAST_3S,
                () -> assertThrows(BadRequestException.class,
                        () -> uploadService.uploadAASModel(bad, OWNER)),
                "reject invalid file fast");
    }

    // testing WK90-lite: 20 concurrent uploads, 90% ≤ 3s --------------------
    @Test
    void concurrentUploads_p90Under3s() throws Exception {
        // Common happy-path stubs for all threads
        doReturn(sampleEnvironment(3)).when(uploadService).parseEnvironment(any());
        doNothing().when(fileUploadValidator).validate(any());
        doNothing().when(aasModelValidator).validate(any());
        when(aasModelMapper.toDto(any())).thenReturn(new AASModelDto());

        int users = 20;
        ExecutorService pool = Executors.newFixedThreadPool(users);
        try {
            List<Callable<Long>> tasks = new ArrayList<>();
            for (int i = 0; i < users; i++) {
                tasks.add(() -> {
                    MultipartFile json = new MockMultipartFile(
                            "file", "env.json", "application/json",
                            minimalEnvJson().getBytes(StandardCharsets.UTF_8)
                    );
                    long t0 = System.nanoTime();
                    AASModelDto dto = uploadService.uploadAASModel(json, OWNER);
                    assertNotNull(dto);
                    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                });
            }
            List<Future<Long>> futures = pool.invokeAll(tasks);
            List<Long> times = new ArrayList<>(users);
            for (Future<Long> f : futures) times.add(f.get());

            // each ≤ 3s, 90% ≤ 3s
            assertTrue(times.stream().allMatch(ms -> ms <= 3000), "some uploads exceeded 3s");
            Collections.sort(times);
            long p90 = times.get((int)Math.ceil(times.size()*0.9) - 1);
            log.info("concurrent uploads p90={} ms, times={}", p90, times);
            assertTrue(p90 <= 3000, "p90 exceeded 3s");
        } finally {
            pool.shutdownNow();
        }
    }

    // helper methods

    private void assertWithin(Duration limit, Runnable action, String label) {
        Instant t0 = Instant.now();
        action.run();
        long ms = Duration.between(t0, Instant.now()).toMillis();
        log.info("{} took {} ms (limit {} ms)", label, ms, limit.toMillis());
        assertTrue(ms <= limit.toMillis(), label + " exceeded " + limit.toMillis() + " ms");
    }

    private String minimalEnvJson() {
        // Minimal JSON; real graph comes from sampleEnvironment(...)
        return """
        {
          "assetAdministrationShells":[{"id":"aas-1","idShort":"aas"}],
          "submodels":[{"id":"sm-1","idShort":"sm1"}]
        }
        """;
    }

    private Environment sampleEnvironment(int submodelCount) {
        DefaultEnvironment env = new DefaultEnvironment();

        AssetAdministrationShell aas = new DefaultAssetAdministrationShell.Builder()
                .id("aas-1")
                .idShort("aas")
                .build();

        List<Submodel> subs = IntStream.range(0, Math.max(1, submodelCount))
                .mapToObj(i -> (Submodel) new DefaultSubmodel.Builder()
                        .id("sm-" + (i + 1))
                        .idShort("sm" + (i + 1))
                        .build())
                .toList();

        env.setAssetAdministrationShells(List.of(aas));
        env.setSubmodels(subs); // List<Submodel>, not List<DefaultSubmodel>
        return env;
    }
}
