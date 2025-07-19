package org.DigiTwinStudio.DigiTwin_Backend.repositories;

import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedFileRepository extends MongoRepository<UploadedFile, String> {

    // retrieves all files uploaded by a specific owner
    List<UploadedFile> findByOwnerId(String ownerId);

    // retrieves a specific file by its ID and owner ID
    Optional<UploadedFile> findByIdAndOwnerId(String id, String ownerId);

    // ToDo: Mabye add ownerId for verification
    List<UploadedFile> findAllByModelId(String modelId);


    // checks if a file exists by its storage path
    boolean existsByStoragePath(String path);
}
