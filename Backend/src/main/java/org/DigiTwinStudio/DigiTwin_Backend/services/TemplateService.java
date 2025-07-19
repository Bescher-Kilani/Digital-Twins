package org.DigiTwinStudio.DigiTwin_Backend.services;


import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.TemplateMapper;
import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.TemplateDto;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.TemplateRepository;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.integration.SMTRepoClient;
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
     * Returns all active templates as DTOs.
     *
     * @return list of TemplateDto for frontend selection
     * @throws RuntimeException if mapping fails
     */
    public List<TemplateDto> getAvailableTemplates() {
        return templateRepository.findAll().stream()
                .filter(Template::isActive)
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
     * Fetches the latest templates from the external SMT-Repository
     * and upserts them into the local database.
     *
     * Each fetched ExternalTemplateDto is mapped to a domain Template,
     * its pulledAt timestamp is set to now, and it is marked active.
     *
     * @throws RuntimeException if fetching or mapping fails
     * @throws NotFoundException if accessing a local template-object fails
     */
    public void syncTemplatesFromRepo() {
        log.info("syncTemplatesFromRepo");
        List<Template> fetchedTemplates = smtRepoClient.fetchTemplates();
        log.info("Fetched {} templates.", fetchedTemplates.size());

        int newCount = 0;
        int oldCount = 0;
        int changedCount = 0;
        for (Template template : fetchedTemplates) {
            log.info("Fetched Template: {}", template.getName());
            Template localTemplate = isInTemplateRepository(template.getName());
            if (localTemplate != null) {
                // a template-object with this name is already present in local repository
                log.info("A template with the name \"{}\" already exists in local repository and has ID \"{}\" .", localTemplate.getName(), localTemplate.getId());

                // check if the version description or JSON have changed
                log.info("Check if something has changed...");
                boolean hasChanged = false;
                // version
                if (!localTemplate.getVersion().equalsIgnoreCase(template.getVersion())) {
                    hasChanged = true;
                    log.info("Version has changed. From old Version \"{}\" to new Version \"{}\".", localTemplate.getVersion(), template.getVersion());
                }
                // descriptions
                if (!localTemplate.getDescriptions().equals(template.getDescriptions())) {
                    hasChanged = true;
                    log.info("Descriptions have changed. From \"{}\" to \"{}\".", localTemplate.getDescriptions(), template.getDescriptions());
                }
                // JSON
                if (!localTemplate.getJson().equals(template.getJson())) {
                    hasChanged = true;
                    log.info("JSON has changed.");
                }
                if (hasChanged) {
                    // deactivate old template -> safe delete later
                    localTemplate.setActive(false);
                    // save changed Template
                    this.templateRepository.save(template);
                    changedCount++;
                } else {
                    log.info("Nothing changed.");
                    oldCount++;
                }

            } else {
                newCount++;
                templateRepository.save(template);
                log.info("New template: {} has been saved to local repository", template.getName());
            }
        }
        log.info("Saved {} new templates in database.", newCount);
        log.info("Saved {} changed templates in database.", changedCount);
        log.info("Kept {} old templates in database.", oldCount);
        log.info("Template-Repository has {} templates.", templateRepository.count());
        // ToDo: Scheduling.
        // ToDo: Nach Scheduling Logik implementieren, dass nur neue Templates (bzw Versionen) gespeichert werden, und der Rest ignoriert wird.
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

    /**
     *
     * @param templateName name of template
     * @return local template-object with the same name, else null
     */
    private Template isInTemplateRepository(String templateName) {
        List<Template> templatesInLocalRepo = this.templateRepository.findByActiveTrueOrderByNameAsc();
        for (Template template : templatesInLocalRepo) {
            if (template.getName().equalsIgnoreCase(templateName)) {
                return template;
            }
        }
        return null;
    }

}
