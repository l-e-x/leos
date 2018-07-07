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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.europa.ec.leos.model.content.*;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.DocumentType;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectFactory;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.util.AbstractPageFetcher;
import org.apache.chemistry.opencmis.client.runtime.util.CollectionIterable;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.google.common.net.MediaType;

import eu.europa.ec.leos.repositories.support.cmis.OperationContextProvider;
import eu.europa.ec.leos.repositories.support.cmis.StorageProperties;
import eu.europa.ec.leos.repositories.support.cmis.StorageProperties.EditableProperty;
import eu.europa.ec.leos.test.support.LeosTest;

public class ContentRepositoryImplTest extends LeosTest {

    @Mock
    private Session session;

    @Mock
    private OperationContextProvider operationContextProvider;

    @InjectMocks
    private ContentRepositoryImpl contentRepository = new ContentRepositoryImpl();

    @Test(expected = IllegalArgumentException.class)
    public void test_getCmisObjectByPath_should_throwIllegalArgumentException_when_pathIsNull() {
        // setup
        String path = null;
        OperationContext oc = setupOperationContext();

        // exercise
        contentRepository.getCmisObjectByPath(path, oc);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getCmisObjectByPath_should_throwIllegalArgumentException_when_contextIsNull() {
        // setup
        String path = "/some/object/path";
        OperationContext oc = null;

        // exercise
        contentRepository.getCmisObjectByPath(path, oc);
    }

    @Test(expected = NullPointerException.class)
    public void test_getCmisObjectByPath_should_throwNullPointerException_when_cmisObjectIsNull() {
        // setup
        String path = "/some/object/path";
        OperationContext oc = setupOperationContext();
        CmisObject cmisObject = null;
        when(session.getObjectByPath(path, oc)).thenReturn(cmisObject);

        // exercise
        contentRepository.getCmisObjectByPath(path, oc);
    }

    @Test
    public void test_getCmisObjectByPath_should_returnNonnull() {
        // setup
        String id = "123";
        String path = "/some/object/path";
        OperationContext oc = setupOperationContext();
        CmisObject cmisObject = setupCmisObject(id);
        when(session.getObjectByPath(path, oc)).thenReturn(cmisObject);

        // exercise
        CmisObject result = contentRepository.getCmisObjectByPath(path, oc);

        // verify
        verify(session).getObjectByPath(same(path), same(oc));
        assertThat(result, is(sameInstance(cmisObject)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getCmisObjectById_should_throwIllegalArgumentException_when_idIsNull() {
        // setup
        String id = null;
        OperationContext oc = setupOperationContext();

        // exercise
        contentRepository.getCmisObjectById(id, oc);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getCmisObjectById_should_throwIllegalArgumentException_when_contextIsNull() {
        // setup
        String id = "123";
        OperationContext oc = null;

        // exercise
        contentRepository.getCmisObjectById(id, oc);
    }

    @Test(expected = NullPointerException.class)
    public void test_getCmisObjectById_should_throwNullPointerException_when_cmisObjectIsNull() {
        // setup
        String id = "123";
        OperationContext oc = setupOperationContext();
        CmisObject cmisObject = null;
        when(session.getObject(id, oc)).thenReturn(cmisObject);

        // exercise
        contentRepository.getCmisObjectById(id, oc);
    }

    @Test
    public void test_getCmisObjectById_should_returnNonnull() {
        // setup
        String id = "123";
        OperationContext oc = setupOperationContext();
        CmisObject cmisObject = setupCmisObject(id);
        when(session.getObject(id, oc)).thenReturn(cmisObject);

        // exercise
        CmisObject result = contentRepository.getCmisObjectById(id, oc);

        // verify
        verify(session).getObject(same(id), same(oc));
        assertThat(result, is(sameInstance(cmisObject)));
    }

    @Test
    public void test_getCmisFolderByPath_should_returnNonnull() {
        // setup
        String id = "123";
        String path = "/some/folder/path";
        OperationContext oc = setupOperationContext();
        Folder cmisFolder = setupCmisFolder(id);
        when(session.getObjectByPath(path, oc)).thenReturn(cmisFolder);

        // exercise
        Folder result = contentRepository.getCmisFolderByPath(path, oc);

        // verify
        verify(session).getObjectByPath(same(path), same(oc));
        assertThat(result, is(sameInstance(cmisFolder)));
    }

    @Test
    public void test_getCmisFolderById_should_returnNonnull() {
        // setup
        String id = "123";
        OperationContext oc = setupOperationContext();
        Folder cmisFolder = setupCmisFolder(id);
        when(session.getObject(id, oc)).thenReturn(cmisFolder);

        // exercise
        Folder result = contentRepository.getCmisFolderById(id, oc);

        // verify
        verify(session).getObject(same(id), same(oc));
        assertThat(result, is(sameInstance(cmisFolder)));
    }

    @Test
    public void test_getCmisDocumentByPath_should_returnNonnull() {
        // setup
        String id = "123";
        String path = "/some/document/path";
        OperationContext oc = setupOperationContext();
        Document cmisDocument = setupCmisDocument(id);
        when(session.getObjectByPath(path, oc)).thenReturn(cmisDocument);

        // exercise
        Document result = contentRepository.getCmisDocumentByPath(path, oc);

        // verify
        verify(session).getObjectByPath(same(path), same(oc));
        assertThat(result, is(sameInstance(cmisDocument)));
    }

    @Test
    public void test_getCmisDocumentById_should_returnNonnull() {
        // setup
        String leosId = "123";
        OperationContext oc = setupOperationContext();
        Document cmisDocument = setupCmisDocument(leosId);
        List<CmisObject> objects= new ArrayList<CmisObject>(Arrays.asList(cmisDocument));
        when(session.queryObjects(eq(LeosTypeId.LEOS_DOCUMENT.value()), anyString() , eq(false), eq(oc))).thenReturn( toItemIterable(objects));

        // exercise
        Document result = contentRepository.getCmisDocumentByLeosId(leosId, oc);

        // verify
        verify(session).queryObjects(eq(LeosTypeId.LEOS_DOCUMENT.value()),contains(leosId),eq(false) ,same(oc));
        assertThat(result, is(sameInstance(cmisDocument)));
    }

    @Test
    public void test_getDocumentVersions_should_returnVersionList() {
        // setup
        String id1 = "123";
        String id2 = "124";
        String idVSeries1 = "123vs";
        

        OperationContext oc = setupOperationContext();
        Document cmisDocumentV1 = setupCMISDocumentAtRepository(id1,idVSeries1);
        Document cmisDocumentV2 = setupCMISDocumentAtRepository(id2,idVSeries1);

        List<CmisObject> objects= new ArrayList<CmisObject>(Arrays.asList(cmisDocumentV1));
        when(session.queryObjects(eq(LeosTypeId.LEOS_DOCUMENT.value()), anyString() , eq(false), eq(oc))).thenReturn( toItemIterable(objects));
        when(operationContextProvider.getMinimalContext()).thenReturn(oc);
        
        ArrayList<Document> arraDoc=new ArrayList<Document>();
        arraDoc.add(cmisDocumentV1);
        arraDoc.add(cmisDocumentV2);
        when(cmisDocumentV1.getAllVersions(oc)).thenReturn(arraDoc);

        // exercise
        List<LeosFileProperties> result = contentRepository.getVersions(id1);

        // verify
        verify(session, atLeastOnce()).queryObjects(eq(LeosTypeId.LEOS_DOCUMENT.value()),contains(id1),eq(false) ,same(oc));
        assertThat(result.get(0).getVersionId(), is(equalTo(cmisDocumentV1.getId())));
        assertThat(result.get(1).getVersionId(), is(equalTo(cmisDocumentV2.getId())));

    }
    
    @Test(expected = IllegalArgumentException.class)
    public void test_browseFolder_should_throwIllegalArgumentException_when_pathIsNull() {
        // setup
        String path = null;

        // exercise
        contentRepository.browse(path);
    }

    @Test
    public void test_browseFolder_should_returnNonnull() {
        // setup
        String id = "123";
        String path = "/some/folder/path";
        OperationContext oc = setupOperationContext();
        Folder cmisFolder = setupCmisFolder(id);
        ItemIterable<CmisObject> cmisObjects = null;
        when(operationContextProvider.getMinimalContext()).thenReturn(oc);
        when(session.getObjectByPath(path, oc)).thenReturn(cmisFolder);
        when(cmisFolder.getChildren(oc)).thenReturn(cmisObjects);

        // exercise
        List<LeosObjectProperties> result = contentRepository.browse(path);

        // verify
        verify(session).getObjectByPath(same(path), same(oc));
        verify(cmisFolder).getChildren(same(oc));
        assertThat(result, is(notNullValue()));
        assertThat(result, is(empty()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_retrieveDocumentById_should_throwIllegalArgumentException_when_idIsNull() {
        // setup
        String id = null;

        // exercise
        contentRepository.retrieveById(id, LeosDocument.class);
    }

    @Test
    public void test_retrieveDocumentById_should_returnNonnull() {
        // setup
        String versionId = "123";
        String leosId = "12300";
        OperationContext oc = setupOperationContext();
        Document cmisDocument = setupCMISDocumentAtRepository(versionId,leosId);
        List<CmisObject> objects= new ArrayList<CmisObject>(Arrays.asList(cmisDocument));
        when(session.queryObjects(eq(LeosTypeId.LEOS_DOCUMENT.value()), anyString() , eq(false), eq(oc))).thenReturn( toItemIterable(objects));
        when(operationContextProvider.getMinimalContext()).thenReturn(oc);

        // exercise
        LeosDocument result = contentRepository.retrieveById(leosId, LeosDocument.class);

        // verify
        verify(session).queryObjects(eq(LeosTypeId.LEOS_DOCUMENT.value()),contains(leosId),eq(false) ,same(oc));
        assertThat(result, is(notNullValue(LeosDocument.class)));
        assertThat(result.getLeosId(), is(equalTo(leosId)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_retrieveDocumentByPath_should_throwIllegalArgumentException_when_pathIsNull() {
        // setup
        String path = null;

        // exercise
        contentRepository.retrieveByPath(path, LeosDocument.class);
    }

    @Test
    public void test_retrieveDocumentByPath_should_returnNonnull() {
        // setup
        String verisonId = "123";
        String leosId = "12300";
        String path = "/some/document/path";
        OperationContext oc = setupOperationContext();
        Document cmisDocument = setupCMISDocumentAtRepository(verisonId,leosId);
        when(operationContextProvider.getMinimalContext()).thenReturn(oc);
        when(session.getObjectByPath(path, oc)).thenReturn(cmisDocument);

        // exercise
        LeosDocument result = contentRepository.retrieveByPath(path, LeosDocument.class);

        // verify
        verify(session).getObjectByPath(same(path), same(oc));
        assertThat(result, is(notNullValue(LeosDocument.class)));
        assertThat(result.getLeosId(), is(equalTo(leosId)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_updateDocumentContent_should_throwIllegalArgumentException_when_idIsNull() {
        // setup
        String id = null;
        String streamName = "test stream";
        MediaType streamMimeType = MediaType.OCTET_STREAM;
        byte[] byteContent = new byte[]{1, 2, 3};
        Long streamLength = Long.valueOf(byteContent.length);
        InputStream inputStream = new ByteArrayInputStream(byteContent);
        
        StorageProperties newProperties = new StorageProperties(LeosTypeId.LEOS_DOCUMENT);
        newProperties.set(EditableProperty.CHECKIN_COMMENT, "operation.intial.version");
        newProperties.set(EditableProperty.NAME, streamName);
        
        // exercise
        contentRepository.updateContent(id, streamLength, streamMimeType, inputStream, newProperties, LeosDocument.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_updateDocumentContent_should_throwIllegalArgumentException_when_streamIsNull() {
        // setup
        String id = "123";
        String streamName = "test stream";
        MediaType streamMimeType = MediaType.OCTET_STREAM;
        Long streamLength = -1L;
        InputStream inputStream = null;
        StorageProperties newProperties = new StorageProperties(LeosTypeId.LEOS_DOCUMENT);
        newProperties.set(EditableProperty.CHECKIN_COMMENT, "operation.intial.version");
        newProperties.set(EditableProperty.NAME, streamName);
        
        // exercise
        contentRepository.updateContent(id, streamLength, streamMimeType, inputStream,newProperties, LeosDocument.class);
    }

    @Test
    public void test_updateDocumentContent_should_returnNonnull() throws Exception {
        // setup
        String id = "123";
        String streamName = "test stream";
        MediaType streamMimeType = MediaType.OCTET_STREAM;
        byte[] byteContent = new byte[]{1, 2, 3};
        Long streamLength = Long.valueOf(byteContent.length);
        InputStream inputStream = new ByteArrayInputStream(byteContent);

        OperationContext oc = setupOperationContext();
        Document cmisDocument = setupCMISDocumentAtRepository(id);
        when(operationContextProvider.getMinimalContext()).thenReturn(oc);
        List<CmisObject> objects= new ArrayList<CmisObject>(Arrays.asList(cmisDocument));
        when(session.queryObjects(eq(LeosTypeId.LEOS_DOCUMENT.value()), anyString() , eq(false), eq(oc))).thenReturn( toItemIterable(objects));

        ContentStream contentStream = setupContentStream(streamName, streamLength, streamMimeType, inputStream);
        ObjectFactory streamFactory = setupContentStreamFactory(streamName, streamLength, streamMimeType, contentStream);
        when(session.getObjectFactory()).thenReturn(streamFactory);

        boolean overwriteStream = true;
        when(cmisDocument.setContentStream(contentStream, overwriteStream)).thenReturn(cmisDocument);
        when(cmisDocument.getContentStream()).thenReturn(contentStream);
        
        StorageProperties newProperties = new StorageProperties(LeosTypeId.LEOS_DOCUMENT);
        newProperties.set(EditableProperty.CHECKIN_COMMENT, "operation.intial.version");
        newProperties.set(EditableProperty.NAME, streamName);

        // exercise
        LeosDocument result = contentRepository.updateContent(id, streamLength, streamMimeType, inputStream, newProperties, LeosDocument.class);

        // verify
        verify(session, atLeastOnce()).queryObjects(eq(LeosTypeId.LEOS_DOCUMENT.value()),contains(id),eq(false) ,same(oc));
        verify(streamFactory).createContentStream(same(streamName), eq(streamLength), eq(streamMimeType.toString()),
                Mockito.any(InputStream.class));
        verify(cmisDocument).setContentStream(same(contentStream), eq(overwriteStream));
        assertThat(result, is(notNullValue(LeosDocument.class)));
        assertThat(IOUtils.toByteArray(result.getContentStream()), is(equalTo(byteContent)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_createDocumentFromPath_should_throwIllegalArgumentException_when_sourcePathIsNull() {
        // setup
        String sourceDocumentPath = null;
        String targetDocumentName = "target document";
        String targetFolderPath = "/target/folder/path";
        StorageProperties newProperties = new StorageProperties(LeosTypeId.LEOS_DOCUMENT);
        newProperties.set(EditableProperty.CHECKIN_COMMENT, "operation.intial.version");
        newProperties.set(EditableProperty.NAME, targetDocumentName);
        // exercise
        contentRepository.copy(sourceDocumentPath, targetFolderPath,newProperties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_createDocumentFromPath_should_throwIllegalArgumentException_when_targetNameIsNull() {
        // setup
        String sourceDocumentPath = "/source/document/path";
        String targetDocumentName = null;
        String targetFolderPath = "/target/folder/path";
        
        StorageProperties newProperties = new StorageProperties(LeosTypeId.LEOS_DOCUMENT);
        newProperties.set(EditableProperty.CHECKIN_COMMENT, "operation.intial.version");
        newProperties.set(EditableProperty.NAME, targetDocumentName);

        // exercise
        contentRepository.copy(sourceDocumentPath, targetFolderPath, newProperties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_createDocumentFromPath_should_throwIllegalArgumentException_when_targetPathIsNull() {
        // setup
        String sourceDocumentPath = "/source/document/path";
        String targetDocumentName = "target document";
        String targetFolderPath = null;
        StorageProperties newProperties = new StorageProperties(LeosTypeId.LEOS_DOCUMENT);
        newProperties.set(EditableProperty.CHECKIN_COMMENT, "operation.intial.version");
        newProperties.set(EditableProperty.NAME, targetDocumentName);
        
        // exercise
        contentRepository.copy(sourceDocumentPath, targetFolderPath, newProperties );
    }

    @Test
    public void test_createDocumentFromPath_should_returnNonnull() {
        // setup
        String sourceDocumentPath = "/source/document/path";
        String targetDocumentTitle = "target document";
        String targetFolderPath = "/target/folder/path";

        OperationContext oc = setupOperationContext();
        when(operationContextProvider.getMinimalContext()).thenReturn(oc);

        String sourceleosId = "123";
        Document sourceDocument = setupCmisDocument(sourceleosId);
        when(session.getObjectByPath(sourceDocumentPath, oc)).thenReturn(sourceDocument);

        String targetFolderId = "456";
        Folder targetFolder = setupCmisFolder(targetFolderId);
        when(session.getObjectByPath(targetFolderPath, oc)).thenReturn(targetFolder);

        String targetFileId = "789";
        Document targetDocument = setupCMISDocumentAtRepository(targetFileId);
        
        Property<Object> title= mock(Property.class);
        when(title.getValueAsString()).thenReturn(targetDocumentTitle);
        when(targetDocument.getProperty(eq(LeosDocumentProperties.TITLE))).thenReturn(title);
        when(sourceDocument.copy(eq(targetFolder), anyMapOf(String.class, Object.class), eq(VersioningState.MAJOR),
                anyListOf(Policy.class), anyListOf(Ace.class), anyListOf(Ace.class), eq(oc))).thenReturn(targetDocument);

        StorageProperties newProperties = new StorageProperties(LeosTypeId.LEOS_DOCUMENT);
        newProperties.set(EditableProperty.CHECKIN_COMMENT, "operation.intial.version");
        newProperties.set(EditableProperty.NAME, targetDocumentTitle);
        
        // exercise
        LeosDocument result = contentRepository.copy(sourceDocumentPath, targetFolderPath,newProperties);

        // verify
        verify(session).getObjectByPath(same(sourceDocumentPath), same(oc));
        verify(session).getObjectByPath(same(targetFolderPath), same(oc));
        verify(sourceDocument).copy(eq(targetFolder), anyMapOf(String.class, Object.class), eq(VersioningState.MAJOR),
                anyListOf(Policy.class), anyListOf(Ace.class), anyListOf(Ace.class), eq(oc));
        assertThat(result, is(notNullValue(LeosDocument.class)));
        assertThat(result.getTitle(), is(equalTo(targetDocumentTitle)));
    }

    private OperationContext setupOperationContext() {
        return mock(OperationContext.class);
    }

    private CmisObject setupCmisObject(String id) {
        // CMIS does not provide a generic base type id,
        // so let's use the ITEM base type id for testing
        BaseTypeId baseTypeId = BaseTypeId.CMIS_ITEM;
        ObjectType objectType = mock(ObjectType.class);
        when(objectType.getId()).thenReturn("cmis:object");
        CmisObject cmisObject = mock(CmisObject.class);
        when(cmisObject.getBaseTypeId()).thenReturn(baseTypeId);
        when(cmisObject.getType()).thenReturn(objectType);
        when(cmisObject.getId()).thenReturn(id);
        return cmisObject;
    }

    private Folder setupCmisFolder(String id) {
        BaseTypeId baseTypeId = BaseTypeId.CMIS_FOLDER;
        ObjectType objectType = mock(ObjectType.class);
        when(objectType.getId()).thenReturn(BaseTypeId.CMIS_FOLDER.value());
        Folder cmisFolder = mock(Folder.class);
        when(cmisFolder.getBaseTypeId()).thenReturn(baseTypeId);
        when(cmisFolder.getType()).thenReturn(objectType);
        when(cmisFolder.getId()).thenReturn(id);
        return cmisFolder;
    }

    private Document setupCmisDocument(String objectId) {
        BaseTypeId baseTypeId = BaseTypeId.CMIS_DOCUMENT;
        ObjectType objectType = mock(ObjectType.class);
        //when(objectType.getId()).thenReturn(ObjectType.DOCUMENT_BASETYPE_ID);
        when(objectType.getId()).thenReturn(BaseTypeId.CMIS_DOCUMENT.value());
        Document cmisDocument = mock(Document.class);
        when(cmisDocument.getBaseTypeId()).thenReturn(baseTypeId);
        when(cmisDocument.getType()).thenReturn(objectType);
        when(cmisDocument.getId()).thenReturn(objectId);
        when(cmisDocument.isLatestVersion()).thenReturn(true);
        when (session.getTypeDefinition(LeosTypeId.LEOS_DOCUMENT.value())).thenReturn(objectType);
        
        return cmisDocument;
    }

    private Document setupCMISDocumentAtRepository(String id) {
    	return setupCMISDocumentAtRepository("0000", id);
    }

    private Document setupCMISDocumentAtRepository(String versionId, String leosId) {
        BaseTypeId baseTypeId = BaseTypeId.CMIS_DOCUMENT;
        
        ObjectType objectParentType = mock(DocumentType.class);
        when(objectParentType.getId()).thenReturn(LeosTypeId.LEOS_FILE.value());
        
        ObjectType objectType = mock(DocumentType.class);
        when(objectType.getId()).thenReturn(LeosTypeId.LEOS_DOCUMENT.value());
        when(objectType.getParentType()).thenReturn(objectParentType);
        
        Document cmisDocument = mock(Document.class);
        when(cmisDocument.getBaseTypeId()).thenReturn(baseTypeId);
        when(cmisDocument.getType()).thenReturn(objectType);
        when(cmisDocument.getId()).thenReturn(versionId);
        when(cmisDocument.getVersionSeriesId()).thenReturn(leosId); 
        when(cmisDocument.isLatestVersion()).thenReturn(true);
        when (session.getTypeDefinition(LeosTypeId.LEOS_DOCUMENT.value())).thenReturn(objectType);
        //role
        Property<Object> authorId= mock(Property.class);
        Property<Object> authorName= mock(Property.class);
        when(cmisDocument.getProperty(eq(LeosObject.AUTHOR_ID))).thenReturn(authorId);
        when(cmisDocument.getProperty(eq(LeosObject.AUTHOR_NAME))).thenReturn(authorName);
        return cmisDocument;
    }
    private ContentStream setupContentStream(String name, long length, MediaType mimeType, InputStream inputStream) {
        return new ContentStreamImpl(name, BigInteger.valueOf(length), mimeType.toString(), inputStream);
    }

    private ObjectFactory setupContentStreamFactory(String name, long length, MediaType mimeType, ContentStream contentStream) {
        ObjectFactory objectFactory = mock(ObjectFactory.class);
        when(objectFactory.createContentStream(eq(name), eq(length), eq(mimeType.toString()),
                Mockito.any(InputStream.class))).thenReturn(contentStream);
        return objectFactory;
    }
    
    private ItemIterable<CmisObject> toItemIterable(final List<CmisObject> objects){

       CollectionIterable<CmisObject> temp= new CollectionIterable<CmisObject>(new AbstractPageFetcher<CmisObject>(10) {
            @Override
            protected AbstractPageFetcher.Page<CmisObject> fetchPage(long skipCount) {
                // SET INTO PAGE objects
                List<CmisObject> page = new ArrayList<CmisObject>();
                if (objects != null) {
                    for (CmisObject object : objects) {
                        page.add(object);
                    }
                }
                return new AbstractPageFetcher.Page<CmisObject>(page, objects.size(), objects.iterator().hasNext());
            }
        });
        return temp;
    }
}
