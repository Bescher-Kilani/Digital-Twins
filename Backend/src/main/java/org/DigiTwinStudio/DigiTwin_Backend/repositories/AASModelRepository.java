package org.DigiTwinStudio.DigiTwin_Backend.repositories;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing aas model data.
 */
@Repository
public interface AASModelRepository extends MongoRepository<AASModel, String> {

    /**
     * Retrieves a model by its unique ID.
     *
     * @param id the unique identifier of the model
     * @return an Optional containing the found AASModel, or empty if not found
     */
    Optional<AASModel> findById(String id);

    /**
     * Returns all models that belong to a specific owner.
     *
     * @param ownerId the owner's user ID
     * @return a list of all models owned by the specified user
     */
    List<AASModel> findByOwnerId(String ownerId);

    /**
     * Finds all models for an owner that were created before a given timestamp.
     * Can be used for time-based cleanups (e.g., old guest models).
     *
     * @param ownerId   the owner's user ID
     * @param threshold the timestamp cutoff (only models created before this will be returned)
     * @return a list of matching models
     */
    List<AASModel> findByOwnerIdAndCreatedAtBefore(String ownerId, LocalDateTime threshold);

}
