package org.DigiTwinStudio.DigiTwin_Backend.performance;

import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.adapter.AAS4jAdapter;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.domain.MarketplaceEntry;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.MarketPlaceEntryRepository;
import org.DigiTwinStudio.DigiTwin_Backend.services.ExportService;
import org.DigiTwinStudio.DigiTwin_Backend.services.FileStorageService;
import org.DigiTwinStudio.DigiTwin_Backend.util.TestModelFactory;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class ExportServicePerformanceTest {

    @InjectMocks
    private ExportService exportService;

    @Mock
    private AASModelRepository aasModelRepository;
    @Mock
    private MarketPlaceEntryRepository marketPlaceEntryRepository;
    @Mock
    private AAS4jAdapter aas4jAdapter;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private AASModelMapper aasModelMapper;

    // NFR thresholds
    private static final Duration FAST_RESPONSE_THRESHOLD = Duration.ofSeconds(3);  // WK60
    private static final Duration MAX_EXPORT_TIME = Duration.ofSeconds(15);         // WK80
    private static final Duration JSON_FAST_THRESHOLD = Duration.ofMillis(500);

    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_MODEL_ID = "test-model-id-123";
    private static final String TEST_ENTRY_ID = "test-marketplace-entry-123";

    private AASModel testModel;

    @BeforeEach
    void setup() throws Exception {
        testModel = new AASModel();
        testModel.setId(TEST_MODEL_ID);
        testModel.setOwnerId(TEST_USER_ID);

        lenient().when(aas4jAdapter.aasModelToDefaultEnvironment(any(AASModel.class)))
                .thenReturn(new DefaultEnvironment());

        lenient().when(aas4jAdapter.serializeToJsonString(any()))
                .thenReturn("{\"ok\":true}");

        lenient().doAnswer(inv -> {
            OutputStream os = inv.getArgument(2, OutputStream.class);
            os.write(new byte[]{1,2,3}); // fake AASX content
            return null;
        }).when(aas4jAdapter).serializeToAASX(any(), any(), any());

        lenient().when(fileStorageService.getInMemoryFilesByModelId(any()))
                .thenReturn(Collections.emptyList());

        lenient().when(aasModelMapper.fromDto(any(), any()))
                .thenReturn(testModel);
    }

    // testing exportAsJson Performance
    @Test void exportAsJson_fast() {
        assertExecutesWithin(JSON_FAST_THRESHOLD,
                () -> exportService.exportAsJson(testModel),
                "exportAsJson");
    }

    // testing exportAsAasx Performance
    @Test void exportAsAasx_under15s() {
        assertExecutesWithin(MAX_EXPORT_TIME,
                () -> exportService.exportAsAasx(testModel),
                "exportAsAasx");
    }

    // testing exportStoredModel Performance
    @Test void exportStoredModel_json_under3s() {
        when(aasModelRepository.findById(TEST_MODEL_ID)).thenReturn(Optional.of(testModel));
        assertExecutesWithin(FAST_RESPONSE_THRESHOLD,
                () -> exportService.exportStoredModel(TEST_MODEL_ID, ExportFormat.JSON, TEST_USER_ID),
                "exportStoredModel(JSON)");
    }

    @Test void exportStoredModel_aasx_under15s() {
        when(aasModelRepository.findById(TEST_MODEL_ID)).thenReturn(Optional.of(testModel));
        assertExecutesWithin(MAX_EXPORT_TIME,
                () -> exportService.exportStoredModel(TEST_MODEL_ID, ExportFormat.AASX, TEST_USER_ID),
                "exportStoredModel(AASX)");
    }

    // testing exportTransientModel Performance
    @Test void exportTransientModel_json_under3s() {
        assertExecutesWithin(FAST_RESPONSE_THRESHOLD,
                () -> exportService.exportTransientModel(new AASModelDto(), ExportFormat.JSON),
                "exportTransientModel(JSON)");
    }

    @Test void exportTransientModel_aasx_under15s() {
        assertExecutesWithin(MAX_EXPORT_TIME,
                () -> exportService.exportTransientModel(new AASModelDto(), ExportFormat.AASX),
                "exportTransientModel(AASX)");
    }

    // testing exportMarketplaceModel Performance
    @Test void exportMarketplaceModel_json_under3s() {
        MarketplaceEntry entry = new MarketplaceEntry();
        entry.setId(TEST_MODEL_ID);
        when(marketPlaceEntryRepository.findById(TEST_ENTRY_ID)).thenReturn(Optional.of(entry));
        when(aasModelRepository.findById(TEST_MODEL_ID)).thenReturn(Optional.of(testModel));

        assertExecutesWithin(FAST_RESPONSE_THRESHOLD,
                () -> exportService.exportMarketplaceModel(TEST_ENTRY_ID, ExportFormat.JSON),
                "exportMarketplaceModel(JSON)");
    }

    @Test void exportMarketplaceModel_aasx_under15s() {
        MarketplaceEntry entry = new MarketplaceEntry();
        entry.setId(TEST_MODEL_ID);
        when(marketPlaceEntryRepository.findById(TEST_ENTRY_ID)).thenReturn(Optional.of(entry));
        when(aasModelRepository.findById(TEST_MODEL_ID)).thenReturn(Optional.of(testModel));

        assertExecutesWithin(MAX_EXPORT_TIME,
                () -> exportService.exportMarketplaceModel(TEST_ENTRY_ID, ExportFormat.AASX),
                "exportMarketplaceModel(AASX)");
    }

    // testing export method Performance
    @Test void export_json_under3s() {
        when(aasModelRepository.findById(TEST_MODEL_ID)).thenReturn(Optional.of(testModel));
        assertExecutesWithin(FAST_RESPONSE_THRESHOLD,
                () -> exportService.export(TEST_MODEL_ID, "file", ExportFormat.JSON, TEST_USER_ID),
                "export(JSON)");
    }

    @Test void export_aasx_under15s() {
        when(aasModelRepository.findById(TEST_MODEL_ID)).thenReturn(Optional.of(testModel));
        assertExecutesWithin(MAX_EXPORT_TIME,
                () -> exportService.export(TEST_MODEL_ID, "file", ExportFormat.AASX, TEST_USER_ID),
                "export(AASX)");
    }

    // helper method
    private void assertExecutesWithin(Duration limit, Runnable action, String label) {
        Instant start = Instant.now();
        action.run();
        Duration dur = Duration.between(start, Instant.now());
        log.info("{} completed in {} ms (limit {} ms)", label, dur.toMillis(), limit.toMillis());
        assertTrue(dur.compareTo(limit) <= 0,
                String.format("%s took %d ms, should be ≤ %d ms",
                        label, dur.toMillis(), limit.toMillis()));
    }

    // testing end-to-end Performance
    @Test
    void exportLargeModel_asJson_under3s() {
        // Arrange: build a large synthetic model with many submodels & properties
        AASModel largeModel = TestModelFactory.buildLargeTestModel(10, 200, TEST_USER_ID);

        // Act + Assert: JSON export should be very fast
        assertExecutesWithin(FAST_RESPONSE_THRESHOLD,
                () -> {
                    byte[] result = exportService.exportAsJson(largeModel);
                    assertNotNull(result);
                    assertTrue(result.length > 0);
                },
                "E2E exportAsJson");
    }

    @Test
    void exportLargeModel_asAasx_under15s() {
        // Arrange: build a large synthetic model with many submodels & properties
        AASModel largeModel = TestModelFactory.buildLargeTestModel(10, 200, TEST_USER_ID);

        // Act + Assert: AASX export should complete within 15s
        assertExecutesWithin(MAX_EXPORT_TIME,
                () -> {
                    byte[] result = exportService.exportAsAasx(largeModel);
                    assertNotNull(result);
                    assertTrue(result.length > 0);
                    log.info("E2E AASX export size: {} bytes", result.length);
                },
                "E2E exportAsAasx");
    }
}
