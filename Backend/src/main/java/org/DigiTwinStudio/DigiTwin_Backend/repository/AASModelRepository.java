package org.DigiTwinStudio.DigiTwin_Backend.repository;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AASModelRepository extends MongoRepository<AASModel, String> {

    List<AASModel> findByOwnerIdAndDeletedFalse(String ownerId);

    Optional<AASModel> findByIdAndDeletedFalse(String id);

    List<AASModel> findByPublishedTrueAndDeletedFalse();

    boolean existsByIdAndOwnerId(String id, String ownerId);
}
