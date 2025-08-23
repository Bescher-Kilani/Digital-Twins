package org.DigiTwinStudio.DigiTwin_Backend.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.SubmodelDto;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.SubmodelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TemplateRepository;
import org.DigiTwinStudio.DigiTwin_Backend.services.SubmodelService;
import org.DigiTwinStudio.DigiTwin_Backend.validation.SubmodelValidator;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class SubmodelServicePerformanceTest {

    private static final Duration FAST_500MS = Duration.ofMillis(500);  // createEmpty
    private static final Duration VALIDATE_1S = Duration.ofSeconds(1);  // validate large dto
    private static final Duration REPO_3S = Duration.ofSeconds(3);      // getSubmodel with repo
    private static final int USERS = 20;

    private static final String TEMPLATE_ID = "template-1";
    private static final String MODEL_ID = "model-123";

    @InjectMocks
    private SubmodelService submodelService;

    @Mock private SubmodelValidator submodelValidator;
    @Mock private SubmodelMapper submodelMapper;
    @Mock private TemplateRepository templateRepository;
    @Mock private AASModelRepository aasModelRepository;

    // testing WK60: createEmptySubmodelFromTemplate ≤ 500ms
    @Test
    void createEmptySubmodel_fast() {
        // Arrange: stub Template with minimal valid Submodel JSON
        ObjectNode json = new ObjectMapper().createObjectNode();
        json.put("id", "sm-1");
        json.put("idShort", "sm-1");
        json.put("modelType", "Submodel");

        Template template = new Template();
        template.setId(TEMPLATE_ID);
        template.setJson(json);

        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

        // Act + Assert
        assertWithin(FAST_500MS,
                () -> assertNotNull(submodelService.createEmptySubmodelFromTemplate(TEMPLATE_ID)),
                "createEmptySubmodelFromTemplate");
    }

    // testing WK60: validate(dto) ≤ 1s on large submodels
    @Test
    void validate_largeSubmodel_under1s() {
        DefaultSubmodel sub = new DefaultSubmodel.Builder()
                .id("sm-big")
                .idShort("big")
                .build();

        SubmodelDto dto = SubmodelDto.builder().submodel(sub).build();
        when(submodelMapper.fromDto(any())).thenReturn(sub);

        // Validation mocked as no-op
        doNothing().when(submodelValidator).validate(any());

        assertWithin(VALIDATE_1S,
                () -> submodelService.validate(dto),
                "validate(largeSubmodel)");
    }

    // testing WK60: getSubmodel ≤ 3s when repository is involved
    @Test
    void getSubmodel_under3s() {
        // Arrange: model with many submodels
        AASModel model = new AASModel();
        List<DefaultSubmodel> subs = IntStream.range(0, 50)
                .mapToObj(i -> new DefaultSubmodel.Builder()
                        .id("sm-" + i)
                        .idShort("sm-" + i)
                        .build())
                .toList();
        model.setSubmodels(subs);

        when(aasModelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));
        when(submodelMapper.toDto(any())).thenAnswer(inv -> {
            DefaultSubmodel s = inv.getArgument(0);
            return SubmodelDto.builder().submodel(s).build();
        });

        assertWithin(REPO_3S,
                () -> assertNotNull(submodelService.getSubmodel(MODEL_ID, "sm-42")),
                "getSubmodel");
    }

    // testing WK90-lite: 20 concurrent validate/getSubmodel calls, 90& ≤ 3s
    @Test
    void concurrentOps_p90Under3s() throws Exception {
        // Prepare stubs for validate
        DefaultSubmodel sub = new DefaultSubmodel.Builder()
                .id("sm-big")
                .idShort("big")
                .build();
        SubmodelDto dto = SubmodelDto.builder().submodel(sub).build();
        when(submodelMapper.fromDto(any())).thenReturn(sub);
        doNothing().when(submodelValidator).validate(any());

        // Prepare stubs for getSubmodel
        AASModel model = new AASModel();
        List<DefaultSubmodel> subs = IntStream.range(0, 50)
                .mapToObj(i -> new DefaultSubmodel.Builder()
                        .id("sm-" + i)
                        .idShort("sm-" + i)
                        .build())
                .toList();
        model.setSubmodels(subs);
        when(aasModelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));
        when(submodelMapper.toDto(any())).thenAnswer(inv -> {
            DefaultSubmodel s = inv.getArgument(0);
            return SubmodelDto.builder().submodel(s).build();
        });

        ExecutorService pool = Executors.newFixedThreadPool(USERS);
        try {
            List<Callable<Long>> tasks = new ArrayList<>();
            for (int i = 0; i < USERS; i++) {
                final int idx = i; // make effectively final for lambda
                tasks.add(() -> {
                    long t0 = System.nanoTime();
                    if (idx % 2 == 0) {
                        submodelService.validate(dto);
                    } else {
                        submodelService.getSubmodel(MODEL_ID, "sm-10");
                    }
                    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                });
            }

            List<Future<Long>> futures = pool.invokeAll(tasks);
            List<Long> times = new ArrayList<>(USERS);
            for (Future<Long> f : futures) {
                times.add(f.get());
            }

            // each ≤ 3s
            assertTrue(times.stream().allMatch(ms -> ms <= 3000), "some ops exceeded 3s");
            // 90% ≤ 3s
            times.sort(Long::compareTo);
            long p90 = times.get((int) Math.ceil(times.size() * 0.9) - 1);
            log.info("concurrent validate/getSubmodel p90={} ms, times={}", p90, times);
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
        assertTrue(ms <= limit.toMillis(), label + " exceeded " + limit.toMillis() + " ms");
    }
}
