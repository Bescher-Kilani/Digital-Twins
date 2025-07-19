package org.DigiTwinStudio.DigiTwin_Backend.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.DigiTwinStudio.DigiTwin_Backend.adapter.AAS4jAdapter;
import org.DigiTwinStudio.DigiTwin_Backend.adapter.FaaastAdapter;
import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.DigiTwinStudio.DigiTwin_Backend.domain.ExportFormat;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {
    private final AAS4jAdapter aas4jAdapter;
    private final FaaastAdapter faaastAdapter;

    public byte[] exportAsJson(AASModel model) {
        DefaultAssetAdministrationShell aas = model.getAas();

        throw new UnsupportedOperationException("Not supported yet.");
    }
    public byte[] exportAsAasx(AASModel model) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public byte[] exportStoredModel(String modelId, ExportFormat format) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public byte[] exportTransientModel(AASModelDto dto, ExportFormat format) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public byte[] exportMarketplaceModel(String entryId, ExportFormat format) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private DefaultEnvironment aasModeltoDefaultEnvironment(AASModel model) {
        // create Environment
        DefaultEnvironment environment = new DefaultEnvironment();
        // add AssetAdministrationShell
        environment.setAssetAdministrationShells(List.of(model.getAas()));

        // convert DefaultSubmodels to Submodels and add to Environment
        List<Submodel> submodels = new ArrayList<>(model.getSubmodels().size());
        submodels.addAll(model.getSubmodels());
        environment.setSubmodels(submodels);

        return environment;
    }
}
