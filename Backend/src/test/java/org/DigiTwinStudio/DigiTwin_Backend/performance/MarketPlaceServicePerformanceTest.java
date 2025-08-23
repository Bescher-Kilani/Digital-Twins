package org.DigiTwinStudio.DigiTwin_Backend.performance;

import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.domain.MarketplaceEntry;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceEntryDto;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.MarketplaceSearchRequest;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.MarketplaceMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.MarketPlaceEntryRepository;
import org.DigiTwinStudio.DigiTwin_Backend.services.MarketPlaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
class MarketplaceServicePerformanceTest {

    private static final Duration FAST_3S = Duration.ofSeconds(3); // WK60
    private static final int USERS = 20;                           // WK90-lite

    @InjectMocks
    private MarketPlaceService marketPlaceService;

    @Mock private MarketPlaceEntryRepository entryRepository;
    @Mock private MarketplaceMapper mapper;
    @Mock private MongoTemplate mongoTemplate;

    // testing WK60: listAllEntries ≤ 3s
    @Test
    void listAllEntries_under3s() {
        // Arrange: repo returns 500 entries
        List<MarketplaceEntry> entries = IntStream.range(0, 500)
                .mapToObj(i -> MarketplaceEntry.builder().id("id-" + i).name("name-" + i).build())
                .toList();
        when(entryRepository.findAll()).thenReturn(entries);
        when(mapper.toDto(any())).thenAnswer(inv -> {
            MarketplaceEntry e = inv.getArgument(0);
            return new MarketplaceEntryDto(e.getId(), e.getName(), null, null, null, null, 0);
        });

        assertWithin(FAST_3S,
                () -> {
                    List<MarketplaceEntryDto> result = marketPlaceService.listAllEntries();
                    assertEquals(500, result.size());
                },
                "listAllEntries");
    }

    // testing WK60: search by single tag ≤ 3s
    @Test
    void searchBySingleTag_under3s() {
        MarketplaceSearchRequest req = new MarketplaceSearchRequest();
        req.setTagIds(List.of("battery"));

        // Return one real entry so mapper is used
        List<MarketplaceEntry> entries = List.of(MarketplaceEntry.builder().id("id-1").name("entry").build());
        when(mongoTemplate.find(any(), eq(MarketplaceEntry.class))).thenReturn(entries);
        when(mapper.toDto(any())).thenAnswer(inv -> {
            MarketplaceEntry e = inv.getArgument(0);
            return new MarketplaceEntryDto(e.getId(), e.getName(), null, null, null, null, 0);
        });

        assertWithin(FAST_3S,
                () -> {
                    List<MarketplaceEntryDto> result = marketPlaceService.search(req);
                    assertEquals(1, result.size());
                },
                "search(single tag)");
    }

    // testing WK60: search by multiple tags ≤ 3s
    @Test
    void searchByMultipleTags_under3s() {
        MarketplaceSearchRequest req = new MarketplaceSearchRequest();
        req.setTagIds(List.of("battery", "iec-61406"));

        // Return multiple entries so mapper is used
        List<MarketplaceEntry> entries = IntStream.range(0, 10)
                .mapToObj(i -> MarketplaceEntry.builder().id("id-" + i).name("entry-" + i).build())
                .toList();
        when(mongoTemplate.find(any(), eq(MarketplaceEntry.class))).thenReturn(entries);
        when(mapper.toDto(any())).thenAnswer(inv -> {
            MarketplaceEntry e = inv.getArgument(0);
            return new MarketplaceEntryDto(e.getId(), e.getName(), null, null, null, null, 0);
        });

        assertWithin(FAST_3S,
                () -> {
                    List<MarketplaceEntryDto> result = marketPlaceService.search(req);
                    assertEquals(10, result.size());
                },
                "search(multiple tags)");
    }

    // testing WK90-lite: 20 concurrent list/search ops, 90% ≤ 3s
    @Test
    void concurrentListAndSearch_p90Under3s() throws Exception {
        List<MarketplaceEntry> entries = IntStream.range(0, 200)
                .mapToObj(i -> MarketplaceEntry.builder().id("id-" + i).name("name-" + i).build())
                .toList();

        when(entryRepository.findAll()).thenReturn(entries);
        when(mongoTemplate.find(any(), eq(MarketplaceEntry.class))).thenReturn(entries);
        when(mapper.toDto(any())).thenAnswer(inv -> {
            MarketplaceEntry e = inv.getArgument(0);
            return new MarketplaceEntryDto(e.getId(), e.getName(), null, null, null, null, 0);
        });

        MarketplaceSearchRequest req = new MarketplaceSearchRequest();
        req.setTagIds(List.of("battery"));

        ExecutorService pool = Executors.newFixedThreadPool(USERS);
        try {
            List<Callable<Long>> tasks = new ArrayList<>();
            for (int i = 0; i < USERS; i++) {
                int finalI = i;
                tasks.add(() -> {
                    long t0 = System.nanoTime();
                    if (finalI % 2 == 0) {
                        marketPlaceService.listAllEntries();
                    } else {
                        marketPlaceService.search(req);
                    }
                    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                });
            }

            List<Future<Long>> futures = pool.invokeAll(tasks);
            List<Long> times = new ArrayList<>(USERS);
            for (Future<Long> f : futures) {
                times.add(f.get());
            }

            // each ≤ 3s and p90 ≤ 3s
            assertTrue(times.stream().allMatch(ms -> ms <= 3000), "some ops exceeded 3s");
            times.sort(Long::compareTo);
            long p90 = times.get((int)Math.ceil(times.size() * 0.9) - 1);
            log.info("concurrent list/search p90={} ms, times={}", p90, times);
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
