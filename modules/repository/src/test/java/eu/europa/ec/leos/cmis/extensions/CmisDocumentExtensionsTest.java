/*
 * Copyright 2019 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.cmis.extensions;

import eu.europa.ec.leos.cmis.domain.ContentImpl;
import eu.europa.ec.leos.cmis.domain.SourceImpl;
import eu.europa.ec.leos.cmis.mapping.CmisProperties;
import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.document.*;
import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.MemorandumMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.ProposalMetadata;
import io.atlassian.fugue.Option;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CmisMetadataExtensions.class)
public class CmisDocumentExtensionsTest {

    private final static String DOC_ID = "DOCUMENT_ID";
    private final static String DOC_NAME = "DOCUMENT_NAME";
    private final static String DOC_CREATED_BY = "DOCUMENT_CREATED_BY";
    private final static Instant DOC_CREATION_INSTANT = LocalDateTime.of(2019, 05, 28, 23, 20).toInstant(ZoneOffset.UTC);
    private final static String DOC_LAST_MODIFIED_BY = "DEVELOPER";
    private final static Instant DOC_LAST_MODIFICATION_INSTANT = LocalDateTime.of(2019, 05, 28, 23, 30).toInstant(ZoneOffset.UTC);
    private final static String DOC_VERSION_SERIES_ID = "DOCUMENT_SERIES_ID";
    private final static String DOC_VERSION_LABEL = "DOCUMENT_VERSION_LABEL";
    private final static String DOC_VERSION_COMMENT = "DOCUMENT_VERSION_COMMENT";
    private final static Boolean DOC_IS_MAJOR_VERSION = Boolean.TRUE;
    private final static Boolean DOC_IS_LATEST_VERSION = Boolean.TRUE;
    private final static String DOC_TITLE = "DOCUMENT_TITLE";
    private final static Map<String, String> DOC_COLLABORATORS = Collections.singletonMap("KEY", "VALUE");
    private final static List<String> DOC_MILESTONE_COMMENTS = Arrays.asList("COMM_1", "COMM_2", "COMM3");
    private final static String DOC_INITIAL_CREATED_BY = "DOCUMENT_INITIAL_CREATED_BY_";
    private final static Instant DOC_INITIAL_CREATION_INSTANT = LocalDateTime.of(2019, 05, 28, 23, 40).toInstant(ZoneOffset.UTC);
    private final static Option<Content> DOC_CONTENT = Option.option(new ContentImpl("testFile", "mime type", 23, new SourceImpl(new ByteArrayInputStream(new byte[]{0, 1, 2}))));

    @Test
    public void test_getCollaborators_MapIsCorrect() {
        //setup
        Property<Object> property = mock(Property.class);
        when(property.getValues()).thenReturn(Arrays.asList("testUser1::OWNER", "testUser2::OWNER", "testUser3::CONTRIBUTOR"));
        Document cmisDocument = mock(Document.class);
        when(cmisDocument.getProperty(eq(CmisProperties.COLLABORATORS.getId()))).thenReturn(property);

        //make call
        Map<String, String> resultUsers = CmisDocumentExtensions.getCollaborators(cmisDocument);

        //verify
        assertThat(resultUsers.size(), is(3));
        assertThat(resultUsers.get("testUser1"), equalTo("OWNER"));
        assertThat(resultUsers.get("testUser2"), equalTo("OWNER"));
        assertThat(resultUsers.get("testUser3"), equalTo("CONTRIBUTOR"));

    }

    @Test
    public void test_getCollaborators_IfIncorrectValuesAreIgnored() {
        //setup
        Property<Object> property = mock(Property.class);
        when(property.getValues()).thenReturn(Arrays.asList("testUser1::OWNER", "testUser2::INCORRECT"));
        Document cmisDocument = mock(Document.class);
        when(cmisDocument.getProperty(eq(CmisProperties.COLLABORATORS.getId()))).thenReturn(property);

        //make call
        Map<String, String> resultUsers = CmisDocumentExtensions.getCollaborators(cmisDocument);

        //verify
        assertThat(resultUsers.size(), is(2));
        assertThat(resultUsers.get("testUser1"), equalTo("OWNER"));
    }

    @Test
    public void test_getCollaborators_IfIncorrectFormatIgnored() {
        //setup
        Property<Object> property = mock(Property.class);
        when(property.getValues()).thenReturn(Arrays.asList("testUser1::OWNER", "XYZ"));
        Document cmisDocument = mock(Document.class);
        when(cmisDocument.getProperty(eq(CmisProperties.COLLABORATORS.getId()))).thenReturn(property);

        //make call
        Map<String, String> resultUsers = CmisDocumentExtensions.getCollaborators(cmisDocument);

        //verify
        assertThat(resultUsers.size(), is(1));
        assertThat(resultUsers.get("testUser1"), equalTo("OWNER"));

    }

    @Test
    public void test_toLeosDocument_IfProposalType() throws IOException {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.PROPOSAL);
        cmisDocument = addXmlDocumentProperties(cmisDocument);
        when(cmisDocument.getPropertyValue(CmisProperties.INITIAL_CREATED_BY.getId())).thenReturn(DOC_INITIAL_CREATED_BY);
        when(cmisDocument.getPropertyValue(CmisProperties.INITIAL_CREATION_DATE.getId())).thenReturn(GregorianCalendar.from(DOC_INITIAL_CREATION_INSTANT.atZone(ZoneId.of("UTC"))));

        Option<ProposalMetadata> proposalMetadata = Option.none();
        mockStatic(CmisMetadataExtensions.class);
        when(CmisMetadataExtensions.getProposalMetadataOption(cmisDocument)).thenReturn(proposalMetadata);

        //make call
        Proposal proposal = CmisDocumentExtensions.toLeosDocument(cmisDocument, Proposal.class, true);

        //verify
        assertThat(proposal, is(notNullValue()));
        checkLeosDocument(proposal);
        checkXmlDocument(proposal);

        assertThat(proposal.getInitialCreatedBy(), is(DOC_INITIAL_CREATED_BY));
        assertThat(proposal.getInitialCreationInstant(), is(DOC_INITIAL_CREATION_INSTANT));
        assertThat(proposal.getMetadata(), is(sameInstance(proposalMetadata)));
    }

    @Test(expected = IllegalStateException.class)
    public void test_toLeosDocument_IfProposalType_when_IncompatibleTypes() {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.PROPOSAL);

        //make call
        CmisDocumentExtensions.toLeosDocument(cmisDocument, Annex.class, true);
    }

    @Test
    public void test_toLeosDocument_IfMemorandumType() throws IOException {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.MEMORANDUM);
        cmisDocument = addXmlDocumentProperties(cmisDocument);
        Option<MemorandumMetadata> memorandumMetadata = Option.none();
        mockStatic(CmisMetadataExtensions.class);
        when(CmisMetadataExtensions.getMemorandumMetadataOption(cmisDocument)).thenReturn(memorandumMetadata);

        //make call
        Memorandum memorandum = CmisDocumentExtensions.toLeosDocument(cmisDocument, Memorandum.class, true);

        //verify
        assertThat(memorandum, is(notNullValue()));
        checkLeosDocument(memorandum);
        checkXmlDocument(memorandum);

        assertThat(memorandum.getMetadata(), is(sameInstance(memorandumMetadata)));
    }

    @Test(expected = IllegalStateException.class)
    public void test_toLeosDocument_IfMemorandum_when_IncompatibleTypes() {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.MEMORANDUM);

        //make call
        CmisDocumentExtensions.toLeosDocument(cmisDocument, Annex.class, true);
    }

    @Test
    public void test_toLeosDocument_IfBillType() throws IOException {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.BILL);
        cmisDocument = addXmlDocumentProperties(cmisDocument);
        Option<BillMetadata> billMetadata = Option.none();
        mockStatic(CmisMetadataExtensions.class);
        when(CmisMetadataExtensions.getBillMetadataOption(cmisDocument)).thenReturn(billMetadata);

        //make call
        Bill bill = CmisDocumentExtensions.toLeosDocument(cmisDocument, Bill.class, true);

        //verify
        assertThat(bill, is(notNullValue()));
        checkLeosDocument(bill);
        checkXmlDocument(bill);

        assertThat(bill.getMetadata(), is(sameInstance(billMetadata)));
    }

    @Test(expected = IllegalStateException.class)
    public void test_toLeosDocument_IfBill_when_IncompatibleTypes() {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.BILL);

        //make call
        CmisDocumentExtensions.toLeosDocument(cmisDocument, Annex.class, true);
    }

    @Test
    public void test_toLeosDocument_IfAnnexType() throws IOException {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.ANNEX);
        cmisDocument = addXmlDocumentProperties(cmisDocument);
        Option<AnnexMetadata> annexMetadata = Option.none();
        mockStatic(CmisMetadataExtensions.class);
        when(CmisMetadataExtensions.getAnnexMetadataOption(cmisDocument)).thenReturn(annexMetadata);

        //make call
        Annex annex = CmisDocumentExtensions.toLeosDocument(cmisDocument, Annex.class, true);

        //verify
        assertThat(annex, is(notNullValue()));
        checkLeosDocument(annex);
        checkXmlDocument(annex);

        assertThat(annex.getMetadata(), is(sameInstance(annexMetadata)));
    }

    @Test(expected = IllegalStateException.class)
    public void test_toLeosDocument_IfAnnex_when_IncompatibleTypes() {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.ANNEX);

        //make call
        CmisDocumentExtensions.toLeosDocument(cmisDocument, Memorandum.class, true);
    }

    @Test
    public void test_toLeosDocument_IfMediaDocumentType() throws IOException {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.MEDIA);

        //make call
        MediaDocument mediaDocument = CmisDocumentExtensions.toLeosDocument(cmisDocument, MediaDocument.class, true);

        //verify
        assertThat(mediaDocument, is(notNullValue()));
        checkLeosDocument(mediaDocument);
    }

    @Test(expected = IllegalStateException.class)
    public void test_toLeosDocument_IfMediaDocument_when_IncompatibleTypes() {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.MEDIA);

        //make call
        CmisDocumentExtensions.toLeosDocument(cmisDocument, Annex.class, true);
    }

    @Test
    public void test_toLeosDocument_IfConfigDocumentType() throws IOException {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.CONFIG);

        //make call
        ConfigDocument configDocument = CmisDocumentExtensions.toLeosDocument(cmisDocument, ConfigDocument.class, true);

        //verify
        assertThat(configDocument, is(notNullValue()));
        checkLeosDocument(configDocument);
    }

    @Test(expected = IllegalStateException.class)
    public void test_toLeosDocument_IfConfigDocument_when_IncompatibleTypes() {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.CONFIG);

        //make call
        CmisDocumentExtensions.toLeosDocument(cmisDocument, Annex.class, true);
    }

    @Test
    public void test_toLeosDocument_IfLegDocumentType() throws IOException {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.LEG);
        String jobId = "DOCUMENT_JOB_ID";
        when(cmisDocument.getPropertyValue(CmisProperties.JOB_ID.getId())).thenReturn(jobId);

        Instant jobDate = LocalDateTime.of(2019, 05, 28, 23, 30).toInstant(ZoneOffset.UTC);
        when(cmisDocument.getPropertyValue(CmisProperties.JOB_DATE.getId())).thenReturn(GregorianCalendar.from(jobDate.atZone(ZoneId.of("UTC"))));

        when(cmisDocument.getPropertyValue(CmisProperties.STATUS.getId())).thenReturn(LeosLegStatus.IN_CONSULTATION.name());

        Property milestoneProperty = mock(Property.class);
        when(milestoneProperty.getValues()).thenReturn(DOC_MILESTONE_COMMENTS);
        when(cmisDocument.getProperty(eq(CmisProperties.MILESTONE_COMMENTS.getId()))).thenReturn(milestoneProperty);

        //make call
        LegDocument legDocument = CmisDocumentExtensions.toLeosDocument(cmisDocument, LegDocument.class, true);

        //verify
        assertThat(legDocument, is(notNullValue()));
        checkLeosDocument(legDocument);
        assertThat(legDocument.getJobId(), is(jobId));
        assertThat(legDocument.getJobDate(), is(jobDate));
        assertThat(legDocument.getStatus(), is(LeosLegStatus.IN_CONSULTATION));
        assertThat(legDocument.getMilestoneComments(), is(DOC_MILESTONE_COMMENTS));
    }

    @Test(expected = IllegalStateException.class)
    public void test_toLeosDocument_IfLegDocument_when_IncompatibleTypes() {
        //setup
        Document cmisDocument = setupLeosDocument(LeosCategory.LEG);

        //make call
        CmisDocumentExtensions.toLeosDocument(cmisDocument, Annex.class, true);
    }

    @Test
    public void test_getCreationInstant_IfNull() {
        //setup
        Document cmisDocument = mock(Document.class);

        //make call
        Instant instant = CmisDocumentExtensions.getCreationInstant(cmisDocument);

        assertThat(instant, is(Instant.MIN));
    }

    @Test
    public void test_getLastModificationInstant_IfNull() {
        //setup
        Document cmisDocument = mock(Document.class);

        //make call
        Instant instant = CmisDocumentExtensions.getLastModificationInstant(cmisDocument);

        assertThat(instant, is(Instant.MIN));
    }

    @Test
    public void test_getInitialCreationInstant_IfNull() {
        //setup
        Document cmisDocument = mock(Document.class);

        //make call
        Instant instant = CmisDocumentExtensions.getInitialCreationInstant(cmisDocument);

        assertThat(instant, is(Instant.MIN));
    }

    @Test
    public void test_getInitialCreatedBy_IfInitialCreatedBy_Provided() {
        //setup
        Document cmisDocument = mock(Document.class);
        when(cmisDocument.getPropertyValue(CmisProperties.INITIAL_CREATED_BY.getId())).thenReturn(DOC_INITIAL_CREATED_BY);
        when(cmisDocument.getCreatedBy()).thenReturn(DOC_CREATED_BY);

        //make call
        String initialCReatedBy = CmisDocumentExtensions.getInitialCreatedBy(cmisDocument);

        assertThat(initialCReatedBy, is(DOC_INITIAL_CREATED_BY));
    }

    @Test
    public void test_getInitialCreatedBy_IfInitialCreatedBy_Null() {
        //setup
        Document cmisDocument = mock(Document.class);
        when(cmisDocument.getPropertyValue(CmisProperties.INITIAL_CREATED_BY.getId())).thenReturn(null);
        when(cmisDocument.getCreatedBy()).thenReturn(DOC_CREATED_BY);

        //make call
        String initialCReatedBy = CmisDocumentExtensions.getInitialCreatedBy(cmisDocument);

        assertThat(initialCReatedBy, is(DOC_CREATED_BY));
    }

    private void checkLeosDocument(LeosDocument leosDocument) throws IOException {
        assertThat(leosDocument.getId(), is(DOC_ID));
        assertThat(leosDocument.getName(), is(DOC_NAME));
        assertThat(leosDocument.getCreatedBy(), is(DOC_CREATED_BY));
        assertThat(leosDocument.getCreationInstant(), is(DOC_CREATION_INSTANT));
        assertThat(leosDocument.getLastModifiedBy(), is(DOC_LAST_MODIFIED_BY));
        assertThat(leosDocument.getLastModificationInstant(), is(DOC_LAST_MODIFICATION_INSTANT));
        assertThat(leosDocument.getVersionSeriesId(), is(DOC_VERSION_SERIES_ID));
        assertThat(leosDocument.getVersionLabel(), is(DOC_VERSION_LABEL));
        assertThat(leosDocument.getVersionComment(), is(DOC_VERSION_COMMENT));
        assertThat(leosDocument.isMajorVersion(), is(DOC_IS_MAJOR_VERSION));
        assertThat(leosDocument.isLatestVersion(), is(DOC_IS_LATEST_VERSION));

        assertThat(leosDocument.getContent().get().getFileName(), is(DOC_CONTENT.get().getFileName()));
        assertThat(leosDocument.getContent().get().getMimeType(), is(DOC_CONTENT.get().getMimeType()));
        assertThat(leosDocument.getContent().get().getLength(), is(DOC_CONTENT.get().getLength()));
        assertThat(leosDocument.getContent().get().getSource().getInputStream().available(), is(DOC_CONTENT.get().getSource().getInputStream().available()));
    }

    private void checkXmlDocument(XmlDocument xmlDocument) {
        assertThat(xmlDocument.getTitle(), is(DOC_TITLE));
        assertThat(xmlDocument.getCollaborators(), is(DOC_COLLABORATORS));
        assertThat(xmlDocument.getMilestoneComments(), is(DOC_MILESTONE_COMMENTS));
    }

    private Document setupLeosDocument(LeosCategory leosCategory) {
        Document cmisDocument = mock(Document.class);

        when(cmisDocument.getPropertyValue(CmisProperties.DOCUMENT_CATEGORY.getId())).thenReturn(leosCategory.name());

        when(cmisDocument.getId()).thenReturn(DOC_ID);
        when(cmisDocument.getName()).thenReturn(DOC_NAME);
        when(cmisDocument.getCreatedBy()).thenReturn(DOC_CREATED_BY);
        when(cmisDocument.getCreationDate()).thenReturn(GregorianCalendar.from(DOC_CREATION_INSTANT.atZone(ZoneId.of("UTC"))));
        when(cmisDocument.getLastModifiedBy()).thenReturn(DOC_LAST_MODIFIED_BY);
        when(cmisDocument.getLastModificationDate()).thenReturn(GregorianCalendar.from(DOC_LAST_MODIFICATION_INSTANT.atZone(ZoneId.of("UTC"))));
        when(cmisDocument.getVersionSeriesId()).thenReturn(DOC_VERSION_SERIES_ID);
        when(cmisDocument.getVersionLabel()).thenReturn(DOC_VERSION_LABEL);
        when(cmisDocument.getCheckinComment()).thenReturn(DOC_VERSION_COMMENT);
        when(cmisDocument.isMajorVersion()).thenReturn(DOC_IS_MAJOR_VERSION);
        when(cmisDocument.isLatestVersion()).thenReturn(DOC_IS_LATEST_VERSION);

        ContentStream contentStream = mock(ContentStream.class);
        when(contentStream.getFileName()).thenReturn(DOC_CONTENT.get().getFileName());
        when(contentStream.getMimeType()).thenReturn(DOC_CONTENT.get().getMimeType());
        when(contentStream.getLength()).thenReturn(DOC_CONTENT.get().getLength());
        when(contentStream.getStream()).thenReturn(DOC_CONTENT.get().getSource().getInputStream());
        when(cmisDocument.getContentStream()).thenReturn(contentStream);

        return cmisDocument;
    }

    private Document addXmlDocumentProperties(Document cmisDocument) {
        when(cmisDocument.getPropertyValue(CmisProperties.DOCUMENT_TITLE.getId())).thenReturn(DOC_TITLE);

        Property collaboratorProperty = mock(Property.class);
        when(collaboratorProperty.getValues()).thenReturn(singletonList("KEY::VALUE"));
        when(cmisDocument.getProperty(eq(CmisProperties.COLLABORATORS.getId()))).thenReturn(collaboratorProperty);

        Property milestoneProperty = mock(Property.class);
        when(milestoneProperty.getValues()).thenReturn(DOC_MILESTONE_COMMENTS);
        when(cmisDocument.getProperty(eq(CmisProperties.MILESTONE_COMMENTS.getId()))).thenReturn(milestoneProperty);
        return cmisDocument;
    }

}