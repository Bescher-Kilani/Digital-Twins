package org.DigiTwinStudio.DigiTwin_Backend.repository;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Template;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends MongoRepository<Template, String> {

    List<Template> findByActiveTrueOrderByNameAsc();

    Optional<Template> findByIdAndActiveTrue(String id);
}
