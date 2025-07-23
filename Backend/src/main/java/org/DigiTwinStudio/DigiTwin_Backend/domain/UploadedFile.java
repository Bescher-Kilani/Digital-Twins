package org.DigiTwinStudio.DigiTwin_Backend.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("uploadedFiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedFile {

    @Id
    private String id;

    private String modelId; // id of the model that uses the file

    private String filename;

    private String contentType; // e.g., "application/json", "image/png"

    private long size; // Size in bytes

    private String storagePath; // GridFS-ID

    private String ownerId; // ID of the user who uploaded the file for authentication and authorization purposes

    private LocalDateTime uploadedAt;
}
