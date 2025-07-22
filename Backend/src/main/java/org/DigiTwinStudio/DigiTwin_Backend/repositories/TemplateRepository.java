package org.DigiTwinStudio.DigiTwin_Backend.repositories;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends MongoRepository<Template, String> {

    // retrieves all active templates, sorted by name
    List<Template> findByActiveTrueOrderByNameAsc();

    // tries to find a template by its id, if it is active, else returns empty
    Optional<Template> findByIdAndActiveTrue(String id);


    Optional<Template> findByNameAndActiveTrue(String name);
}