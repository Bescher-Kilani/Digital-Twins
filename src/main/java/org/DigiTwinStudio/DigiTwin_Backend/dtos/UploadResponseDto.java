package org.DigiTwinStudio.DigiTwin_Backend.dtos;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadResponseDto {

    private String fileId;

    private String filename;

    private String contentType;

    private long size;
}
