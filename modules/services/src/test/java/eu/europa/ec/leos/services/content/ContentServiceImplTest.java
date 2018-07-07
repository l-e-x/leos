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
package eu.europa.ec.leos.services.content;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.notNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;


import com.google.common.net.MediaType;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.repositories.content.ContentRepository;
import eu.europa.ec.leos.repositories.support.cmis.StorageProperties;
import eu.europa.ec.leos.services.exception.LeosDocumentLockException;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.support.xml.XmlHelper;
import eu.europa.ec.leos.support.xml.XmlMetaDataProcessor;
import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.vo.MetaDataVO;

public class ContentServiceImplTest extends LeosTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private XmlContentProcessor xmlContentProcessor;

    @Mock
    private LockingService lockingService;

    @Mock
    private XmlMetaDataProcessor xmlMetaDataProcessor;

    @InjectMocks
    private ContentServiceImpl contentServiceImpl;

    @Test
    public void test_updateMetaData() throws Exception {
        String docId = "doc Id";
        String docName = "original doc name";
        byte[] docContent = "original doc content".getBytes(UTF_8);
        LeosDocument originalDocument = setupLeosDocumentMock(docId, docName, docContent);
        
        String metaDocStage = "metadata doc Stage";
        String metaDocType = "metadata doc type";
        String metaDocPurpose = "metadata doc title";
        MetaDataVO metadataVO = setupMetadataVO(metaDocStage, metaDocType, metaDocPurpose);
        Map<String, String> hm = new HashMap<String, String>();
        hm.put(XmlMetaDataProcessor.DEFAULT_DOC_PURPOSE_ID, metadataVO.getDocPurpose());
        
        String proprietaryTag = "meta";
        String proprietaryXml = "proprietary xml";
        byte[] updatedProprietaryContent = "updated document content with proprietary xml".getBytes(UTF_8);

        when(lockingService.isDocumentLockedFor(docId, "userId", null)).thenReturn(true);
        
        when(xmlContentProcessor.getElementByNameAndId(docContent, "meta", null)).thenReturn(proprietaryXml);
        when(xmlMetaDataProcessor.toXML(metadataVO,proprietaryXml)).thenReturn(proprietaryXml);
        when(xmlContentProcessor.replaceElementsWithTagName(docContent, proprietaryTag, proprietaryXml)).thenReturn(updatedProprietaryContent);

        String docPurposeTag = "docPurpose";
        String docPurposeXml = XmlHelper.buildTag(docPurposeTag, metadataVO.getDocPurpose());
        byte[] updateddocPurposeContent = "updated document content with title xml".getBytes(UTF_8);

        LeosDocument updatedTitleDocument = setupLeosDocumentMock(docId, docName, updateddocPurposeContent);
        //when(xmlContentProcessor.replaceElementsWithTagName(updatedProprietaryContent, docPurposeTag, docPurposeXml)).thenReturn(updateddocPurposeContent);
        when(xmlContentProcessor.updateReferedAttributes(updatedProprietaryContent,hm)).thenReturn(updateddocPurposeContent);
        when(xmlContentProcessor.doXMLPostProcessing(updateddocPurposeContent)).thenReturn(updateddocPurposeContent);
        
        when(contentRepository.updateContent(same(docId), anyLong(), isA(MediaType.class), isA(InputStream.class), isA(StorageProperties.class), same(LeosDocument.class))).thenReturn(
                updatedTitleDocument);

        String updatedDocName = metaDocStage +" "+metaDocType + " " + metaDocPurpose;
        LeosDocument updatedNameDocument = setupLeosDocumentMock(docId, updatedDocName, updateddocPurposeContent);

        // DO THE ACTUAL CALL
        contentServiceImpl.updateMetaData(originalDocument, "userId", metadataVO);

        verify(xmlMetaDataProcessor).toXML(metadataVO,proprietaryXml);
        verify(xmlContentProcessor).getElementByNameAndId(docContent, "meta", null);
        verify(xmlContentProcessor).replaceElementsWithTagName(docContent, proprietaryTag, proprietaryXml);
        //verify(xmlContentProcessor).replaceElementsWithTagName(updatedProprietaryContent, docPurposeTag, docPurposeXml);
        verify(xmlContentProcessor).updateReferedAttributes(updatedProprietaryContent,hm);
        verify(xmlContentProcessor).doXMLPostProcessing(updateddocPurposeContent);

        verify(contentRepository).updateContent(same(docId), anyLong(), isA(MediaType.class),  isA(InputStream.class),isA(StorageProperties.class), same(LeosDocument.class));
        verifyNoMoreInteractions(xmlMetaDataProcessor, xmlContentProcessor, contentRepository);
    }

    @Test(expected = LeosDocumentLockException.class)
    public void test_updateMetaData_lock() throws Exception {
        String docId = "doc Id";
        String docName = "original doc name";
        byte[] docContent = "original doc content".getBytes(UTF_8);
        LeosDocument originalDocument = setupLeosDocumentMock(docId, docName, docContent);

        String metaDocStage = "metadata doc Stage";
        String metaDocType = "metadata doc type";
        String metaDocPurpose = "metadata doc title";
        MetaDataVO metadataVO = setupMetadataVO(metaDocStage, metaDocType, metaDocPurpose);

        // DO THE ACTUAL CALL
        contentServiceImpl.updateMetaData(originalDocument, "sessionId", metadataVO);
    }

    @Test
    public void test_updateMetaData_when_noProprietaryFound_shouldInsert() throws Exception {
        String docId = "doc Id";
        String docName = "original doc name";
        byte[] docContent = "original doc content".getBytes(UTF_8);
        LeosDocument originalDocument = setupLeosDocumentMock(docId, docName, docContent);

        String metaDocStage = "metadata doc Stage";
        String metaDocType = "metadata doc type";
        String metaDocPurpose = "metadata doc title";
        MetaDataVO metadataVO = setupMetadataVO(metaDocStage, metaDocType, metaDocPurpose);
        Map<String, String> hm = new HashMap<String, String>();
        hm.put(XmlMetaDataProcessor.DEFAULT_DOC_PURPOSE_ID, metadataVO.getDocPurpose());
        
        String metaTag = "meta";
        String proprietaryXml = "proprietary xml";

        when(lockingService.isDocumentLockedFor(docId, "sessionId", null)).thenReturn(true);
        when(xmlContentProcessor.getElementByNameAndId(docContent, "meta", null)).thenReturn(proprietaryXml);
        when(xmlMetaDataProcessor.toXML(metadataVO,proprietaryXml)).thenReturn(proprietaryXml);
        when(xmlContentProcessor.replaceElementsWithTagName(docContent, metaTag, proprietaryXml)).thenThrow(new IllegalArgumentException());

        byte[] updatedMetaContent = "updated document content with meta and proprietary xml".getBytes(UTF_8);
        when(xmlContentProcessor.appendElementToTag(docContent, "bill", proprietaryXml)).thenReturn(updatedMetaContent);

        //String docPurposeTag = "docPurpose";
        //String docPurposeXml = XmlHelper.buildTag(docPurposeTag, metadataVO.getDocPurpose());
        byte[] updateddocPurposeContent = "updated document content with title xml".getBytes(UTF_8);
        LeosDocument updatedTitleDocument = setupLeosDocumentMock(docId, docName, updateddocPurposeContent);
        //when(xmlContentProcessor.replaceElementsWithTagName(updatedMetaContent, docPurposeTag, docPurposeXml)).thenReturn(updateddocPurposeContent);
        when(xmlContentProcessor.updateReferedAttributes(updatedMetaContent,hm)).thenReturn(updateddocPurposeContent);
        when(xmlContentProcessor.doXMLPostProcessing(updateddocPurposeContent)).thenReturn(updateddocPurposeContent);
        when(contentRepository.updateContent(same(docId), anyLong(), isA(MediaType.class),isA(InputStream.class), isA(StorageProperties.class), same(LeosDocument.class))).thenReturn(updatedTitleDocument);

        String updatedDocName =metaDocStage +" "+ metaDocType + " " + metaDocPurpose;
        LeosDocument updatedNameDocument = setupLeosDocumentMock(docId, updatedDocName, updateddocPurposeContent);

        // DO THE ACTUAL CALL
        contentServiceImpl.updateMetaData(originalDocument, "sessionId", metadataVO);

        verify(xmlContentProcessor).getElementByNameAndId(docContent, "meta", null);
        verify(xmlMetaDataProcessor).toXML(metadataVO,proprietaryXml);
        verify(xmlContentProcessor).replaceElementsWithTagName(docContent, metaTag, proprietaryXml);
        verify(xmlContentProcessor).appendElementToTag(docContent, "bill", proprietaryXml);
        //verify(xmlContentProcessor).replaceElementsWithTagName(updatedMetaContent, docPurposeTag, docPurposeXml);
        verify(xmlContentProcessor).updateReferedAttributes(updatedMetaContent,hm);
        verify(xmlContentProcessor).doXMLPostProcessing(updateddocPurposeContent);
        verify(contentRepository).updateContent(same(docId), anyLong(), isA(MediaType.class), isA(InputStream.class),isA(StorageProperties.class), same(LeosDocument.class));
        verifyNoMoreInteractions(xmlMetaDataProcessor, xmlContentProcessor, contentRepository);
    }

    @Test
    public void test_updateMetaData_when_noMetaFound_shouldInsert() throws Exception {
        String docId = "doc Id";
        String docName = "original doc name";
        byte[] docContent = "original doc content".getBytes(UTF_8);
        LeosDocument originalDocument = setupLeosDocumentMock(docId, docName, docContent);

        String metaDocStage = "metadata doc Stage";
        String metaDocType = "metadata doc type";
        String metaDocPurpose = "metadata doc title";
        MetaDataVO metadataVO = setupMetadataVO(metaDocStage, metaDocType, metaDocPurpose);
        Map<String, String> hm = new HashMap<String, String>();
        hm.put(XmlMetaDataProcessor.DEFAULT_DOC_PURPOSE_ID, metadataVO.getDocPurpose());
        
        String metaTag = "meta";
        String metaXml = "proprietary xml";

        when(lockingService.isDocumentLockedFor(docId, "sessionId", null)).thenReturn(true);
        when(xmlContentProcessor.getElementByNameAndId(docContent, "meta", null)).thenReturn(metaXml);
        when(xmlMetaDataProcessor.toXML(metadataVO,metaXml)).thenReturn(metaXml);
        when(xmlContentProcessor.replaceElementsWithTagName(docContent, metaTag, metaXml)).thenThrow(new IllegalArgumentException());

        String billTag = "bill";
        byte[] updatedBillContent = "updated document content with bill, meta and proprietary xml".getBytes(UTF_8);
        when(xmlContentProcessor.appendElementToTag(docContent, billTag, metaXml)).thenReturn(updatedBillContent);

        String docPurposeTag = "docPurpose";
        String docPurposeXml = XmlHelper.buildTag(docPurposeTag, metadataVO.getDocPurpose());
        byte[] updateddocPurposeContent = "updated document content with title xml".getBytes(UTF_8);
        LeosDocument updatedTitleDocument = setupLeosDocumentMock(docId, docName, updateddocPurposeContent);
        //when(xmlContentProcessor.replaceElementsWithTagName(updatedBillContent, docPurposeTag, docPurposeXml)).thenReturn(updateddocPurposeContent);
        when(contentRepository.updateContent(same(docId), anyLong(), isA(MediaType.class), isA(InputStream.class), isA(StorageProperties.class), same(LeosDocument.class))).thenReturn(
                updatedTitleDocument);
        when(xmlContentProcessor.updateReferedAttributes(updatedBillContent,hm)).thenReturn(updateddocPurposeContent);
        when(xmlContentProcessor.doXMLPostProcessing(updateddocPurposeContent)).thenReturn(updateddocPurposeContent);

        String updatedDocName =metaDocStage +" "+ metaDocType + " " + metaDocPurpose;
        LeosDocument updatedNameDocument = setupLeosDocumentMock(docId, updatedDocName, updateddocPurposeContent);

        // DO THE ACTUAL CALL
        contentServiceImpl.updateMetaData(originalDocument, "sessionId", metadataVO);

        verify(xmlMetaDataProcessor).toXML(metadataVO,metaXml);
        verify(xmlContentProcessor).getElementByNameAndId(docContent, "meta", null);
        verify(xmlContentProcessor).replaceElementsWithTagName(docContent, metaTag, metaXml);
        verify(xmlContentProcessor).appendElementToTag(docContent, billTag, metaXml);
        //verify(xmlContentProcessor).replaceElementsWithTagName(updatedBillContent, docPurposeTag, docPurposeXml);
        verify(xmlContentProcessor).updateReferedAttributes(updatedBillContent,hm);
        verify(xmlContentProcessor).doXMLPostProcessing(updateddocPurposeContent);
        verify(contentRepository).updateContent(same(docId),  anyLong(), isA(MediaType.class), isA(InputStream.class), isA(StorageProperties.class), same(LeosDocument.class));
        verifyNoMoreInteractions(xmlMetaDataProcessor, xmlContentProcessor, contentRepository);
    }

    @Test
    public void test_getMetaData() {

        LeosDocument originalDocument = mock(LeosDocument.class);
        byte[] originalByteContent = new byte[]{1, 2, 3};
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));
        MetaDataVO metaDataVO = new MetaDataVO();

        when(xmlContentProcessor.getElementByNameAndId(originalByteContent, "meta", null)).thenReturn("meta");
        when(xmlMetaDataProcessor.fromXML("meta")).thenReturn(metaDataVO);

        MetaDataVO result = contentServiceImpl.getMetaData(originalDocument);

        assertThat(result, is(metaDataVO));
    }

    @Test
    public void test_getAncestorsIdsForElementId_when_elementIdPassed_return_matchedAncestorsIds() {
        LeosDocument document = mock(LeosDocument.class);
        byte[] originalByteContent = new byte[]{1, 2, 3};
        when(document.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));
        String elementId = "xyz";
        List<String> stubAncestorsIds = Arrays.asList("ab", "cd", "a1");
        when(xmlContentProcessor.getAncestorsIdsForElementId((byte[]) notNull(), same(elementId))).thenReturn(stubAncestorsIds);
        //Actual call
        List<String> returnedAncestorsIds = contentServiceImpl.getAncestorsIdsForElementId(document, elementId);
        assertThat(stubAncestorsIds, is(returnedAncestorsIds));
    }

    private LeosDocument setupLeosDocumentMock(String id, String name, byte[] content) {
        LeosDocument document = mock(LeosDocument.class);
        when(document.getLeosId()).thenReturn(id);
        when(document.getName()).thenReturn(name);
        when(document.getTitle()).thenReturn(name);
        InputStream stream = new ByteArrayInputStream(content);
        when(document.getContentStream()).thenReturn(stream);
        return document;
    }

    private MetaDataVO setupMetadataVO(String docStage,String docType, String docPurpose) {
        MetaDataVO metaDataVO = new MetaDataVO();
        metaDataVO.setDocStage(docStage);
        metaDataVO.setDocType(docType);
        metaDataVO.setDocPurpose(docPurpose);
        return metaDataVO;
    }
}
