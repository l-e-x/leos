/**
 * Copyright 2015 European Commission
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

import com.google.common.net.MediaType;
import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.repositories.content.ContentRepository;
import eu.europa.ec.leos.services.exception.LeosDocumentLockException;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.support.xml.XmlHelper;
import eu.europa.ec.leos.support.xml.XmlMetaDataProcessor;
import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.vo.MetaDataVO;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

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

        String proprietaryTag = "proprietary";
        String proprietaryXml = "proprietary xml";
        byte[] updatedProprietaryContent = "updated document content with proprietary xml".getBytes(UTF_8);

        when(lockingService.isDocumentLockedFor(docId, "userId", null)).thenReturn(true);

        when(xmlMetaDataProcessor.createXmlForProprietary(metadataVO)).thenReturn(proprietaryXml);
        when(xmlContentProcessor.replaceElementWithTagName(docContent, proprietaryTag, proprietaryXml)).thenReturn(updatedProprietaryContent);

        String docPurposeTag = "docPurpose";
        String docPurposeXml = XmlHelper.buildTag(docPurposeTag, metadataVO.getDocPurpose());
        byte[] updateddocPurposeContent = "updated document content with title xml".getBytes(UTF_8);
        LeosDocument updatedTitleDocument = setupLeosDocumentMock(docId, docName, updateddocPurposeContent);
        when(xmlContentProcessor.replaceElementWithTagName(updatedProprietaryContent, docPurposeTag, docPurposeXml)).thenReturn(updateddocPurposeContent);
        when(xmlContentProcessor.doXMLPostProcessing(updateddocPurposeContent)).thenReturn(updateddocPurposeContent);
        when(contentRepository.updateContent(same(docId), anyString(), anyLong(), isA(MediaType.class), isA(InputStream.class), same("operation.metadata.updated"), same(LeosDocument.class))).thenReturn(
                updatedTitleDocument);

        String updatedDocName = metaDocType + " " + metaDocPurpose;
        LeosDocument updatedNameDocument = setupLeosDocumentMock(docId, updatedDocName, updateddocPurposeContent);
        when(contentRepository.rename(docId, updatedDocName,("operation.document.renamed"))).thenReturn(updatedNameDocument);

        // DO THE ACTUAL CALL
        contentServiceImpl.updateMetaData(originalDocument, "userId", metadataVO);

        verify(xmlMetaDataProcessor).createXmlForProprietary(metadataVO);
        verify(xmlContentProcessor).replaceElementWithTagName(docContent, proprietaryTag, proprietaryXml);
        verify(xmlContentProcessor).replaceElementWithTagName(updatedProprietaryContent, docPurposeTag, docPurposeXml);
        verify(xmlContentProcessor).doXMLPostProcessing(updateddocPurposeContent);
        verify(contentRepository).updateContent(same(docId), anyString(), anyLong(), isA(MediaType.class),  isA(InputStream.class),same("operation.metadata.updated"), same(LeosDocument.class));
        verify(contentRepository).rename(docId, updatedDocName,("operation.document.renamed"));
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

        String proprietaryTag = "proprietary";
        String proprietaryXml = "proprietary xml";

        when(lockingService.isDocumentLockedFor(docId, "sessionId", null)).thenReturn(true);

        when(xmlMetaDataProcessor.createXmlForProprietary(metadataVO)).thenReturn(proprietaryXml);
        when(xmlContentProcessor.replaceElementWithTagName(docContent, proprietaryTag, proprietaryXml)).thenThrow(new IllegalArgumentException());

        String metaTag = "meta";
        byte[] updatedMetaContent = "updated document content with meta and proprietary xml".getBytes(UTF_8);
        when(xmlContentProcessor.appendElementToTag(docContent, metaTag, proprietaryXml)).thenReturn(updatedMetaContent);

        String docPurposeTag = "docPurpose";
        String docPurposeXml = XmlHelper.buildTag(docPurposeTag, metadataVO.getDocPurpose());
        byte[] updateddocPurposeContent = "updated document content with title xml".getBytes(UTF_8);
        LeosDocument updatedTitleDocument = setupLeosDocumentMock(docId, docName, updateddocPurposeContent);
        when(xmlContentProcessor.replaceElementWithTagName(updatedMetaContent, docPurposeTag, docPurposeXml)).thenReturn(updateddocPurposeContent);
        when(contentRepository.updateContent(same(docId), anyString(), anyLong(), isA(MediaType.class),isA(InputStream.class), same("operation.metadata.updated"), same(LeosDocument.class))).thenReturn(updatedTitleDocument);
        when(xmlContentProcessor.doXMLPostProcessing(updateddocPurposeContent)).thenReturn(updateddocPurposeContent);

        String updatedDocName = metaDocType + " " + metaDocPurpose;
        LeosDocument updatedNameDocument = setupLeosDocumentMock(docId, updatedDocName, updateddocPurposeContent);
        when(contentRepository.rename(docId, updatedDocName,("operation.document.renamed"))).thenReturn(updatedNameDocument);

        // DO THE ACTUAL CALL
        contentServiceImpl.updateMetaData(originalDocument, "sessionId", metadataVO);

        verify(xmlMetaDataProcessor).createXmlForProprietary(metadataVO);
        verify(xmlContentProcessor).replaceElementWithTagName(docContent, proprietaryTag, proprietaryXml);
        verify(xmlContentProcessor).appendElementToTag(docContent, metaTag, proprietaryXml);
        verify(xmlContentProcessor).replaceElementWithTagName(updatedMetaContent, docPurposeTag, docPurposeXml);
        verify(xmlContentProcessor).doXMLPostProcessing(updateddocPurposeContent);
        verify(contentRepository).updateContent(same(docId), anyString(), anyLong(), isA(MediaType.class), isA(InputStream.class),same("operation.metadata.updated"), same(LeosDocument.class));
        verify(contentRepository).rename(docId, updatedDocName,("operation.document.renamed"));
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

        String proprietaryTag = "proprietary";
        String proprietaryXml = "proprietary xml";

        when(lockingService.isDocumentLockedFor(docId, "sessionId", null)).thenReturn(true);

        when(xmlMetaDataProcessor.createXmlForProprietary(metadataVO)).thenReturn(proprietaryXml);
        when(xmlContentProcessor.replaceElementWithTagName(docContent, proprietaryTag, proprietaryXml)).thenThrow(new IllegalArgumentException());

        String metaTag = "meta";
        when(xmlContentProcessor.appendElementToTag(docContent, metaTag, proprietaryXml)).thenThrow(new IllegalArgumentException());

        String billTag = "bill";
        String metaXml = XmlHelper.buildTag(metaTag, proprietaryXml);
        byte[] updatedBillContent = "updated document content with bill, meta and proprietary xml".getBytes(UTF_8);
        when(xmlContentProcessor.appendElementToTag(docContent, billTag, metaXml)).thenReturn(updatedBillContent);

        String docPurposeTag = "docPurpose";
        String docPurposeXml = XmlHelper.buildTag(docPurposeTag, metadataVO.getDocPurpose());
        byte[] updateddocPurposeContent = "updated document content with title xml".getBytes(UTF_8);
        LeosDocument updatedTitleDocument = setupLeosDocumentMock(docId, docName, updateddocPurposeContent);
        when(xmlContentProcessor.replaceElementWithTagName(updatedBillContent, docPurposeTag, docPurposeXml)).thenReturn(updateddocPurposeContent);
        when(contentRepository.updateContent(same(docId), anyString(), anyLong(), isA(MediaType.class), isA(InputStream.class), same("operation.metadata.updated"), same(LeosDocument.class))).thenReturn(
                updatedTitleDocument);
        when(xmlContentProcessor.doXMLPostProcessing(updateddocPurposeContent)).thenReturn(updateddocPurposeContent);

        String updatedDocName = metaDocType + " " + metaDocPurpose;
        LeosDocument updatedNameDocument = setupLeosDocumentMock(docId, updatedDocName, updateddocPurposeContent);
        when(contentRepository.rename(docId, updatedDocName,("operation.document.renamed"))).thenReturn(updatedNameDocument);

        // DO THE ACTUAL CALL
        contentServiceImpl.updateMetaData(originalDocument, "sessionId", metadataVO);

        verify(xmlMetaDataProcessor).createXmlForProprietary(metadataVO);
        verify(xmlContentProcessor).replaceElementWithTagName(docContent, proprietaryTag, proprietaryXml);
        verify(xmlContentProcessor).appendElementToTag(docContent, metaTag, proprietaryXml);
        verify(xmlContentProcessor).appendElementToTag(docContent, billTag, metaXml);
        verify(xmlContentProcessor).replaceElementWithTagName(updatedBillContent, docPurposeTag, docPurposeXml);
        verify(xmlContentProcessor).doXMLPostProcessing(updateddocPurposeContent);
        verify(contentRepository).updateContent(same(docId), anyString(), anyLong(), isA(MediaType.class), isA(InputStream.class), same("operation.metadata.updated"), same(LeosDocument.class));
        verify(contentRepository).rename(docId, updatedDocName,("operation.document.renamed"));
        verifyNoMoreInteractions(xmlMetaDataProcessor, xmlContentProcessor, contentRepository);
    }

    @Test
    public void test_getMetaData() {

        LeosDocument originalDocument = mock(LeosDocument.class);
        byte[] originalByteContent = new byte[]{1, 2, 3};
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));
        MetaDataVO metaDataVO = new MetaDataVO();

        when(xmlContentProcessor.getElementByNameAndId(originalByteContent, "proprietary", null)).thenReturn("meta");
        when(xmlMetaDataProcessor.createMetaDataVOFromXml("meta")).thenReturn(metaDataVO);

        MetaDataVO result = contentServiceImpl.getMetaData(originalDocument);

        assertThat(result, is(metaDataVO));
    }

    private LeosDocument setupLeosDocumentMock(String id, String name, byte[] content) {
        LeosDocument document = mock(LeosDocument.class);
        when(document.getLeosId()).thenReturn(id);
        when(document.getName()).thenReturn(name);
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
