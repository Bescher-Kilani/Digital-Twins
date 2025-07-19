package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TemplateRepository;
import org.slf4j.Logger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartUp implements ApplicationListener<ApplicationReadyEvent> {

    private final TemplateService templateService;
    private final TemplateRepository templateRepository;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Application is ready – initiate StartUp.");

        // ToDo: TEMPORARY FIX BEFORE TEMPLATE FETCH IMPLEMENTATION

        log.info("Reset Database");

        templateRepository.deleteAll();
        log.info("Database has {} templates.", templateRepository.count());
        log.info("Database has been deleted.");




        log.info("Fetch Templates");
        templateService.syncTemplatesFromRepo();

        log.info("StartUp completed.");
    }
}
