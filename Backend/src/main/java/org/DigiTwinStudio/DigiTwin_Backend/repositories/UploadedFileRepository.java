package org.DigiTwinStudio.DigiTwin_Backend.repositories;

import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing uploaded file entities.
 */
@Repository
public interface UploadedFileRepository extends MongoRepository<UploadedFile, String> {

    /**
     * Finds all uploaded files by model ID.
     *
     * @param modelId the model ID
     * @return list of uploaded files for the given model
     */
    List<UploadedFile> findAllByModelId(String modelId);
}
