/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.repositories.content;

import com.google.common.base.Stopwatch;
import com.google.common.net.MediaType;
import eu.europa.ec.leos.model.content.*;
import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.repositories.support.cmis.OperationContextProvider;
import eu.europa.ec.leos.repositories.support.cmis.StorageProperties;
import eu.europa.ec.leos.repositories.support.cmis.StorageProperties.EditableProperty;
import eu.europa.ec.leos.vo.UserVO;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Repository
public class ContentRepositoryImpl implements ContentRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ContentRepositoryImpl.class);

    @Autowired
    private Session session;

    @Autowired
    private OperationContextProvider operationContextProvider;

    @Autowired
    protected SecurityContext leosSecurityContext;

    @Override
    public List<LeosObjectProperties> browse(@Nonnull String folderPath){
        LOG.trace("Browsing folder... [path={}]", folderPath);
        Validate.isTrue((folderPath != null), "The folder path must not be null!");
        Stopwatch stopwatch = Stopwatch.createStarted();
        OperationContext context = operationContextProvider.getMinimalContext();
        Folder folder = getCmisFolderByPath(folderPath, context);
        ItemIterable<CmisObject> cmisObjects = folder.getChildren(context);
        List<LeosObjectProperties> leosProperties = wrapIntoLeosObjectProperties(cmisObjects);
        LOG.trace("Browse folder finished! ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return leosProperties;
    }

    @Override
    public List<LeosObjectProperties> getUserLeosObjectProperties(@Nonnull String folderPath, @Nonnull User user){
        LOG.trace("getUserLeosObjectProperties folder... [path={}] for user [id={}]", folderPath, user.getId());
        Validate.isTrue((folderPath != null), "The folder path must not be null!");
        Validate.isTrue((user != null && user.getLogin()!=null), "The userVO  must not be null!");

        Stopwatch stopwatch = Stopwatch.createStarted();
        OperationContext context = operationContextProvider.getMinimalContext();
        Folder folder = getCmisFolderByPath(folderPath, context);
        StringBuffer sb= new StringBuffer("any leos:contributorIds in ('").append(user.getLogin()).append("')");
        sb.append(" or leos:authorId = '").append(user.getLogin()).append("'");
        sb.append(" and leos:stage <> 'ARCHIVED' and in_folder ('").append(folder.getId()).append("')");
        LOG.trace("query = "+sb.toString());
        ItemIterable<CmisObject> cmisObjects= session.queryObjects(LeosTypeId.LEOS_DOCUMENT.value(), sb.toString() , false, context);
        List<LeosObjectProperties> leosProperties = wrapIntoLeosObjectProperties(cmisObjects);
        LOG.trace("Browse folder finished! ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return leosProperties;
    }

    @Override
    public <T extends LeosObject> T retrieveById(@Nonnull String leosId, Class<T> type){
        LOG.trace("Retrieving by leosId... [leosId={}]", leosId);
        Validate.isTrue((leosId != null), "The leosId must not be null!");
        Stopwatch stopwatch = Stopwatch.createStarted();
        OperationContext context = operationContextProvider.getMinimalContext();
        CmisObject cmisObject=(LeosFolder.class.equals(type))
                        ? getCmisObjectById(leosId, context)
                        : getCmisDocumentByLeosId(leosId, context);
        T leosObject = wrapIntoLeosObject(cmisObject);
        LOG.trace("Retrieve document by leosId finished! ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return leosObject;
    }

    @Override
    public <T extends LeosObject> T retrieveByPath(String path, Class<T> type) {
        LOG.trace("Retrieving LeosObject by path... [path={}]", path);
        Validate.isTrue((path != null), "The path must not be null!");
        Stopwatch stopwatch = Stopwatch.createStarted();
        OperationContext context = operationContextProvider.getMinimalContext();
        CmisObject    cmisObject= getCmisObjectByPath(path, context);
        T leosObject = wrapIntoLeosObject(cmisObject);
        LOG.trace("Retrieve LeosObject by path finished! ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return leosObject;
    }

    @Override
    public <T extends LeosFile> T retrieveVersion(String versionId, Class<T> type) {
        LOG.trace("Retrieving LeosFile version by versionId... [versionId={}]", versionId);
        Validate.isTrue((versionId != null), "The versionId must not be null!");
        Stopwatch stopwatch = Stopwatch.createStarted();
        OperationContext context = operationContextProvider.getMinimalContext();
        Document cmisDocument = getCmisDocumentVersionById(versionId, context);
        T leosFileObject = wrapIntoLeosObject(cmisDocument);
        LOG.trace("Retrieve LeosFile version by versionId finished! ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return leosFileObject;
    }

    @Override
    public <T extends LeosFile> T updateContent(String leosId, Long streamLength, MediaType streamMimeType, InputStream inputStream, StorageProperties newProperties, Class<T> type) {
        LOG.trace("Updating leosfile content... [leosId={}]", leosId);
        Validate.isTrue((leosId != null), "The leosId must not be null!");
        Validate.isTrue((inputStream != null), "The content stream must not be null!");
        Stopwatch stopwatch = Stopwatch.createStarted();
        OperationContext context = operationContextProvider.getMinimalContext();
        
        Document originalDocument = getCmisDocumentByLeosId(leosId, context);
        String nameProp = (String)newProperties.getValue(EditableProperty.NAME);
        String name = StringUtils.isNotBlank(nameProp)
                                ? nameProp
                                : originalDocument.getName();
                                
        long length = ((streamLength != null) && (streamLength >= -1)) ? streamLength : -1;
        String mimeType = (streamMimeType != null) ? streamMimeType.toString() : MediaType.OCTET_STREAM.toString();

        Document updatedCmisDocument;
        try (InputStream stream = new AutoCloseInputStream(inputStream)) {
            ContentStream contentStream = session.getObjectFactory().createContentStream(name, length, mimeType, stream);
                boolean versionable = ((DocumentType) (originalDocument.getType())).isVersionable();
                LOG.trace(originalDocument.getName() + " is versionable :" + (versionable));
                updatedCmisDocument = (!versionable)
                        ? originalDocument.setContentStream(contentStream, true)
                        : createNewFileVersion(originalDocument, contentStream, VersioningState.MINOR, newProperties);
        } catch (Exception e) {
            String errorMessage = String.format("Unable to update document content! [leosId=%s]", leosId);
            LOG.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }

        if (updatedCmisDocument == null) {
            // depending on the CMIS binding, the returned document may be null,
            // still the update was successful, so let's retrieve the updated document
            updatedCmisDocument = getCmisDocumentByLeosId(leosId, context);
        }

        T leosFile = wrapIntoLeosObject(updatedCmisDocument);
        LOG.trace("Update File content finished! ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return leosFile;
    }

    private Document createNewFileVersion(Document doc, ContentStream updatedContentStream, VersioningState version, StorageProperties newProperties) {
        String pwcId = doc.checkOut().getId();
        String updatedDocid = doc.getId();
        OperationContext context = operationContextProvider.getMinimalContext();
        Document pwc = getCmisDocumentVersionById(pwcId, context);
        // Check in the pwc
        try {

            /*Kludge: we are cumulating all the properties as Atompub binding requires complete propertyset.
             In case property is not included, it will be reset to default values*/
            Map<String, Object> tempMap = new HashMap<>();
            for (Property prop : pwc.getProperties()) {
                if (prop.getId().startsWith("leos:")) {
                    tempMap.put(prop.getId(), prop.getValue());
                }
            }
            for (String key : newProperties.getAllProperties().keySet()) {//override with new values
                tempMap.put(key, newProperties.getAllProperties().get(key));
            }

            updatedDocid = pwc.checkIn(VersioningState.MINOR.equals(version) ? false : true,
                    tempMap,
                    updatedContentStream,
                    (String)newProperties.getValue(EditableProperty.CHECKIN_COMMENT))
                    .getId();
        } catch (Exception e) {
            LOG.error("checkin failed, trying to cancel the checkout", e);
            pwc.cancelCheckOut();
        }
        LOG.trace("Updated version... [original version  id={}, pwc Id={}, updated version Id={}]", doc.getId(), pwcId, updatedDocid);
        Document updatedDoc = getCmisDocumentVersionById(updatedDocid, context);
        return updatedDoc;
    }

    @Override
    public List<LeosFileProperties> getVersions(String leosId){
        LOG.trace("Getting leos File versions... [leosId={}]", leosId);
        Validate.isTrue((leosId != null), "The leosId must not be null!");
        Stopwatch stopwatch = Stopwatch.createStarted();
        OperationContext context = operationContextProvider.getMinimalContext();
        Document originalDocument = getCmisDocumentByLeosId(leosId, context);

        List<Document> versions = originalDocument.getAllVersions(context);
        List<LeosFileProperties> lstVersions = wrapIntoLeosFileProperties(versions);
        LOG.trace("Got file versions... ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return lstVersions;
    }

    @Override
    public <T extends LeosFile> T copy(String sourceFilePath, String targetFolderPath, StorageProperties properties ) {
        LOG.trace("Creating LeosFile from source path... [sourceFilePath={}]", sourceFilePath);
        Validate.isTrue((sourceFilePath != null), "The source LeosFile path must not be null!");
        Validate.isTrue((targetFolderPath != null), "The target folder path must not be null!");

        Validate.isTrue((properties.getValue(EditableProperty.NAME) != null), "The target name must not be null!");
        Validate.isTrue((properties.getValue(EditableProperty.CHECKIN_COMMENT) != null), "Checkin Comment must not be null!");

        Stopwatch stopwatch = Stopwatch.createStarted();

        OperationContext context = operationContextProvider.getMinimalContext();
        Document sourceDocument = getCmisDocumentByPath(sourceFilePath, context);
        Folder targetFolder = getCmisFolderByPath(targetFolderPath, context);

        Map<String, Object> propertiesMap = properties.getAllProperties();
        //verify
        LOG.trace("Creating target document... [path={}, name={}]", targetFolderPath, properties.getValue(EditableProperty.NAME));
        Document newDocument =
                sourceDocument.copy(targetFolder, propertiesMap, VersioningState.MAJOR, null, null, null, context);

        T leosFile = wrapIntoLeosObject(newDocument);
        LOG.trace("Create document from path finished! ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return leosFile;
    }

    @Override
    public <T extends LeosObject> T updateProperties(String leosId, StorageProperties newProperties) {
        LOG.trace("Updating document properties... [leosId={}, name={}]", leosId);
        Validate.isTrue((leosId != null), "The leosId must not be null!");
        Validate.isTrue((newProperties != null), "The properties must not be null!");
        Stopwatch stopwatch = Stopwatch.createStarted();

        OperationContext context = operationContextProvider.getMinimalContext();
        CmisObject cmisobject = getCmisDocumentByLeosId(leosId, context);
        
        LOG.trace("Updating properties...{}", newProperties);
        cmisobject = cmisobject.updateProperties(newProperties.getAllProperties());
        
        T leosObject = wrapIntoLeosObject(cmisobject);
        LOG.trace("Object properties updated! ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return leosObject;
    }

    @Override
    public <T extends LeosFile> T create(String folderPath, MediaType streamMimeType, InputStream inputStream, Long streamLength, StorageProperties newProperties, Class<T> type) {
        Validate.isTrue((newProperties.getValue(EditableProperty.NAME) != null), "The Document name must not be null!");
        
        OperationContext context = operationContextProvider.getMinimalContext();
        Folder folder = createFolder(folderPath, context);

        long length = ((streamLength != null) && (streamLength >= -1)) ? streamLength : -1;
        String mimeType = (streamMimeType != null) ? streamMimeType.toString() : MediaType.OCTET_STREAM.toString();
        
        String fileName = (String)newProperties.getValue(EditableProperty.NAME);

        if(type.equals(LeosDocument.class)){
            newProperties.set(EditableProperty.TYPE,LeosTypeId.LEOS_DOCUMENT.value());
            if(newProperties.getValue(EditableProperty.TITLE)==null){
                newProperties.set(EditableProperty.TITLE, fileName);
            }
        }
        else{
            newProperties.set(EditableProperty.TYPE,LeosTypeId.LEOS_FILE.value());
        }
        
        try (InputStream stream = new AutoCloseInputStream(inputStream)) {
            ContentStream contentStream = session.getObjectFactory().createContentStream(fileName, length, mimeType, stream);
            Document document = folder.createDocument(newProperties.getAllProperties(), contentStream, null, null, null, null, context);
            LOG.trace("Created LeosFile: {}/{} [leosId={}]", folder.getPath(), document.getName(), document.getVersionSeriesId());
            return wrapIntoLeosObject(document);
        } catch (Exception e) {
            String errorMessage = String.format("Unable to create LeosFile! [path=%s, name=%s]", folderPath, fileName);
            LOG.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public void delete(String leosId, Class type) {
        LOG.trace("Deleting LeosObject... [leosId={}]", leosId);
        Validate.isTrue((leosId != null), "The leosId must not be null!");
        OperationContext context = operationContextProvider.getMinimalContext();
        CmisObject cmisObject;
        if (LeosFile.class.isAssignableFrom(type)){
            cmisObject = getCmisDocumentByLeosId(leosId, context);
        }
        else{
            cmisObject = getCmisObjectById(leosId, context);
        }
        try {
            cmisObject.delete(); // delete the object
        } catch (Exception e) {
            String errorMessage = String.format("Unable to delete Object! [leosId=%s]", leosId);
            LOG.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    private Folder createFolder(String path, OperationContext context) {
        String pathDelimiter = "/";
        StringTokenizer pathTokenizer = new StringTokenizer(path, pathDelimiter, false);

        Folder parentFolder = session.getRootFolder(context);

        while (pathTokenizer.hasMoreTokens()) {
            String folderName = pathTokenizer.nextToken();
            boolean folderExists = false;
            for (CmisObject obj : parentFolder.getChildren()) {
                folderExists = BaseTypeId.CMIS_FOLDER.equals(obj.getBaseTypeId()) && folderName.equals(obj.getName());
                if (folderExists) {
                    parentFolder = (Folder) obj;
                    break;
                }
            }
            if (!folderExists) {
                Map<String, Object> properties = new HashMap<>();
                properties.put(PropertyIds.OBJECT_TYPE_ID, LeosTypeId.LEOS_FOLDER.value()); // FIXME object type should be a generic folder
                properties.put(PropertyIds.NAME, folderName);
                parentFolder = parentFolder.createFolder(properties, null, null, null, context);
                LOG.trace("Created folder: {} [id={}]", parentFolder.getPath(), parentFolder.getId());
            }
        }

        return parentFolder;
    }    

    protected CmisObject getCmisObjectByPath(String path, OperationContext context) {
        LOG.trace("Getting CMIS object by path... [path={}]", path);
        Validate.isTrue((path != null), "The object path must not be null!");
        Validate.isTrue((context != null), "The operation context must not be null!");
        CmisObject cmisObject = session.getObjectByPath(path, context);
        return validateCmisObject(cmisObject);
    }

    protected CmisObject getCmisObjectById(String id, OperationContext context) {
        LOG.trace("Getting CMIS object by id... [id={}]", id);
        Validate.isTrue((id != null), "The object id must not be null!");
        Validate.isTrue((context != null), "The operation context must not be null!");
        CmisObject cmisObject = session.getObject(id, context);
        return validateCmisObject(cmisObject);
    }

    protected Folder getCmisFolderByPath(String path, OperationContext context) {
        LOG.trace("Getting CMIS folder by path... [path={}]", path);
        CmisObject cmisObject = getCmisObjectByPath(path, context);
        return validateCmisFolder(cmisObject);
    }

    protected Folder getCmisFolderById(String id, OperationContext context) {
        LOG.trace("Getting CMIS folder by id... [id={}]", id);
        CmisObject cmisObject = getCmisObjectById(id, context);
        return validateCmisFolder(cmisObject);
    }

    protected Document getCmisDocumentByPath(String path, OperationContext context) {
        LOG.trace("Getting CMIS document by path... [path={}]", path);
        CmisObject cmisObject = getCmisObjectByPath(path, context);
        return validateCmisDocument(cmisObject);
    }

    protected Document getCmisDocumentByLeosId(String leosId, OperationContext context) {
        LOG.trace("Getting CMIS document by leosId... [leosId={}]", leosId);
        //the id passed should be versionSeriesId(leosId)
        CmisObject cmisObject= getLatestObject(leosId, context);
        Document doc =validateCmisDocument(cmisObject);
        return doc;
    }

    protected Document getCmisDocumentVersionById(String versionId, OperationContext context) {
        LOG.trace("Getting CMIS document by id... [versionId={}]", versionId);
        CmisObject cmisObject= getCmisObjectById(versionId, context);
        Document doc =validateCmisDocument(cmisObject);
        return doc;
    }

    protected CmisObject getLatestObject(String leosId, OperationContext context) {
        LOG.trace("Getting latest obejct from CMIS by leosId... [leosId={}]", leosId);
        Stopwatch stopwatch=Stopwatch.createStarted();
        StringBuffer sb= new StringBuffer("cmis:versionSeriesId='").append(leosId).append("'");
        ItemIterable<CmisObject> cmisObjects= session.queryObjects(LeosTypeId.LEOS_DOCUMENT.value(), sb.toString() , false, context);
        CmisObject latestVersion = cmisObjects.iterator().hasNext()?cmisObjects.iterator().next():null;
        LOG.trace("Latest object received from CMIS! ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return latestVersion;
    }

    private CmisObject validateCmisObject(CmisObject cmisObject) {
        Validate.notNull(cmisObject, "CMIS object is null!");
        LOG.trace("CMIS object base type id: {}", cmisObject.getBaseTypeId().value());
        LOG.trace("CMIS object type id: {}", cmisObject.getType().getId());
        LOG.trace("CMIS object id: {}", cmisObject.getId());
        return cmisObject;
    }

    private Folder validateCmisFolder(CmisObject cmisObject) {
        Validate.isTrue(
                BaseTypeId.CMIS_FOLDER.equals(cmisObject.getBaseTypeId()),
                "CMIS object is not a CMIS folder! [id=%s, typeId=%s]",
                cmisObject.getId(),
                cmisObject.getBaseTypeId());
        Validate.isInstanceOf(Folder.class, cmisObject);
        return (Folder) cmisObject;
    }

    private Document validateCmisDocument(CmisObject cmisObject) {
        Validate.isTrue((cmisObject!=null), "No Document Found");
        Validate.isTrue(
                BaseTypeId.CMIS_DOCUMENT.equals(cmisObject.getBaseTypeId()),
                "CMIS object is not a CMIS document! [id=%s, typeId=%s]",
                cmisObject.getId(),
                cmisObject.getBaseTypeId());
        Validate.isInstanceOf(Document.class, cmisObject);
        return (Document) cmisObject;
    }

    private List<LeosObjectProperties> wrapIntoLeosObjectProperties(ItemIterable<CmisObject> cmisObjects) {
        final List<LeosObjectProperties> leosProperties = new ArrayList<>();
        if (cmisObjects != null) {
            for (CmisObject obj : cmisObjects) {
                leosProperties.add(wrapIntoLeosObject(obj));
            }
        }
        return leosProperties;
    }

    private List<LeosFileProperties> wrapIntoLeosFileProperties(List<Document> cmisDocObjects) {
        final List<LeosFileProperties> leosFileObjects = new ArrayList<>();
        if (cmisDocObjects != null) {
            for (CmisObject obj : cmisDocObjects) {
                if (obj != null) {
                    if (LeosTypeId.LEOS_DOCUMENT.value().equals(obj.getType().getId())) {
                        LeosDocument leosDocument = new LeosDocument((Document) obj);
                        enrichLeosObject(leosDocument, obj);
                        leosFileObjects.add(leosDocument);
                    }
                    else if (LeosTypeId.LEOS_FILE.value().equals(obj.getType().getId())) {
                        LeosFile leosFile = new LeosFile((Document) obj);
                        enrichLeosObject(leosFile, obj);
                        leosFileObjects.add(leosFile);
                    }
                }
            }
        }

        return leosFileObjects;
    }

    private <T extends LeosObject> T wrapIntoLeosObject(CmisObject cmisObject) {
        LeosObject leosObject = null;
        if (cmisObject != null) {
            if (LeosTypeId.LEOS_DOCUMENT.value().equals(cmisObject.getType().getId())) {
                leosObject= new LeosDocument((Document) cmisObject);
            }
            else if (LeosTypeId.LEOS_FOLDER.value().equals(cmisObject.getType().getId())) {
                leosObject = new LeosFolder((Folder)cmisObject);
            }
            else if (LeosTypeId.LEOS_FILE.value().equals(cmisObject.getType().getId())) {
                leosObject = new LeosFile((Document)cmisObject);
            }
            enrichLeosObject(leosObject, cmisObject);
        }

        return (T) leosObject; //TODO remove the unchecked cast
    }

    private void enrichLeosObject(LeosObject leosObject, CmisObject cmisObject) {
        leosObject.setContributors(getContributors(getContributorIds(cmisObject), getContributorNames(cmisObject)));
        leosObject.setAuthor(getUser(getAuthorId(cmisObject), getAuthorName(cmisObject)));
    }

    private String getAuthorName(CmisObject cmisObject) {
        return cmisObject.getProperty(LeosObject.AUTHOR_NAME).getValueAsString();
    }

    private String getAuthorId(CmisObject cmisObject) {
        return cmisObject.getProperty(LeosObject.AUTHOR_ID).getValueAsString();
    }

    private List<String> getContributorNames(CmisObject cmisObject) {
        List<String> list = new ArrayList<>();
        if (cmisObject.getProperty(LeosObject.CONTRIBUTOR_NAMES)!=null) {
            for (Object o : cmisObject.getProperty(LeosObject.CONTRIBUTOR_NAMES).getValues()) {
                list.add((String) o);
            }
        }
        return list;
    }

    public List<String> getContributorIds(CmisObject cmisObject) {
        List<String> list = new ArrayList<>();
        if (cmisObject.getProperty(LeosObject.CONTRIBUTOR_IDS)!=null) {
            for (Object o : cmisObject.getProperty(LeosObject.CONTRIBUTOR_IDS).getValues()) {
                list.add((String) o);
            }
        }
        return list;
    }

    private List<UserVO> getContributors(List<String> contributorIds, List<String> contributorNames) {
        List<UserVO> contributors = new ArrayList<>();
        int i = 0;
        int contributorNamesSize = contributorNames.size();
        for (String id : contributorIds) {
            if (i<contributorNamesSize) {
                contributors.add(getUser(id, contributorNames.get(i)));
            }
            i++;
        }
        return contributors;
    }

    private UserVO getUser (String id, String name) {
        UserVO userVO = new UserVO();
        userVO.setId(id);
        userVO.setName(name);
        return userVO;
    }

}
