package org.DigiTwinStudio.DigiTwin_Backend.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.*;

/**
 * Custom Jackson module for Asset Administration Shell (AAS4j v3) models.
 * <p>
 * This module configures Jackson to handle the (de)serialization of AAS4j interfaces
 * by mapping them to their default implementation classes and registers custom deserializers
 * for enums and polymorphic submodel elements. This allows for dynamic and robust
 * (de)serialization of AAS-conformant JSON data structures in a Spring Boot environment.
 * </p>
 * <ul>
 *   <li>Maps all relevant AAS4j interfaces to their {@code Default*} implementations.</li>
 *   <li>Registers custom deserializers for enums such as {@link ModellingKind}, {@link KeyTypes}, etc.,
 *       to support various JSON representations and naming variations.</li>
 *   <li>Provides a polymorphic deserializer for {@link SubmodelElement} based on the {@code modelType} field.</li>
 * </ul>
 */
public class JacksonAASModule extends SimpleModule {

    /**
     * Initializes the module with type mappings and custom deserializers
     * for AAS4j v3 interfaces and enums.
     */
    public JacksonAASModule() {
        // Map all AAS4j v3 interfaces to their default implementations
        addAbstractTypeMapping(LangStringTextType.class, DefaultLangStringTextType.class);
        addAbstractTypeMapping(LangStringNameType.class, DefaultLangStringNameType.class);
        addAbstractTypeMapping(Reference.class, DefaultReference.class);
        addAbstractTypeMapping(AdministrativeInformation.class, DefaultAdministrativeInformation.class);
        addAbstractTypeMapping(Extension.class, DefaultExtension.class);
        addAbstractTypeMapping(Qualifier.class, DefaultQualifier.class);
        addAbstractTypeMapping(EmbeddedDataSpecification.class, DefaultEmbeddedDataSpecification.class);
        addAbstractTypeMapping(Key.class, DefaultKey.class);
        addAbstractTypeMapping(File.class, DefaultFile.class);
        addAbstractTypeMapping(AssetInformation.class, DefaultAssetInformation.class);
        addAbstractTypeMapping(SpecificAssetId.class, DefaultSpecificAssetId.class);
        addAbstractTypeMapping(Resource.class, DefaultResource.class);

        addAbstractTypeMapping(AssetAdministrationShell.class, DefaultAssetAdministrationShell.class);
        addAbstractTypeMapping(Submodel.class, DefaultSubmodel.class);

        // Register custom deserializers for complex and polymorphic types
        addDeserializer(SubmodelElement.class, new SubmodelElementDeserializer());
        addDeserializer(ModellingKind.class, new GenericEnumDeserializer<>(ModellingKind.class));
        addDeserializer(KeyTypes.class, new GenericEnumDeserializer<>(KeyTypes.class));
        addDeserializer(DataTypeDefXsd.class, new GenericEnumDeserializer<>(DataTypeDefXsd.class));
        addDeserializer(ReferenceTypes.class, new GenericEnumDeserializer<>(ReferenceTypes.class));
        addDeserializer(QualifierKind.class, new GenericEnumDeserializer<>(QualifierKind.class));
        addDeserializer(DataTypeDefXsd.class, new GenericEnumDeserializer<>(DataTypeDefXsd.class));
        addDeserializer(AasSubmodelElements.class, new GenericEnumDeserializer<>(AasSubmodelElements.class));
        addDeserializer(AssetKind.class, new GenericEnumDeserializer<>(AssetKind.class));
    }
}
