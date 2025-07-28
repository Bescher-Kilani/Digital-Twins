package org.DigiTwinStudio.DigiTwin_Backend.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TagRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TemplateRepository;
import org.DigiTwinStudio.DigiTwin_Backend.services.TemplateService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartUp implements ApplicationListener<ApplicationReadyEvent> {

    private final TemplateService templateService;
    private final TemplateRepository templateRepository;
    private final TagRepository tagRepository;

    /**
     * Handles the {@link ApplicationReadyEvent}, which is triggered when the Spring Boot application has fully started.
     * <p>
     * This method is used to perform application-level startup tasks such as initializing templates and tags.
     * <p>
     * The current steps include:
     * <ul>
     *     <li>Syncing templates from a remote or local repository via {@code templateService.syncTemplatesFromRepo()}</li>
     *     <li>Initializing tags from a predefined file using {@code initializeTags()}</li>
     * </ul>
     *
     * <p>
     * Optional database reset logic is included but commented out, intended for development or temporary fixes.
     *
     * <p>
     * Logging is used to trace the execution and state at each step.
     *
     * @param event the Spring {@link ApplicationReadyEvent} signaling the application is fully initialized
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Application is ready – initiate StartUp.");

        // ToDo: TEMPORARY FIX BEFORE TEMPLATE FETCH IMPLEMENTATION
        /*
        log.info("Reset Database");

        templateRepository.deleteAll();
        log.info("Database has {} templates.", templateRepository.count());
        log.info("Database has been deleted.");
        */
        log.info("Initialize templates.");
        templateService.syncTemplatesFromRepo();

        log.info("Initialize tags.");
        initializeTags();

        log.info("StartUp completed.");
    }

    /**
     * Initializes the MongoDB 'tags' collection with entries from the {@code tags.txt} file located in the classpath.
     * <p>
     * Each non-empty line in the file is treated as a tag name. If a tag with the same name (case-insensitive)
     * does not already exist in the database, it will be created and saved with:
     * <ul>
     *     <li>an empty {@code category}</li>
     *     <li>{@code usageCount} set to {@code 0}</li>
     * </ul>
     * <p>
     * The method logs the number of tags added during initialization and handles errors gracefully
     * by logging them without crashing the application.
     * <p>
     * Example content of {@code tags.txt}:
     * <pre>
     *     AI
     *     Simulation
     *     IoT
     * </pre>
     *
     * <p><b>Note:</b> Existing tags in the database will not be modified or duplicated.
     */
    private void initializeTags() {
        try {
            log.info("Initializing Tags from tags.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource("tags.txt").getInputStream(), StandardCharsets.UTF_8));

            Stream<String> tagStream = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty());

            int oldCount = (int) tagRepository.count();
            tagStream.forEach(tagName -> tagRepository.findByNameIgnoreCase(tagName)
                    .orElseGet(() -> {
                        Tag tag = Tag.builder()
                                .name(tagName)
                                .category("")       // Leer beim Start
                                .usageCount(0)      // Startwert 0
                                .build();
                        log.info("Adding tag {}", tag);
                        return tagRepository.save(tag);
                    }));

            log.info("Tag initialization complete.");
            log.info("Added {} new Tags.", tagRepository.count() - oldCount);
        } catch (Exception e) {
            log.error("Failed to initialize tags from file", e);
        }
    }
}
