package eu.europa.ec.leos.cmis.types;

import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.inmemory.types.PropertyCreationHelper;

import java.math.BigInteger;

class LeosPropertyDefinition {

    static final PropertyDefinition<String> LEOS_CATEGORY =
            PropertyCreationHelper.createStringDefinition("leos:category", "LEOS Category", Updatability.READWRITE);

    static final PropertyDefinition<String> LEOS_TEMPLATE =
            PropertyCreationHelper.createStringDefinition("leos:template", "LEOS Template", Updatability.READWRITE);

    static final PropertyDefinition<String> LEOS_LANGUAGE =
            PropertyCreationHelper.createStringDefinition("leos:language", "Document Language", Updatability.READWRITE);

    static final PropertyDefinition<String> LEOS_TITLE =
            PropertyCreationHelper.createStringDefinition("leos:title", "Document Title", Updatability.READWRITE);

    static final PropertyDefinition<String> LEOS_METADATA_REF =
            PropertyCreationHelper.createStringDefinition("metadata:ref", "LEOS REF", Updatability.READWRITE);

    static final PropertyDefinition<String> LEOS_COLLABORATORS =
            PropertyCreationHelper.createStringMultiDefinition("leos:collaborators", "Document Collaborators", Updatability.READWRITE);

    static final PropertyDefinition<String> LEOS_METADATA_DOC_TEMPLATE =
            PropertyCreationHelper.createStringDefinition("metadata:docTemplate", "Document Template", Updatability.READWRITE);

    static final PropertyDefinition<String> LEOS_METADATA_DOC_STAGE =
            PropertyCreationHelper.createStringDefinition("metadata:docStage", "Document Stage", Updatability.READWRITE);

    static final PropertyDefinition<String> LEOS_METADATA_DOC_TYPE =
            PropertyCreationHelper.createStringDefinition("metadata:docType", "Document Type", Updatability.READWRITE);

    static final PropertyDefinition<String> LEOS_METADATA_DOC_PURPOSE =
            PropertyCreationHelper.createStringDefinition("metadata:docPurpose", "Document Purpose", Updatability.READWRITE);

    static final PropertyDefinition<BigInteger> LEOS_ANNEX_DOC_INDEX =
            PropertyCreationHelper.createIntegerDefinition("annex:docIndex", "Annex Index", Updatability.READWRITE);

    static final PropertyDefinition<String> LEOS_ANNEX_DOC_NUMBER =
            PropertyCreationHelper.createStringDefinition("annex:docNumber", "Annex Number", Updatability.READWRITE);

    static final PropertyDefinition<String> LEOS_ANNEX_DOC_TITLE =
            PropertyCreationHelper.createStringDefinition("annex:docTitle", "Annex Title", Updatability.READWRITE);

}
