package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.UploadedFileRepository;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestCleanupService {

    private final AASModelRepository aasModelRepository;
    private final UploadedFileRepository uploadedFileRepository;

    /**
     * Scheduled cleanup of guest models older than 2 hours.
     * Hard deletes the models and all referenced files.
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // Every 30 minutes
    public void deleteExpiredGuestModels() {
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);

        List<AASModel> expiredGuestModels = aasModelRepository
                .findByOwnerIdAndCreatedAtBefore("GUEST", twoHoursAgo);

        if (expiredGuestModels.isEmpty()) {
            log.info("No expired guest models found.");
            return;
        }

        for (AASModel model : expiredGuestModels) {
            log.info("Hard-deleting guest model: {}", model.getId());

            if (model.getSubmodels() != null) {
                model.getSubmodels().forEach(submodel -> {
                    if (submodel.getSubmodelElements() != null) {
                        for (SubmodelElement element : submodel.getSubmodelElements()) {
                            if (element instanceof org.eclipse.digitaltwin.aas4j.v3.model.File fileElem) {
                                String fileId = fileElem.getValue();
                                if(fileId != null) {
                                    try {
                                        uploadedFileRepository.deleteById(fileId);
                                        log.info("Deleted uploaded file: {}", fileId);
                                    } catch (Exception e) {
                                        log.warn("Failed to delete uploaded file: {}", fileId, e);
                                    }
                                }
                            }
                        }
                    }
                });
            }

            try {
                aasModelRepository.deleteById(model.getId());
                log.info("Deleted guest model: {}", model.getId());
            } catch (Exception e) {
                log.warn("Failed to delete guest model: {}", model.getId(), e);
            }
        }

        log.info("Cleanup complete. {} guest models hard-deleted.", expiredGuestModels.size());
    }
}
