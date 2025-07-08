package org.DigiTwinStudio.DigiTwin_Backend.repository;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends MongoRepository<Tag, String> {

    Optional<Tag> findByName(String name);

    List<Tag> findByNameContainingIgnoreCase(String keyword);

    List<Tag> findByIdIn(List<String> ids);
}
