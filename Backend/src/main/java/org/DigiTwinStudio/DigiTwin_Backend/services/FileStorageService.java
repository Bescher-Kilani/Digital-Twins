package org.DigiTwinStudio.DigiTwin_Backend.services;

import com.mongodb.client.gridfs.model.GridFSFile;

import lombok.RequiredArgsConstructor;

import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.UploadedFileRepository;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.FileStorageException;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.aasx.InMemoryFile;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.web.multipart.MultipartFile;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import org.bson.types.ObjectId;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;

/**
 * Handles storing, loading, and deleting uploaded files in MongoDB GridFS.
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final UploadedFileRepository uploadedFileRepository;
    private final GridFsTemplate gridFsTemplate;

    /**
     * Stores the uploaded file in MongoDB GridFS and saves metadata in a separate Mongo collection.
     *
     * @param file    the uploaded file (PDF, JSON, etc.)
     * @param ownerId the ID of the user who is uploading the file
     * @return UploadedFile metadata object saved in MongoDB
     */
    public UploadedFile store(MultipartFile file, String ownerId, String modelId) {
        try {
            // Generate a random file ID for your domain logic
            String fileId = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();

            // Save the binary file into MongoDB GridFS
            ObjectId gridFsId = gridFsTemplate.store(
                    file.getInputStream(),
                    originalFilename,
                    file.getContentType()
            );

            // Create and save your domain-level metadata object
            UploadedFile uploadedFile = UploadedFile.builder()
                    .id(fileId)
                    .modelId(modelId)
                    .filename(originalFilename)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .ownerId(ownerId)
                    .storagePath(gridFsId.toHexString())  // link to GridFS file ID
                    .uploadedAt(LocalDateTime.now())
                    .build();

            return uploadedFileRepository.save(uploadedFile);

        } catch (IOException e) {
            throw new FileStorageException("Could not store file in GridFS", e);
        }
    }

    /**
     * Deletes the file from GridFS and its metadata if the current user is the owner.
     *
     * @param fileId  the metadata ID stored in your UploadedFile document
     * @param ownerId the ID of the authenticated user
     */
    public void delete(String fileId, String ownerId) {
        // Load metadata document first
        UploadedFile file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new FileStorageException("File not found: " + fileId));

        // Check if the current user is the file owner
        if (!file.getOwnerId().equals(ownerId)) {
            throw new FileStorageException("Access denied: not the file owner");
        }

        try {
            // Delete binary data from GridFS using the ObjectId
            ObjectId gridFsId = new ObjectId(file.getStoragePath());
            gridFsTemplate.delete(query(where("_id").is(gridFsId)));

            // Delete metadata from your MongoDB collection
            uploadedFileRepository.deleteById(fileId);

        } catch (Exception e) {
            throw new FileStorageException("Error deleting file from GridFS", e);
        }
    }

    /**
     * Retrieves all uploaded files for the given model and loads them as {@link InMemoryFile} objects from GridFS.
     *
     * @param modelId the model ID whose files are to be loaded
     * @return list of {@link InMemoryFile} for use in AASX export
     * @throws RuntimeException if a file cannot be read from GridFS
     */
    public List<InMemoryFile> getInMemoryFilesByModelId(String modelId) {
        List<UploadedFile> uploadedFiles = uploadedFileRepository.findAllByModelId(modelId);
        List<InMemoryFile> inMemoryFiles = new ArrayList<>();

        for (UploadedFile file : uploadedFiles) {
            try {
                ObjectId gridFsId = new ObjectId(file.getStoragePath());
                GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(gridFsId)));

                if (gridFSFile != null) {
                    GridFsResource resource = gridFsTemplate.getResource(gridFSFile);
                    byte[] content = resource.getInputStream().readAllBytes();

                    // Verwende einen standardisierten Pfad z. B. "aasx/mydoc.pdf"
                    String aasxPath = "aasx/" + file.getFilename();

                    inMemoryFiles.add(new InMemoryFile(content, aasxPath));
                } else {
                    System.err.println("GridFS file not found for ID: " + file.getStoragePath());
                }
            } catch (IOException | IllegalArgumentException e) {
                throw new RuntimeException("Error reading file content from GridFS for ID: " + file.getStoragePath(), e);
            }
        }

        return inMemoryFiles;
    }
}
