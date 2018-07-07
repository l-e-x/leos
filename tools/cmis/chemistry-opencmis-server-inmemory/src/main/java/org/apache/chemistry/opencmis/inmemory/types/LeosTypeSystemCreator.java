package org.apache.chemistry.opencmis.inmemory.types;

import org.apache.chemistry.opencmis.commons.definitions.MutableDocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.inmemory.TypeCreator;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LeosTypeSystemCreator implements TypeCreator {

    public static final List<TypeDefinition> singletonTypes = buildTypesList();

    public List<TypeDefinition> createTypesList() {
        return singletonTypes;
    }

    private static List<TypeDefinition> buildTypesList() {
        TypeDefinitionFactory typeFactory = DocumentTypeCreationHelper.getTypeDefinitionFactory();

        List<TypeDefinition> typesList = new LinkedList<TypeDefinition>();

        // create LEOS folder type
        MutableTypeDefinition leosFolderType =
                typeFactory.createChildTypeDefinition(DocumentTypeCreationHelper.getCmisFolderType(), "leos:folder");
        leosFolderType.setDisplayName("LEOS Folder");
        leosFolderType.setDescription("LEOS Folder Type");
        leosFolderType.setLocalNamespace("leos");
        typesList.add(leosFolderType);
        //roles
        PropertyStringDefinitionImpl propDocType = PropertyCreationHelper.createStringDefinition( "leos:contributorIds", "contributorIds", Updatability.READWRITE);
        propDocType.setIsRequired(false);
        propDocType.setQueryName("leos:contributorIds");
        propDocType.setCardinality(Cardinality.MULTI);
        leosFolderType.addPropertyDefinition(propDocType);

        propDocType = PropertyCreationHelper.createStringDefinition( "leos:contributorNames", "contributorNames", Updatability.READWRITE);
        propDocType.setIsRequired(false);
        propDocType.setQueryName("leos:contributorNames");
        propDocType.setCardinality(Cardinality.MULTI);
        leosFolderType.addPropertyDefinition(propDocType);

        propDocType = PropertyCreationHelper.createStringDefinition( "leos:authorId", "authorId", Updatability.READWRITE);
        propDocType.setIsRequired(false); // should be true
        propDocType.setQueryName("leos:authorId");
        propDocType.setCardinality(Cardinality.SINGLE);
        leosFolderType.addPropertyDefinition(propDocType);

        propDocType = PropertyCreationHelper.createStringDefinition( "leos:authorName", "authorName", Updatability.READWRITE);
        propDocType.setIsRequired(false); // should be true
        propDocType.setQueryName("leos:authorName");
        propDocType.setCardinality(Cardinality.SINGLE);
        leosFolderType.addPropertyDefinition(propDocType);

        // create LEOS folder type
        MutableDocumentTypeDefinition leosFileType =
                (MutableDocumentTypeDefinition) typeFactory.createChildTypeDefinition(DocumentTypeCreationHelper.getCmisDocumentType(), "leos:file");
        leosFileType.setDisplayName("LEOS File");
        leosFileType.setDescription("LEOS File Type");
        leosFileType.setLocalNamespace("leos");
        leosFileType.setIsVersionable(true);
        typesList.add(leosFileType);
        //roles
        PropertyStringDefinitionImpl propDocTypeDef = PropertyCreationHelper.createStringDefinition( "leos:contributorIds", "contributorIds", Updatability.READWRITE);
        propDocTypeDef.setIsRequired(false);
        propDocTypeDef.setQueryName("leos:contributorIds");
        propDocTypeDef.setCardinality(Cardinality.MULTI);
        leosFileType.addPropertyDefinition(propDocTypeDef);

        propDocTypeDef = PropertyCreationHelper.createStringDefinition( "leos:contributorNames", "contributorNames", Updatability.READWRITE);
        propDocTypeDef.setIsRequired(false);
        propDocTypeDef.setQueryName("leos:contributorNames");
        propDocTypeDef.setCardinality(Cardinality.MULTI);
        leosFileType.addPropertyDefinition(propDocTypeDef);

        propDocTypeDef = PropertyCreationHelper.createStringDefinition( "leos:authorId", "authorId", Updatability.READWRITE);
        propDocTypeDef.setIsRequired(false); // should be true
        propDocTypeDef.setQueryName("leos:authorId");
        propDocTypeDef.setCardinality(Cardinality.SINGLE);
        leosFileType.addPropertyDefinition(propDocTypeDef);

        propDocTypeDef = PropertyCreationHelper.createStringDefinition( "leos:authorName", "authorName", Updatability.READWRITE);
        propDocTypeDef.setIsRequired(false); // should be true
        propDocTypeDef.setQueryName("leos:authorName");
        propDocTypeDef.setCardinality(Cardinality.SINGLE);
        leosFileType.addPropertyDefinition(propDocTypeDef);

        // create LEOS document type
        MutableDocumentTypeDefinition leosDocType =
                (MutableDocumentTypeDefinition) typeFactory.createChildTypeDefinition(leosFileType, "leos:document");
        leosDocType.setDisplayName("LEOS Document");
        leosDocType.setDescription("LEOS Document Type");
        leosDocType.setLocalNamespace("leos");
        leosDocType.setIsVersionable(true);

        //setting custom properties
        PropertyStringDefinitionImpl prop = PropertyCreationHelper.createStringDefinition( "leos:system", "System", Updatability.READWRITE);
        prop.setIsRequired(false);
        prop.setQueryName("leos:system");
        prop.setMaxLength(BigInteger.valueOf(25));
        List<String>  lstDefaultValues= new ArrayList<String>();
        lstDefaultValues.add("LEOS");

        prop.setDefaultValue(lstDefaultValues);
        leosDocType.addPropertyDefinition(prop);

        prop = PropertyCreationHelper.createStringDefinition( "leos:title", "Title", Updatability.READWRITE);
        prop.setIsRequired(false);
        prop.setQueryName("leos:title");
        leosDocType.addPropertyDefinition(prop);

        prop = PropertyCreationHelper.createStringDefinition( "leos:template", "Template", Updatability.READWRITE);
        prop.setIsRequired(false);
        prop.setQueryName("leos:template");
        prop.setMaxLength(BigInteger.valueOf(100));
        lstDefaultValues= new ArrayList<String>();
        lstDefaultValues.add("SJ-016");
        prop.setDefaultValue(lstDefaultValues);
        leosDocType.addPropertyDefinition(prop);

        prop = PropertyCreationHelper.createStringDefinition( "leos:language", "Language", Updatability.READWRITE);
        prop.setIsRequired(false);
        prop.setQueryName("leos:language");
        prop.setMaxLength(BigInteger.valueOf(25));
        lstDefaultValues= new ArrayList<String>();
        lstDefaultValues.add("EN");
        prop.setDefaultValue(lstDefaultValues);
        leosDocType.addPropertyDefinition(prop);

        prop = PropertyCreationHelper.createStringDefinition( "leos:stage", "Stage", Updatability.READWRITE);
        prop.setIsRequired(false);
        prop.setQueryName("leos:stage");
        prop.setMaxLength(BigInteger.valueOf(25));
        lstDefaultValues= new ArrayList<String>();
        lstDefaultValues.add("DRAFT");
        prop.setDefaultValue(lstDefaultValues);
        leosDocType.addPropertyDefinition(prop);

        typesList.add(leosDocType);

        // CMIS base types are always added by default
        return typesList;
    }
}