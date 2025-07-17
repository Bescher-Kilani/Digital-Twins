package org.DigiTwinStudio.DigiTwin_Backend.repositories;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AASModelRepository extends MongoRepository<AASModel, String> {

    Optional<AASModel> findById(String id);

    // None deleted models for a specific owner
    List<AASModel> findByOwnerIdAndDeletedFalse(String ownerId);

    // Finds a model by ID, owner, and ensures it's not deleted
    Optional<AASModel> findByIdAndOwnerIdAndDeletedFalse(String id, String ownerId);

    // specific model by ID
    Optional<AASModel> findByIdAndDeletedFalse(String id);

    // published models that are not deleted
    List<AASModel> findByPublishedTrueAndDeletedFalse();

    // ownership check for a specific model
    boolean existsByIdAndOwnerId(String id, String ownerId);
}
