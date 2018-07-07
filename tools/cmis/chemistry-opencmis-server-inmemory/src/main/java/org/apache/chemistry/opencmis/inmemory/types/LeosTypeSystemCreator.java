package org.apache.chemistry.opencmis.inmemory.types;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.chemistry.opencmis.commons.definitions.MutableDocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.inmemory.TypeCreator;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;

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

        // create LEOS folder type
        MutableDocumentTypeDefinition leosFileType =
                (MutableDocumentTypeDefinition) typeFactory.createChildTypeDefinition(DocumentTypeCreationHelper.getCmisDocumentType(), "leos:file");
        leosFileType.setDisplayName("LEOS File");
        leosFileType.setDescription("LEOS File Type");
        leosFileType.setLocalNamespace("leos");
        leosFileType.setIsVersionable(true);
        typesList.add(leosFileType);

        // create LEOS document type
        MutableDocumentTypeDefinition leosDocType =
                (MutableDocumentTypeDefinition) typeFactory.createChildTypeDefinition(leosFileType, "leos:document");
        leosDocType.setDisplayName("LEOS Document");
        leosDocType.setDescription("LEOS Document Type");
        leosDocType.setLocalNamespace("leos");
        leosDocType.setIsVersionable(true);
        
        //setting custom properties
        PropertyStringDefinitionImpl prop = PropertyCreationHelper.createStringDefinition( "leos:title", "Title", Updatability.READWRITE);
        prop.setIsRequired(false);
        prop.setQueryName("leos:title");
        prop.setMaxLength(BigInteger.valueOf(150)); 
        leosDocType.addPropertyDefinition(prop);

        prop = PropertyCreationHelper.createStringDefinition( "leos:template", "Tempalte", Updatability.READWRITE);
        prop.setIsRequired(false);
        prop.setQueryName("leos:template");
        prop.setMaxLength(BigInteger.valueOf(150));
        leosDocType.addPropertyDefinition(prop);

        prop = PropertyCreationHelper.createStringDefinition( "leos:language", "Language", Updatability.READWRITE);
        prop.setIsRequired(false);
        prop.setQueryName("leos:language");
        prop.setMaxLength(BigInteger.valueOf(150));
        leosDocType.addPropertyDefinition(prop);
        List<String> lstDefaultValues= new ArrayList<String>();
        lstDefaultValues.add("en");
        prop.setDefaultValue(lstDefaultValues);
        typesList.add(leosDocType);

        // CMIS base types are always added by default
        return typesList;
    }
}