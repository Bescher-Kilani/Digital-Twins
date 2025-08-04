package org.DigiTwinStudio.DigiTwin_Backend.domain;

import lombok.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entity representing an uploaded file and its metadata.
 */
@Document("uploadedFiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedFile {

    @Id
    private String id;

    // id of the model that uses the file
    private String modelId;

    private String filename;

    // e.g., "application/json", "image/png"
    private String contentType;

    // Size in bytes
    private long size;

    // GridFS-ID
    private String storagePath;

    // ID of the user who uploaded the file for authentication and authorization purposes
    private String ownerId;

    private LocalDateTime uploadedAt;
}
