package eu.europa.ec.leos.cmis.types;

import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.inmemory.TypeCreator;

import java.util.ArrayList;
import java.util.List;

public class LeosPrimaryTypesTypeSystemCreator implements TypeCreator {

    private static final List<TypeDefinition> singletonTypes = buildTypesList();

    @Override
    public List<TypeDefinition> createTypesList() {
        return singletonTypes;
    }

    private static List<TypeDefinition> buildTypesList() {

        // NOTE CMIS base types are always added by default
        List<TypeDefinition> typesList = new ArrayList<>();

        // create LEOS document type
        TypeDefinition leosDocType =
                LeosPrimaryTypes.createLeosDocumentType(
                        LeosPrimaryTypes.CMIS_DOCUMENT_TYPE,
                        LeosPropertyDefinition.LEOS_CATEGORY);
        typesList.add(leosDocType);

        // create LEOS Akoma Ntoso XML document type
        TypeDefinition leosXmlDocType =
                LeosPrimaryTypes.createLeosXmlType(
                        leosDocType,
                        LeosPropertyDefinition.LEOS_TEMPLATE,
                        LeosPropertyDefinition.LEOS_LANGUAGE,
                        LeosPropertyDefinition.LEOS_TITLE,
                        LeosPropertyDefinition.LEOS_COLLABORATORS,
                        LeosPropertyDefinition.LEOS_METADATA_REF,
                        LeosPropertyDefinition.LEOS_METADATA_DOC_TEMPLATE,
                        LeosPropertyDefinition.LEOS_METADATA_DOC_STAGE,
                        LeosPropertyDefinition.LEOS_METADATA_DOC_TYPE,
                        LeosPropertyDefinition.LEOS_METADATA_DOC_PURPOSE,
                        LeosPropertyDefinition.LEOS_ANNEX_DOC_INDEX,
                        LeosPropertyDefinition.LEOS_ANNEX_DOC_NUMBER,
                        LeosPropertyDefinition.LEOS_ANNEX_DOC_TITLE);
        typesList.add(leosXmlDocType);

        // create LEOS Media File document type
        typesList.add(LeosPrimaryTypes.createLeosMediaType(leosDocType));

        // create LEOS Configuration File document type
        typesList.add(LeosPrimaryTypes.createLeosConfigType(leosDocType));

        return typesList;
    }
}
