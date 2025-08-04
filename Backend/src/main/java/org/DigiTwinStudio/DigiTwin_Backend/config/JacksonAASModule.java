package org.DigiTwinStudio.DigiTwin_Backend.config;

import com.fasterxml.jackson.databind.module.SimpleModule;

import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.*;

/**
 * Jackson module for AAS4j v3 (Asset Administration Shell) models.
 * <p>
 * Maps AAS4j interfaces to their default implementations and registers custom deserializers
 * for enums and submodel elements. Enables correct JSON (de)serialization for AAS models.
 * </p>
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

        // Deserializes SubmodelElement based on the "modelType" field (e.g., "Property", "File")
        addDeserializer(SubmodelElement.class, new SubmodelElementDeserializer());

        // Handles different representations of ModellingKind enum
        // Example inputs: "instance", "INSTANCE", "Instance" → ModellingKind.INSTANCE
        addDeserializer(ModellingKind.class, new GenericEnumDeserializer<>(ModellingKind.class));

        // Handles various formats of KeyTypes enum
        // Example inputs: "globalReference", "GLOBAL_REFERENCE", "global-reference" → KeyTypes.GLOBAL_REFERENCE
        addDeserializer(KeyTypes.class, new GenericEnumDeserializer<>(KeyTypes.class));

        // Handles ReferenceTypes enum with flexible input
        // Example inputs: "modelReference", "Model Reference", "xs:modelReference" → ReferenceTypes.MODEL_REFERENCE
        addDeserializer(ReferenceTypes.class, new GenericEnumDeserializer<>(ReferenceTypes.class));

        // Handles QualifierKind enum from different JSON values
        // Example inputs: "tolerance", "TOLERANCE", "Tolerance" → QualifierKind.TOLERANCE
        addDeserializer(QualifierKind.class, new GenericEnumDeserializer<>(QualifierKind.class));

        // Handles XML-style data type values (DataTypeDefXsd)
        // Example inputs: "xs:string", "String", "string" → DataTypeDefXsd.STRING
        addDeserializer(DataTypeDefXsd.class, new GenericEnumDeserializer<>(DataTypeDefXsd.class));

        // Handles submodel element type enum
        // Example inputs: "Property", "multiLanguageProperty", "submodel-element-collection" → AasSubmodelElements.PROPERTY / MULTI_LANGUAGE_PROPERTY / SUBMODEL_ELEMENT_COLLECTION
        addDeserializer(AasSubmodelElements.class, new GenericEnumDeserializer<>(AasSubmodelElements.class));

        // Handles AssetKind enum flexibly
        // Example inputs: "instance", "INSTANCE", "type" → AssetKind.INSTANCE / TYPE
        addDeserializer(AssetKind.class, new GenericEnumDeserializer<>(AssetKind.class));

    }
}
