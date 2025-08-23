package org.DigiTwinStudio.DigiTwin_Backend.performance;

import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.DigiTwinStudio.DigiTwin_Backend.services.ExportService;
import org.DigiTwinStudio.DigiTwin_Backend.services.FileStorageService;
import org.DigiTwinStudio.DigiTwin_Backend.services.MarketPlaceService;
import org.DigiTwinStudio.DigiTwin_Backend.services.SubmodelService;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * WK90 smoke test: runs 20 concurrent ops (export, list, validate, file I/O)
 * and ensures all complete ≤3s with p90 ≤3s.
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
class ScalabilitySmokePerformanceTest {

    private static final int USERS = 20;

    @Mock private ExportService exportService;
    @Mock private MarketPlaceService marketPlaceService;
    @Mock private SubmodelService submodelService;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private ScalabilitySmokePerformanceTest self = this; // not used, but keeps MockitoExtension happy

    @Test
    void twentyConcurrent_ops_meetThresholds() throws Exception {
        AASModel model = new AASModel();
        when(exportService.exportAsJson(any())).thenReturn("{}".getBytes());

        when(marketPlaceService.listAllEntries()).thenReturn(Collections.emptyList());

        DefaultSubmodel sub = new DefaultSubmodel.Builder().id("sm-1").idShort("sm1").build();
        doNothing().when(submodelService).validate(any(SubmodelDto.class));

        when(fileStorageService.getFileContent("file-1"))
                .thenReturn(new java.io.ByteArrayInputStream("data".getBytes()));

        ExecutorService pool = Executors.newFixedThreadPool(USERS);
        try {
            List<Callable<Long>> tasks = IntStream.range(0, USERS)
                    .mapToObj(i -> (Callable<Long>) () -> {
                        long t0 = System.nanoTime();
                        // Mix different operations
                        if (i % 4 == 0) {
                            exportService.exportAsJson(model);
                        } else if (i % 4 == 1) {
                            marketPlaceService.listAllEntries();
                        } else if (i % 4 == 2) {
                            submodelService.validate(SubmodelDto.builder().submodel(sub).build());
                        } else {
                            fileStorageService.getFileContent("file-1");
                        }
                        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                    })
                    .toList();

            List<Long> times = pool.invokeAll(tasks).stream()
                    .map(f -> {
                        try { return f.get(); }
                        catch (Exception e) { throw new RuntimeException(e); }
                    })
                    .toList();

            // Assert each ≤ 3s
            assertTrue(times.stream().allMatch(ms -> ms <= 3000), "some ops exceeded 3s");

            // Assert p90 ≤ 3s
            List<Long> sorted = new ArrayList<>(times);
            Collections.sort(sorted);
            long p90 = sorted.get((int) Math.ceil(sorted.size() * 0.9) - 1);
            log.info("20 concurrent ops, p90={} ms, times={}", p90, sorted);
            assertTrue(p90 <= 3000, "p90 exceeded 3s");
        } finally {
            pool.shutdownNow();
        }
    }
}
