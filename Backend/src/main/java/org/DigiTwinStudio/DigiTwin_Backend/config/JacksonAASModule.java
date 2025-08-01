package org.DigiTwinStudio.DigiTwin_Backend.config;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.*;

public class JacksonAASModule extends SimpleModule {


    public JacksonAASModule() {
        // Für alle Interfaces aus AAS4j v3 → passende Implementierung setzen
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
        addAbstractTypeMapping(Resource.class, DefaultResource.class);



        addAbstractTypeMapping(AssetAdministrationShell.class, DefaultAssetAdministrationShell.class);
        addAbstractTypeMapping(Submodel.class, DefaultSubmodel.class);


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
