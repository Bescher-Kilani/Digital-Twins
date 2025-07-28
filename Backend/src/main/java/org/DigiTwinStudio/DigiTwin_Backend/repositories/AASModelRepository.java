package org.DigiTwinStudio.DigiTwin_Backend.repositories;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AASModelRepository extends MongoRepository<AASModel, String> {

    Optional<AASModel> findById(String id);

    // None deleted models for a specific owner
    List<AASModel> findByOwnerIdAndDeletedFalse(String ownerId);

    // specific model by ID
    Optional<AASModel> findByIdAndDeletedFalse(String id);

    // published models that are not deleted
    List<AASModel> findByPublishedTrueAndDeletedFalse();

    // ownership check for a specific model
    boolean existsByIdAndOwnerId(String id, String ownerId);

    /**
     * Retrieves all AAS models that belong to the specified owner, are not marked as deleted,
     * and were created before the given timestamp.
     *
     * <p>This is typically used to find outdated guest models for automatic cleanup.</p>
     *
     * @param ownerId the ID of the model owner (e.g. "GUEST")
     * @param threshold the cutoff timestamp; only models created before this will be returned
     * @return a list of matching {@link AASModel} entities
     */
    List<AASModel> findByOwnerIdAndDeletedFalseAndCreatedAtBefore(String ownerId, LocalDateTime threshold);

}
