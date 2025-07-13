package org.DigiTwinStudio.DigiTwin_Backend.services;


import java.util.List;
import java.util.stream.Collectors;

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
     */
    public void syncTemplatesFromRepo() {
        List<Template> fetchedTemplates = smtRepoClient.fetchTemplates();
        templateRepository.saveAll(fetchedTemplates);
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
}
