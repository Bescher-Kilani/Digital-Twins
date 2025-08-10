package org.DigiTwinStudio.DigiTwin_Backend.init;

import com.mongodb.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TagRepository;
import org.DigiTwinStudio.DigiTwin_Backend.services.TemplateService;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

/**
 * Runs template and tag initialization when the application starts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartUp implements ApplicationListener<ApplicationReadyEvent> {

    private final TemplateService templateService;
    private final TagRepository tagRepository;

    /**
     * Called when the application is ready; initializes templates and tags.
     *
     * @param event the ready event
     */
    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        log.info("Application is ready – initiate StartUp.");

        // TEMPORARY SOLUTION TO RESET TEMPLATE AND TAG DATABASE
        //this.templateRepository.deleteAll();
        //log.info("Deleted all templates.");
        //this.tagRepository.deleteAll();
        //log.info("Deleted all tags.");

        // INITIALIZATION
        log.info("Initialize templates.");
        try {
            templateService.syncTemplatesFromRepo();
        } catch (RuntimeException e) {
            log.error("Template sync failed", e);
            log.warn("Templates may be out of date or not initialized!");
        }

        log.info("Initialize tags.");
        try {
            initializeTags(new ClassPathResource("tags.txt"));
        } catch (RuntimeException e) {
            log.error("Tag initialization failed", e);
            log.warn("Tags may be out of date or not initialized!");
        }
        log.info("StartUp completed.");
    }

    /**
     * Loads tags from the tags.txt resource file and saves new tags to the database.
     * Existing tags are not modified.
     */
    void initializeTags(ClassPathResource resource) {
        try {
            log.info("Initializing Tags from {}", resource.getFilename());
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            Stream<String> tagStream = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty());

            int oldCount = (int) tagRepository.count();

            tagStream.forEach(line -> {
                String[] parts = line.split(":", 2);
                String tagName = parts[0].trim();
                String category = parts.length > 1 ? parts[1].trim() : "";

                tagRepository.findByNameIgnoreCase(tagName)
                        .orElseGet(() -> {
                            Tag tag = Tag.builder()
                                    .name(tagName)
                                    .category(category)
                                    .usageCount(0)
                                    .build();
                            log.info("Adding tag: {} (Category: {})", tagName, category);
                            return tagRepository.save(tag);
                        });
            });

            log.info("Added {} new Tags.", tagRepository.count() - oldCount);
            log.info("Tag initialization complete.");
        } catch (Exception e) {
            log.error("Failed to initialize tags from file", e);
        }
    }
}
