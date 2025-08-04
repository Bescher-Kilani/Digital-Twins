package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for the response after a successful file upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadResponseDto {

    private String fileId;

    private String filename;

    private String contentType;

    private long size;

    private LocalDateTime uploadedAt;
}
