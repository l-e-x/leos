package eu.europa.ec.leos.cmis.types;

import org.apache.chemistry.opencmis.commons.definitions.MutableTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.inmemory.types.DocumentTypeCreationHelper;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;

final class LeosSecondaryTypes {

    static final TypeDefinition CMIS_SECONDARY_TYPE = DocumentTypeCreationHelper.getCmisSecondaryType();

    private static final TypeDefinitionFactory typeFactory = DocumentTypeCreationHelper.getTypeDefinitionFactory();

    static TypeDefinition createLeosMetadataType(TypeDefinition parentType, PropertyDefinition<?>... properties) {
        MutableTypeDefinition leosMetadataSecType =
                typeFactory.createChildTypeDefinition(parentType, "leos:metadata");
        leosMetadataSecType.setDisplayName("LEOS Metadata");
        leosMetadataSecType.setDescription("LEOS Metadata Secondary Type");
        leosMetadataSecType.setLocalNamespace("leos");
        addProperties(leosMetadataSecType, properties);
        return leosMetadataSecType;
    }

    static TypeDefinition createLeosProposalType(TypeDefinition parentType, PropertyDefinition<?>... properties) {
        MutableTypeDefinition leosProposalSecType =
                typeFactory.createChildTypeDefinition(parentType, "leos:proposal");
        leosProposalSecType.setDisplayName("LEOS Proposal Metadata");
        leosProposalSecType.setDescription("LEOS Proposal Metadata Secondary Type");
        leosProposalSecType.setLocalNamespace("leos");
        addProperties(leosProposalSecType, properties);
        return leosProposalSecType;
    }

    static TypeDefinition createLeosMemorandumType(TypeDefinition parentType, PropertyDefinition<?>... properties) {
        MutableTypeDefinition leosMemorandumSecType =
                typeFactory.createChildTypeDefinition(parentType, "leos:memorandum");
        leosMemorandumSecType.setDisplayName("LEOS Memorandum Metadata");
        leosMemorandumSecType.setDescription("LEOS Memorandum Metadata Secondary Type");
        leosMemorandumSecType.setLocalNamespace("leos");
        addProperties(leosMemorandumSecType, properties);
        return leosMemorandumSecType;
    }

    static TypeDefinition createLeosBillType(TypeDefinition parentType, PropertyDefinition<?>... properties) {
        MutableTypeDefinition leosBillSecType =
                typeFactory.createChildTypeDefinition(parentType, "leos:bill");
        leosBillSecType.setDisplayName("LEOS Bill Metadata");
        leosBillSecType.setDescription("LEOS Bill Metadata Secondary Type");
        leosBillSecType.setLocalNamespace("leos");
        addProperties(leosBillSecType, properties);
        return leosBillSecType;
    }

    static TypeDefinition createLeosAnnexType(TypeDefinition parentType, PropertyDefinition<?>... properties) {
        MutableTypeDefinition leosAnnexSecType =
                typeFactory.createChildTypeDefinition(parentType, "leos:annex");
        leosAnnexSecType.setDisplayName("LEOS Annex Metadata");
        leosAnnexSecType.setDescription("LEOS Annex Metadata Secondary Type");
        leosAnnexSecType.setLocalNamespace("leos");
        addProperties(leosAnnexSecType, properties);
        return leosAnnexSecType;
    }

    static TypeDefinition createLeosLegType(TypeDefinition parentType, PropertyDefinition<?>... properties) {
        MutableTypeDefinition leosLegDocType =
                typeFactory.createChildTypeDefinition(parentType, "leos:leg");
        leosLegDocType.setDisplayName("LEOS LEG Document");
        leosLegDocType.setDescription("LEOS LEG Document Type");
        leosLegDocType.setIsCreatable(true);
        addProperties(leosLegDocType, properties);
        return leosLegDocType;
    }
    
    private static void addProperties(MutableTypeDefinition typeDefinition, PropertyDefinition<?>[] properties) {
        for (PropertyDefinition<?> property : properties) {
            typeDefinition.addPropertyDefinition(property);
        }
    }
}
