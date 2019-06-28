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

import eu.europa.ec.leos.cmis.mapping.CmisProperties;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.metadata.*;
import io.atlassian.fugue.Option;
import org.apache.chemistry.opencmis.client.api.Document;
import org.junit.Test;

import java.math.BigInteger;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CmisMetadataExtensionsTest {

    private final static String METADATA_STAGE = "DOCUMENT METADATA_STAGE";
    private final static String METADATA_TYPE = "DOCUMENT METADATA_TYPE";
    private final static String METADATA_PURPOSE = "DOCUMENT METADATA_PURPOSE";
    private final static String METADATA_DOCTEMPLATE = "DOCUMENT METADATA_DOCTEMPLATE";
    private final static String METADATA_REF = "DOCUMENT METADATA_REF";
    private final static String DOCUMENT_TEMPLATE = "DOCUMENT_TEMPLATE";
    private final static String DOCUMENT_LANGUAGE = "DOCUMENT_LANGUAGE";

    @Test
    public void test_getProposalMetadataOption() {
        //setup
        Document cmisDocument = setupCommonDocument();

        //make call
        Option<ProposalMetadata> proposalMetadataOption = CmisMetadataExtensions.getProposalMetadataOption(cmisDocument);

        //verify
        assertThat(proposalMetadataOption.isEmpty(), is(false));
        ProposalMetadata proposalMetadata = proposalMetadataOption.get();
        assertThat(proposalMetadata, is(notNullValue()));
        assertThat(proposalMetadata.getCategory(), is(LeosCategory.PROPOSAL));
        verifyCommonMetadata(proposalMetadata);
    }

    @Test
    public void test_getProposalMetadataOption_IfNullValues() {
        //setup
        Document cmisDocument = mock(Document.class);

        //make call
        Option<ProposalMetadata> proposalMetadataOption = CmisMetadataExtensions.getProposalMetadataOption(cmisDocument);

        //verify
        assertThat(proposalMetadataOption.isEmpty(), is(true));
    }

    @Test
    public void test_getMemorandumMetadataOption() {
        //setup
        Document cmisDocument = setupCommonDocument();

        //make call
        Option<MemorandumMetadata> memorandumMetadataOption = CmisMetadataExtensions.getMemorandumMetadataOption(cmisDocument);

        //verify
        assertThat(memorandumMetadataOption.isEmpty(), is(false));
        MemorandumMetadata memorandumMetadata = memorandumMetadataOption.get();
        assertThat(memorandumMetadata, is(notNullValue()));
        assertThat(memorandumMetadata.getCategory(), is(LeosCategory.MEMORANDUM));
        verifyCommonMetadata(memorandumMetadata);
    }

    @Test
    public void test_getMemorandumMetadataOption_IfNullValues() {
        //setup
        Document cmisDocument = mock(Document.class);

        //make call
        Option<MemorandumMetadata> memorandumMetadataOption = CmisMetadataExtensions.getMemorandumMetadataOption(cmisDocument);

        //verify
        assertThat(memorandumMetadataOption.isEmpty(), is(true));
    }

    @Test
    public void test_getBillMetadataOption() {
        //setup
        Document cmisDocument = setupCommonDocument();

        //make call
        Option<BillMetadata> billMetadataOption = CmisMetadataExtensions.getBillMetadataOption(cmisDocument);

        //verify
        assertThat(billMetadataOption.isEmpty(), is(false));
        BillMetadata billMetadata = billMetadataOption.get();
        assertThat(billMetadata, is(notNullValue()));
        assertThat(billMetadata.getCategory(), is(LeosCategory.BILL));
        verifyCommonMetadata(billMetadata);
    }

    @Test
    public void test_getBillMetadataOption_IfNullValues() {
        //setup
        Document cmisDocument = mock(Document.class);

        //make call
        Option<BillMetadata> billMetadataOption = CmisMetadataExtensions.getBillMetadataOption(cmisDocument);

        //verify
        assertThat(billMetadataOption.isEmpty(), is(true));
    }

    @Test
    public void test_getAnnexMetadataOption() {
        //setup
        Document cmisDocument = setupCommonDocument();
        BigInteger ANNEX_INDEX = new BigInteger("20");
        String ANNEX_NUMBER = "DOCUMENT ANNEX_NUMBER";
        String ANNEX_TITLE = "DOCUMENT ANNEX_TITLE";
        when(cmisDocument.getPropertyValue(CmisProperties.ANNEX_INDEX.getId())).thenReturn(ANNEX_INDEX);
        when(cmisDocument.getPropertyValue(CmisProperties.ANNEX_NUMBER.getId())).thenReturn(ANNEX_NUMBER);
        when(cmisDocument.getPropertyValue(CmisProperties.ANNEX_TITLE.getId())).thenReturn(ANNEX_TITLE);

        //make call
        Option<AnnexMetadata> annexMetadataOption = CmisMetadataExtensions.getAnnexMetadataOption(cmisDocument);

        //verify
        assertThat(annexMetadataOption.isEmpty(), is(false));
        AnnexMetadata annexMetadata = annexMetadataOption.get();
        assertThat(annexMetadata, is(notNullValue()));
        assertThat(annexMetadata.getCategory(), is(LeosCategory.ANNEX));
        verifyCommonMetadata(annexMetadata);

        assertThat(annexMetadata.getIndex(), is(ANNEX_INDEX.intValue()));
        assertThat(annexMetadata.getNumber(), is(ANNEX_NUMBER));
        assertThat(annexMetadata.getTitle(), is(ANNEX_TITLE));
    }

    @Test
    public void test_getAnnexMetadataOption_IfNullValues() {
        //setup
        Document cmisDocument = mock(Document.class);

        //make call
        Option<AnnexMetadata> annexMetadataOption = CmisMetadataExtensions.getAnnexMetadataOption(cmisDocument);

        //verify
        assertThat(annexMetadataOption.isEmpty(), is(true));
    }

    private void verifyCommonMetadata(LeosMetadata leosMetadata) {
        assertThat(leosMetadata.getStage(), is(METADATA_STAGE));
        assertThat(leosMetadata.getType(), is(METADATA_TYPE));
        assertThat(leosMetadata.getPurpose(), is(METADATA_PURPOSE));
        assertThat(leosMetadata.getDocTemplate(), is(METADATA_DOCTEMPLATE));
        assertThat(leosMetadata.getRef(), is(METADATA_REF));
        assertThat(leosMetadata.getTemplate(), is(DOCUMENT_TEMPLATE));
        assertThat(leosMetadata.getLanguage(), is(DOCUMENT_LANGUAGE));
    }

    private Document setupCommonDocument() {
        Document cmisDocument = mock(Document.class);

        when(cmisDocument.getPropertyValue(CmisProperties.METADATA_STAGE.getId())).thenReturn(METADATA_STAGE);
        when(cmisDocument.getPropertyValue(CmisProperties.METADATA_TYPE.getId())).thenReturn(METADATA_TYPE);
        when(cmisDocument.getPropertyValue(CmisProperties.METADATA_PURPOSE.getId())).thenReturn(METADATA_PURPOSE);
        when(cmisDocument.getPropertyValue(CmisProperties.METADATA_DOCTEMPLATE.getId())).thenReturn(METADATA_DOCTEMPLATE);
        when(cmisDocument.getPropertyValue(CmisProperties.METADATA_REF.getId())).thenReturn(METADATA_REF);
        when(cmisDocument.getPropertyValue(CmisProperties.DOCUMENT_TEMPLATE.getId())).thenReturn(DOCUMENT_TEMPLATE);
        when(cmisDocument.getPropertyValue(CmisProperties.DOCUMENT_LANGUAGE.getId())).thenReturn(DOCUMENT_LANGUAGE);

        return cmisDocument;
    }
}