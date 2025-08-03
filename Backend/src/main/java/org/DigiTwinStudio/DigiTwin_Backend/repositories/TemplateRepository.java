package org.DigiTwinStudio.DigiTwin_Backend.repositories;

import lombok.NonNull;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing template entities.
 */
@Repository
public interface TemplateRepository extends MongoRepository<Template, String> {

    /**
     * Finds a template by its ID.
     *
     * @param id the template ID
     * @return an Optional containing the template if found
     */
    Optional<Template> findById(@NonNull String id);

    /**
     * Finds a template by its name.
     *
     * @param name the template name
     * @return an Optional containing the template if found
     */
    Optional<Template> findByName(String name);
}