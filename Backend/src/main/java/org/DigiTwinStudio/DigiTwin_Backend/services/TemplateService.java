package org.DigiTwinStudio.DigiTwin_Backend.services;


import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.TemplateMapper;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.TemplateDto;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TemplateRepository;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.integration.SMTRepoClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final SMTRepoClient smtRepoClient;
    private final TemplateMapper templateMapper;

    /**
     * Returns all templates as DTOs.
     *
     * @return list of TemplateDto for frontend selection
     * @throws RuntimeException if mapping fails
     */
    public List<TemplateDto> getAvailableTemplates() {
        return templateRepository.findAll().stream()
                .map(templateMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Loads a single template by its ID and returns it as a DTO,
     * including the embedded JSON definition.
     *
     * @param id the template ID
     * @return the TemplateDto
     * @throws NotFoundException if no template with the given ID exists
     */
    public TemplateDto getTemplateById(String id) {
        Template tpl = templateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Template not found: " + id));
        return templateMapper.toDto(tpl);
    }

    /**
     * Scheduled to work once a day.
     * Fetches the latest templates from the external SMT-Repository
     * and upserts them into the local database.
     * Each fetched ExternalTemplateDto is mapped to a domain Template,
     * its pulledAt timestamp is set to now.
     *
     * @throws RuntimeException if fetching or mapping fails
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Berlin") // Every day at 00:00
    public void syncTemplatesFromRepo() {
        log.info("syncTemplatesFromRepo");
        List<Template> fetchedTemplates = smtRepoClient.fetchTemplates();
        log.info("Fetched {} templates.", fetchedTemplates.size());

        int newCount = 0;
        int oldCount = 0;
        int updatedCount = 0;
        for (Template template : fetchedTemplates) {
            log.info("Fetched Template: {}", template.getName());
            if (this.templateRepository.findByName(template.getName()).isEmpty()) {
                // no template with this name exists locally
                newCount++;
                this.templateRepository.save(template);
                log.info("New Template: \"{}\" saved in database.", template.getName());
            } else {
                log.info("Template with name \"{}\" already exists. Checking for updated Version.", template.getName());

                // check for an updated version. only set newest one active=True, all others active=False
                Template localTemplate = this.templateRepository.findByName(template.getName()).get();
                int localTemplateVersion = Integer.parseInt(localTemplate.getVersion());
                int templateVersion = Integer.parseInt(template.getVersion());
                int localTemplateRevision = Integer.parseInt(localTemplate.getRevision());
                int templateRevision = Integer.parseInt(template.getRevision());

                if (templateVersion > localTemplateVersion || (localTemplateVersion == templateVersion && templateRevision > localTemplateRevision)) {
                    // delete old template
                    this.templateRepository.delete(localTemplate);
                    log.info("Deleted older Version of \"{}\" in database.", localTemplate.getName());


                    this.templateRepository.save(template);
                    log.info("Saved updated Version \"{}.{}\" (old: \"{}.{}\") in database.", templateVersion, templateRevision, localTemplateVersion, localTemplateRevision);
                    updatedCount++;

                } else {
                    // no changes
                    oldCount++;
                    log.info("Template in database is up to date. Skipping.");
                }

            }

        }
        log.info("Saved {} new templates in database.", newCount);
        log.info("Updated {} templates in database.", updatedCount);
        log.info("Kept {} old templates in database.", oldCount);
        log.info("Database holds {} templates", this.templateRepository.count());
   }

    /**
     * Resolves a domain Template by ID for reuse in Submodel instantiation.
     * Returns the Template entity (including its raw JSON) so that the
     * AAS4J adapter can parse it into a Submodel.
     *
     * @param templateId the ID of the template to resolve
     * @return the Template entity
     * @throws NotFoundException if no template with the given ID exists
     */
    public Template resolveTemplate(String templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));
    }

}
