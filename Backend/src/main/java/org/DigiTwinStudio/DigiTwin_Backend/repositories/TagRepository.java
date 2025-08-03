package org.DigiTwinStudio.DigiTwin_Backend.repositories;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing tag entities.
 */
@Repository
public interface TagRepository extends MongoRepository<Tag, String> {

    /**
     * Finds a tag by name, ignoring case.
     *
     * @param name the tag name
     * @return an Optional containing the tag if found
     */
    Optional<Tag> findByNameIgnoreCase(String name);

    /**
     * Finds all tags matching the given IDs.
     *
     * @param ids the list of tag IDs
     * @return list of matching tags
     */
    List<Tag> findByIdIn(List<String> ids);
}
