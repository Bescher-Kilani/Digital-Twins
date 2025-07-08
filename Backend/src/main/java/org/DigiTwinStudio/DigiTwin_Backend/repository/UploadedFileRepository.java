package org.DigiTwinStudio.DigiTwin_Backend.repository;

import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedFileRepository extends MongoRepository<UploadedFile, String> {

    List<UploadedFile> findByOwnerId(String ownerId);

    Optional<UploadedFile> findByIdAndOwnerId(String id, String ownerId);

    boolean existsByStoragePath(String path);
}
