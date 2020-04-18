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

import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.MemorandumMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.ProposalMetadata;
import org.junit.Test;

import java.util.Map;

import static eu.europa.ec.leos.cmis.mapping.CmisProperties.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class LeosMetadataExtensionsTest {

    private final static String METADATA_STAGE_VALUE = "stage";
    private final static String METADATA_TYPE_VALUE = "type";
    private final static String METADATA_PURPOSE_VALUE = "purpose";
    private final static String DOCUMENT_TEMPLATE_VALUE = "template";
    private final static String DOCUMENT_LANGUAGE_VALUE = "language";
    private final static String METADATA_DOCTEMPLATE_VALUE = "docTemplate";
    private final static String METADATA_REF_VALUE = "ref";
    private final static String DOCUMENT_OBJECT_ID_VALUE = "title";

    @Test
    public void toCmisProperties_IfProposalMetadata() {
        //setup
        ProposalMetadata proposalMetadata = new ProposalMetadata(METADATA_STAGE_VALUE, METADATA_TYPE_VALUE, METADATA_PURPOSE_VALUE,
                DOCUMENT_TEMPLATE_VALUE, DOCUMENT_LANGUAGE_VALUE, METADATA_DOCTEMPLATE_VALUE, METADATA_REF_VALUE, DOCUMENT_OBJECT_ID_VALUE, "0.1.0");

        //make call
        Map<String, ? extends Object> metadata = LeosMetadataExtensions.toCmisProperties(proposalMetadata);

        //verify
        verifyCommonCmisProperties(metadata, METADATA_TYPE_VALUE, METADATA_REF_VALUE, "stage type purpose");
    }

    @Test
    public void toCmisProperties_IfProposalMetadata_whenSomeFieldsNull() {
        //setup
        ProposalMetadata proposalMetadata = new ProposalMetadata(METADATA_STAGE_VALUE, null, METADATA_PURPOSE_VALUE,
                DOCUMENT_TEMPLATE_VALUE, DOCUMENT_LANGUAGE_VALUE, METADATA_DOCTEMPLATE_VALUE, null, DOCUMENT_OBJECT_ID_VALUE, "0.1.0");

        //make call
        Map<String, ? extends Object> metadata = LeosMetadataExtensions.toCmisProperties(proposalMetadata);

        //verify
        verifyCommonCmisProperties(metadata, null, null, "stage purpose");

    }

    @Test
    public void toCmisProperties_IfMemorandumMetadata() {
        //setup
        MemorandumMetadata memorandumMetadata = new MemorandumMetadata(METADATA_STAGE_VALUE, METADATA_TYPE_VALUE, METADATA_PURPOSE_VALUE,
                DOCUMENT_TEMPLATE_VALUE, DOCUMENT_LANGUAGE_VALUE, METADATA_DOCTEMPLATE_VALUE, METADATA_REF_VALUE, DOCUMENT_OBJECT_ID_VALUE, "0.1.0");

        //make call
        Map<String, ? extends Object> metadata = LeosMetadataExtensions.toCmisProperties(memorandumMetadata);

        //verify
        verifyCommonCmisProperties(metadata, METADATA_TYPE_VALUE, METADATA_REF_VALUE, METADATA_TYPE_VALUE);
    }

    @Test
    public void toCmisProperties_IfMemorandumMetadata_whenSomeFieldsNull() {
        //setup
        MemorandumMetadata memorandumMetadata = new MemorandumMetadata(METADATA_STAGE_VALUE, null, METADATA_PURPOSE_VALUE,
                DOCUMENT_TEMPLATE_VALUE, DOCUMENT_LANGUAGE_VALUE, METADATA_DOCTEMPLATE_VALUE, null, DOCUMENT_OBJECT_ID_VALUE, "0.1.0");

        //make call
        Map<String, ? extends Object> metadata = LeosMetadataExtensions.toCmisProperties(memorandumMetadata);

        //verify
        verifyCommonCmisProperties(metadata, null, null, null);

    }

    @Test
    public void toCmisProperties_IfBillMetadata() {
        //setup
        BillMetadata billMetadata = new BillMetadata(METADATA_STAGE_VALUE, METADATA_TYPE_VALUE, METADATA_PURPOSE_VALUE,
                DOCUMENT_TEMPLATE_VALUE, DOCUMENT_LANGUAGE_VALUE, METADATA_DOCTEMPLATE_VALUE, METADATA_REF_VALUE, DOCUMENT_OBJECT_ID_VALUE, "0.1.0");

        //make call
        Map<String, ? extends Object> metadata = LeosMetadataExtensions.toCmisProperties(billMetadata);

        //verify
        verifyCommonCmisProperties(metadata, METADATA_TYPE_VALUE, METADATA_REF_VALUE, "stage type purpose");
    }

    @Test
    public void toCmisProperties_IfBillMetadata_whenSomeFieldsNull() {
        //setup
        BillMetadata billMetadata = new BillMetadata(METADATA_STAGE_VALUE, null, METADATA_PURPOSE_VALUE,
                DOCUMENT_TEMPLATE_VALUE, DOCUMENT_LANGUAGE_VALUE, METADATA_DOCTEMPLATE_VALUE, null, DOCUMENT_OBJECT_ID_VALUE, "0.1.0");

        //make call
        Map<String, ? extends Object> metadata = LeosMetadataExtensions.toCmisProperties(billMetadata);

        //verify
        verifyCommonCmisProperties(metadata, null, null, "stage purpose");

    }

    @Test
    public void toCmisProperties_IfAnnexMetadata() {
        //setup
        int annexIndex = 12;
        String annexNumber = "annexNumber";
        String annexTitle = "annexTitle";
        AnnexMetadata annexMetadata = new AnnexMetadata(METADATA_STAGE_VALUE, METADATA_TYPE_VALUE, METADATA_PURPOSE_VALUE,
                DOCUMENT_TEMPLATE_VALUE, DOCUMENT_LANGUAGE_VALUE, METADATA_DOCTEMPLATE_VALUE, METADATA_REF_VALUE, annexIndex,
                annexNumber, annexTitle, DOCUMENT_OBJECT_ID_VALUE, "0.1.0");

        //make call
        Map<String, ? extends Object> metadata = LeosMetadataExtensions.toCmisProperties(annexMetadata);

        //verify
        verifyCommonCmisProperties(metadata, METADATA_TYPE_VALUE, METADATA_REF_VALUE, METADATA_TYPE_VALUE);
        assertThat(metadata.get(ANNEX_INDEX.getId()), is(annexIndex));
        assertThat(metadata.get(ANNEX_NUMBER.getId()), is(annexNumber));
        assertThat(metadata.get(ANNEX_TITLE.getId()), is(annexTitle));
    }

    @Test
    public void toCmisProperties_IfAnnexMetadata_whenSomeFieldsNull() {
        //setup
        int annexIndex = 12;
        String annexNumber = "annexNumber";
        String annexTitle = "annexTitle";
        AnnexMetadata annexMetadata = new AnnexMetadata(METADATA_STAGE_VALUE, null, METADATA_PURPOSE_VALUE,
                DOCUMENT_TEMPLATE_VALUE, DOCUMENT_LANGUAGE_VALUE, METADATA_DOCTEMPLATE_VALUE, null, annexIndex,
                annexNumber, annexTitle, DOCUMENT_OBJECT_ID_VALUE, "0.1.0");

        //make call
        Map<String, ? extends Object> metadata = LeosMetadataExtensions.toCmisProperties(annexMetadata);

        //verify
        verifyCommonCmisProperties(metadata, null, null, null);
        assertThat(metadata.get(ANNEX_INDEX.getId()), is(annexIndex));
        assertThat(metadata.get(ANNEX_NUMBER.getId()), is(annexNumber));
        assertThat(metadata.get(ANNEX_TITLE.getId()), is(annexTitle));
    }

    private void verifyCommonCmisProperties(Map<String, ?> metadata, String expectedType, String expectedRefValue, String expectedTitleValue) {
        assertThat(metadata, notNullValue());
        assertThat(metadata.get(METADATA_STAGE.getId()), is(METADATA_STAGE_VALUE));
        assertThat(metadata.get(METADATA_TYPE.getId()), is(expectedType));
        assertThat(metadata.get(METADATA_PURPOSE.getId()), is(METADATA_PURPOSE_VALUE));
        assertThat(metadata.get(DOCUMENT_TEMPLATE.getId()), is(DOCUMENT_TEMPLATE_VALUE));
        assertThat(metadata.get(DOCUMENT_LANGUAGE.getId()), is(DOCUMENT_LANGUAGE_VALUE));
        assertThat(metadata.get(METADATA_DOCTEMPLATE.getId()), is(METADATA_DOCTEMPLATE_VALUE));

        assertThat(metadata.get(METADATA_REF.getId()), is(expectedRefValue));
        assertThat(metadata.get(DOCUMENT_TITLE.getId()), is(expectedTitleValue));
    }
}