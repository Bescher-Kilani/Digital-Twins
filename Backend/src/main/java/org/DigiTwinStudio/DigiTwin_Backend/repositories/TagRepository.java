package org.DigiTwinStudio.DigiTwin_Backend.repositories;

import org.DigiTwinStudio.DigiTwin_Backend.domain.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends MongoRepository<Tag, String> {

    // ToDo: pageable queries for large datasets

    // Find tags by their name
    Optional<Tag> findByNameIgnoreCase(String name);

    // Find tags by their name, ignoring case, e.g., for search functionality
    List<Tag> findByNameContainingIgnoreCase(String keyword);

    // Find tags by their IDs, e.g., from the list in publishMetadata
    List<Tag> findByIdIn(List<String> ids);

    List<Tag> findByCategoryIgnoreCase(String category);
}
