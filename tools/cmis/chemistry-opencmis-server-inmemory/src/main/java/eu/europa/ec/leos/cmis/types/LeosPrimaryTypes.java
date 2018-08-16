package eu.europa.ec.leos.cmis.types;

import org.apache.chemistry.opencmis.commons.definitions.MutableDocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.inmemory.types.DocumentTypeCreationHelper;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;

final class LeosPrimaryTypes {

    static final TypeDefinition CMIS_DOCUMENT_TYPE = DocumentTypeCreationHelper.getCmisDocumentType();

    private static final TypeDefinitionFactory typeFactory = DocumentTypeCreationHelper.getTypeDefinitionFactory();

    static TypeDefinition createLeosDocumentType(TypeDefinition parentType, PropertyDefinition<?>... properties) {
        MutableDocumentTypeDefinition leosDocType =
                (MutableDocumentTypeDefinition) typeFactory.createChildTypeDefinition(parentType, "leos:document");
        leosDocType.setDisplayName("LEOS Document");
        leosDocType.setDescription("LEOS Document Type");
        leosDocType.setLocalNamespace("leos");
        leosDocType.setIsVersionable(true);
        leosDocType.setIsCreatable(false);
        addProperties(leosDocType, properties);
        return leosDocType;
    }

    static TypeDefinition createLeosXmlType(TypeDefinition parentType, PropertyDefinition<?>... properties) {
        MutableTypeDefinition leosXmlDocType =
                typeFactory.createChildTypeDefinition(parentType, "leos:xml");
        leosXmlDocType.setDisplayName("LEOS XML Document");
        leosXmlDocType.setDescription("LEOS XML Document Type");
        leosXmlDocType.setIsCreatable(true);
        addProperties(leosXmlDocType, properties);
        return leosXmlDocType;
    }

    static TypeDefinition createLeosMediaType(TypeDefinition parentType, PropertyDefinition<?>... properties) {
        MutableTypeDefinition leosMediaDocType =
                typeFactory.createChildTypeDefinition(parentType, "leos:media");
        leosMediaDocType.setDisplayName("LEOS Media Document");
        leosMediaDocType.setDescription("LEOS Media Document Type");
        leosMediaDocType.setIsCreatable(true);
        addProperties(leosMediaDocType, properties);
        return leosMediaDocType;
    }

    static TypeDefinition createLeosConfigType(TypeDefinition parentType, PropertyDefinition<?>... properties) {
        MutableTypeDefinition leosConfigDocType =
                typeFactory.createChildTypeDefinition(parentType, "leos:config");
        leosConfigDocType.setDisplayName("LEOS Configuration Document");
        leosConfigDocType.setDescription("LEOS Configuration Document Type");
        leosConfigDocType.setIsCreatable(true);
        addProperties(leosConfigDocType, properties);
        return leosConfigDocType;
    }

    private static void addProperties(MutableTypeDefinition typeDefinition, PropertyDefinition<?>[] properties) {
        for (PropertyDefinition<?> property : properties) {
            typeDefinition.addPropertyDefinition(property);
        }
    }
}
