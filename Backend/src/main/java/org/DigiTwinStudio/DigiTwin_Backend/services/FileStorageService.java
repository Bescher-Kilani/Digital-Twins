package org.DigiTwinStudio.DigiTwin_Backend.services;

import io.adminshell.aas.v3.dataformat.aasx.InMemoryFile;
import lombok.RequiredArgsConstructor;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.UploadedFileRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final UploadedFileRepository uploadedFileRepository;

    /**
     * returns a list of InMemoryFiles of all files from the given model
     * @param model model to get all files from
     * @return InMemoryFile list of all files found
     */
    public List<InMemoryFile> getUploadedFilesFromModel(AASModel model) {
        List<UploadedFile> uploadedFiles = this.uploadedFileRepository.findAllByModelId(model.getId());
        // z.B. "/aasx/files/doc1.pdf"
        return uploadedFiles.stream()
                .map(file -> {
                    try {
                        String path = file.getStoragePath(); // z.B. "/aasx/files/doc1.pdf"
                        byte[] content = Files.readAllBytes(Paths.get(path));
                        return new InMemoryFile(content, path);
                    } catch (IOException e) {
                        throw new RuntimeException("Error when trying to read file: " + file.getId(), e);
                    }
                })
                .toList();
    }
}
