package org.DigiTwinStudio.DigiTwin_Backend.util;

import org.DigiTwinStudio.DigiTwin_Backend.domain.AASModel;
import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestModelFactory {

    public static AASModel buildLargeTestModel(int submodelCount, int elementsPerSubmodel, String ownerId) {
        // Root Asset Administration Shell
        DefaultAssetAdministrationShell aas = new DefaultAssetAdministrationShell.Builder()
                .id("aas-" + UUID.randomUUID())
                .idShort("PerfTestAAS")
                .build();

        List<DefaultSubmodel> submodels = new ArrayList<>();

        for (int i = 0; i < submodelCount; i++) {
            DefaultSubmodel submodel = new DefaultSubmodel.Builder()
                    .id("sm-" + UUID.randomUUID())
                    .idShort("Submodel_" + i)
                    .kind(ModellingKind.INSTANCE)
                    .build();

            // Add dummy properties
            List<SubmodelElement> elements = new ArrayList<>();
            for (int j = 0; j < elementsPerSubmodel; j++) {
                elements.add(new DefaultProperty.Builder()
                        .idShort("prop_" + i + "_" + j)
                        .value("value-" + j)
                        .valueType(DataTypeDefXsd.STRING) // correct type enum
                        .build());
            }

            submodel.setSubmodelElements(elements);
            submodels.add(submodel);
        }

        return AASModel.builder()
                .id("perf-" + UUID.randomUUID())
                .ownerId(ownerId)
                .aas(aas)
                .submodels(submodels)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .published(false)
                .build();
    }
}
