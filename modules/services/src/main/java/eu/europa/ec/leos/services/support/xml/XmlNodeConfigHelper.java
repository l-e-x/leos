/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.services.support.xml;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosMetadata;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

@Component
public class XmlNodeConfigHelper {

    private static final Map<LeosCategory, Map<String, XmlNodeConfig>> All_CONFIG_MAP = new HashMap<>();

    public static final String DOC_PURPOSE_META = "docPurposeMeta";
    public static final String DOC_STAGE_META = "docStageMeta";
    public static final String DOC_TYPE_META = "docTypeMeta";

    public static final String DOC_PURPOSE_COVER = "docPurposeCover";
    public static final String DOC_STAGE_COVER = "docStageCover";
    public static final String DOC_TYPE_COVER = "docTypeCover";

    public static final String DOC_PURPOSE_PREFACE = "docPurposePreface";
    public static final String DOC_STAGE_PREFACE = "docStagePreface";
    public static final String DOC_TYPE_PREFACE = "docTypePreface";

    public static final String DOC_REF_COVER = "coverPage";

    public static final String PROPOSAL_DOC_COLLECTION = "docCollectionName";

    public static final String ANNEX_INDEX_META = "annexIndexMeta";
    public static final String ANNEX_NUMBER_META = "annexNumberMeta";
    public static final String ANNEX_TITLE_META = "annexTitleMeta";
    public static final String ANNEX_NUMBER_COVER = "annexNumberCover";
    public static final String ANNEX_TITLE_PREFACE = "annexTitlePreface";

    static {
        All_CONFIG_MAP.put(LeosCategory.PROPOSAL, createProposalConfig());
        All_CONFIG_MAP.put(LeosCategory.BILL, createBillConfig());
        All_CONFIG_MAP.put(LeosCategory.MEMORANDUM, createMemorandumConfig());
        All_CONFIG_MAP.put(LeosCategory.ANNEX, createAnnexConfig());
    }

    private static Map<String, XmlNodeConfig> createProposalConfig() {
        Map<String, XmlNodeConfig> proposalConfigMap = new HashMap<>();

        final Map<String, XmlNodeConfig> metadataConfig = new HashMap<>(4);
        metadataConfig.put(DOC_STAGE_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docStage", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__docstage", "leos:docStage"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        metadataConfig.put(DOC_TYPE_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docType", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__doctype", "leos:docType"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        metadataConfig.put(DOC_PURPOSE_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docPurpose", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__docpurpose", "leos:docPurpose"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        proposalConfigMap.putAll(metadataConfig);

        final Map<String, XmlNodeConfig> coverPageConfig = new HashMap<>(4);
        coverPageConfig.put(DOC_STAGE_COVER, new XmlNodeConfig("//coverPage/longTitle/p/docStage", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "cover_docstage", "docStage"))));
        coverPageConfig.put(DOC_TYPE_COVER, new XmlNodeConfig("//coverPage/longTitle/p/docType", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "cover_doctype", "docType"))));
        coverPageConfig.put(DOC_PURPOSE_COVER, new XmlNodeConfig("//coverPage/longTitle/p/docPurpose", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "cover_docpurpose", "docPurpose"))));
        proposalConfigMap.putAll(coverPageConfig);

        final Map<String, XmlNodeConfig> otherConfig = new HashMap<>(4);
        otherConfig.put(PROPOSAL_DOC_COLLECTION, new XmlNodeConfig("//documentCollection/@name", false, Collections.EMPTY_LIST));
        otherConfig.put(DOC_REF_COVER, new XmlNodeConfig("//coverPage/@GUID", false, Collections.EMPTY_LIST));
        proposalConfigMap.putAll(otherConfig);

        return proposalConfigMap;
    }

    private static Map<String, XmlNodeConfig> createBillConfig() {
        Map<String, XmlNodeConfig> billConfigMap = new HashMap<>();

        final Map<String, XmlNodeConfig> metadataConfig = new HashMap<>(4);
        metadataConfig.put(DOC_STAGE_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docStage", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__docstage", "leos:docStage"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        metadataConfig.put(DOC_TYPE_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docType", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__doctype", "leos:docType"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        metadataConfig.put(DOC_PURPOSE_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docPurpose", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__docpurpose", "leos:docPurpose"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        billConfigMap.putAll(metadataConfig);

        final Map<String, XmlNodeConfig> coverPageConfig = new HashMap<>(4);
        coverPageConfig.put(DOC_STAGE_COVER, new XmlNodeConfig("//coverPage/longTitle/p/docStage", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "cover_docstage", "docStage"))));
        coverPageConfig.put(DOC_TYPE_COVER, new XmlNodeConfig("//coverPage/longTitle/p/docType", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "cover_doctype", "docType"))));
        coverPageConfig.put(DOC_PURPOSE_COVER, new XmlNodeConfig("//coverPage/longTitle/p/docPurpose", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "cover_docpurpose", "docPurpose"))));
        billConfigMap.putAll(coverPageConfig);

        final Map<String, XmlNodeConfig> prefaceConfig = new HashMap<>(4);
        prefaceConfig.put(DOC_STAGE_PREFACE, new XmlNodeConfig("//preface/longTitle/p/docStage", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "preface_docStage", "docStage"))));
        prefaceConfig.put(DOC_TYPE_PREFACE, new XmlNodeConfig("//preface/longTitle/p/docType", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "preface_doctype", "docType"))));
        prefaceConfig.put(DOC_PURPOSE_PREFACE, new XmlNodeConfig("//preface/longTitle/p/docPurpose", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "preface_docpurpose", "docPurpose"))));
        billConfigMap.putAll(prefaceConfig);

        final Map<String, XmlNodeConfig> otherConfig = new HashMap<>(4);
        otherConfig.put(DOC_REF_COVER, new XmlNodeConfig("//coverPage/@GUID", false, Collections.EMPTY_LIST));
        billConfigMap.putAll(otherConfig);

        return billConfigMap;
    }

    private static Map<String, XmlNodeConfig> createMemorandumConfig() {
        Map<String, XmlNodeConfig> memorandumConfigMap = new HashMap<>();

        final Map<String, XmlNodeConfig> metadataConfig = new HashMap<>(4);
        metadataConfig.put(DOC_STAGE_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docStage", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__docstage", "leos:docStage"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        metadataConfig.put(DOC_TYPE_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docType", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__doctype", "leos:docType"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        metadataConfig.put(DOC_PURPOSE_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docPurpose", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__docpurpose", "leos:docPurpose"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        memorandumConfigMap.putAll(metadataConfig);

        final Map<String, XmlNodeConfig> coverPageConfig = new HashMap<>(4);
        coverPageConfig.put(DOC_STAGE_COVER, new XmlNodeConfig("//coverPage/longTitle/p/docStage", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "cover_docstage", "docStage"))));
        coverPageConfig.put(DOC_TYPE_COVER, new XmlNodeConfig("//coverPage/longTitle/p/docType", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "cover_doctype", "docType"))));
        coverPageConfig.put(DOC_PURPOSE_COVER, new XmlNodeConfig("//coverPage/longTitle/p/docPurpose", true,
                Arrays.asList(
                        new XmlNodeConfig.Attribute("GUID", "cover_docpurpose", "docPurpose"),
                        new XmlNodeConfig.Attribute("GUID", "em_coverpage__longTitle", "longTitle"),
                        new XmlNodeConfig.Attribute("refersTo", "#bill", "longTitle"))));
        memorandumConfigMap.putAll(coverPageConfig);

        final Map<String, XmlNodeConfig> otherConfig = new HashMap<>(4);
        otherConfig.put(DOC_REF_COVER, new XmlNodeConfig("//coverPage/@GUID", false, Collections.EMPTY_LIST));
        memorandumConfigMap.putAll(otherConfig);

        return memorandumConfigMap;
    }

    private static Map<String, XmlNodeConfig> createAnnexConfig() {
        Map<String, XmlNodeConfig> annexConfigMap = new HashMap<>();

        final Map<String, XmlNodeConfig> metadataConfig = new HashMap<>(8);
        metadataConfig.put(DOC_STAGE_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docStage", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__docstage", "leos:docStage"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        metadataConfig.put(DOC_TYPE_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docType", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__doctype", "leos:docType"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        metadataConfig.put(DOC_PURPOSE_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docPurpose", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__docpurpose", "leos:docPurpose"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));

        metadataConfig.put(ANNEX_INDEX_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:annexIndex", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__annexIndex", "leos:annexIndex"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        metadataConfig.put(ANNEX_NUMBER_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:annexNumber", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__annexNumber", "leos:annexNumber"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        metadataConfig.put(ANNEX_TITLE_META, new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:annexTitle", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "proprietary__annexTitle", "leos:annexTitle"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        annexConfigMap.putAll(metadataConfig);

        final Map<String, XmlNodeConfig> coverPageConfig = new HashMap<>(4);
        coverPageConfig.put(DOC_STAGE_COVER, new XmlNodeConfig("//coverPage/longTitle/p/docStage", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "cover_docstage", "docStage"))));
        coverPageConfig.put(DOC_TYPE_COVER, new XmlNodeConfig("//coverPage/longTitle/p/docType", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "cover_doctype", "docType"))));
        coverPageConfig.put(DOC_PURPOSE_COVER, new XmlNodeConfig("//coverPage/longTitle/p/docPurpose", true,
                Arrays.asList(
                        new XmlNodeConfig.Attribute("GUID", "cover_docpurpose", "docPurpose"),
                        new XmlNodeConfig.Attribute("GUID", "em_coverpage__longTitle", "longTitle"),
                        new XmlNodeConfig.Attribute("refersTo", "#bill", "longTitle"))));
        coverPageConfig.put(ANNEX_NUMBER_COVER, new XmlNodeConfig("//coverPage/container[@name='annexNumber']/p", false,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "cover_annexNumber", "container"))));
        annexConfigMap.putAll(coverPageConfig);

        final Map<String, XmlNodeConfig> prefaceConfig = new HashMap<>(4);
        prefaceConfig.put(ANNEX_TITLE_PREFACE, new XmlNodeConfig("//preface/longTitle/p/docTitle", true,
                Arrays.asList(new XmlNodeConfig.Attribute("GUID", "preface_doctitle", "docTitle"))));
        annexConfigMap.putAll(prefaceConfig);

        final Map<String, XmlNodeConfig> otherConfig = new HashMap<>(4);
        otherConfig.put(DOC_REF_COVER, new XmlNodeConfig("//coverPage/@GUID", false, Collections.EMPTY_LIST));
        annexConfigMap.putAll(otherConfig);

        return annexConfigMap;
    }

    public Map<String, XmlNodeConfig> getConfig(LeosCategory category) {
        Map<String, XmlNodeConfig> config = All_CONFIG_MAP.get(category);
        if (config == null) {
            throw new UnsupportedOperationException("There is no configuraiton present for category " + category);
        }
        return config;
    }

    public Map<String, XmlNodeConfig> getProposalComponentsConfig(LeosCategory leosCategory, String attributeName) {
        Validate.notNull(leosCategory);
        Validate.notNull(attributeName);

        Map<String, XmlNodeConfig> componentRefConfig = new HashMap<>();
        String showAs;
        String refersTo = leosCategory.name().toLowerCase();

        switch (leosCategory) {
            case BILL:
                showAs = "Bill";
                break;
            case MEMORANDUM:
                showAs = "Explanatory Memorandum";
                break;
            default:
                throw new IllegalArgumentException("Invalid configuration");
        }
        componentRefConfig.put(leosCategory.name() + "_" + attributeName,
                new XmlNodeConfig(String.format("//documentCollection/collectionBody/component[@refersTo='#%s']/documentRef/@%s", refersTo, attributeName), true,
                        Arrays.asList(new XmlNodeConfig.Attribute("showAs", showAs, "documentRef"))));
        return componentRefConfig;
    }

    public static Map<String, String> createValueMap(LeosMetadata.AnnexMetadata metadata) {
        Map<String, String> keyValueMap = new HashMap<>();
        keyValueMap.put(DOC_STAGE_META, metadata.getStage());
        keyValueMap.put(DOC_TYPE_META, metadata.getType());
        keyValueMap.put(DOC_PURPOSE_META, metadata.getPurpose());
        keyValueMap.put(ANNEX_INDEX_META, Integer.toString(metadata.getIndex()));
        keyValueMap.put(ANNEX_NUMBER_META, metadata.getNumber());
        keyValueMap.put(ANNEX_TITLE_META, metadata.getTitle());

        keyValueMap.put(DOC_STAGE_COVER, metadata.getStage());
        keyValueMap.put(DOC_TYPE_COVER, metadata.getType());
        keyValueMap.put(DOC_PURPOSE_COVER, metadata.getPurpose());

        keyValueMap.put(ANNEX_NUMBER_COVER, metadata.getNumber());

        keyValueMap.put(ANNEX_TITLE_PREFACE, metadata.getTitle());

        return keyValueMap;
    }

    public static Map<String, String> createValueMap(LeosMetadata.ProposalMetadata metadata) {
        Map<String, String> keyValueMap = new HashMap<>();
        keyValueMap.put(DOC_STAGE_META, metadata.getStage());
        keyValueMap.put(DOC_TYPE_META, metadata.getType());
        keyValueMap.put(DOC_PURPOSE_META, metadata.getPurpose());

        keyValueMap.put(DOC_STAGE_COVER, metadata.getStage());
        keyValueMap.put(DOC_TYPE_COVER, metadata.getType());
        keyValueMap.put(DOC_PURPOSE_COVER, metadata.getPurpose());

        return keyValueMap;
    }

    public static Map<String, String> createValueMap(LeosMetadata.BillMetadata metadata) {
        Map<String, String> keyValueMap = new HashMap<>();
        keyValueMap.put(DOC_STAGE_META, metadata.getStage());
        keyValueMap.put(DOC_TYPE_META, metadata.getType());
        keyValueMap.put(DOC_PURPOSE_META, metadata.getPurpose());

        keyValueMap.put(DOC_STAGE_COVER, metadata.getStage());
        keyValueMap.put(DOC_TYPE_COVER, metadata.getType());
        keyValueMap.put(DOC_PURPOSE_COVER, metadata.getPurpose());

        keyValueMap.put(DOC_STAGE_PREFACE, metadata.getStage());
        keyValueMap.put(DOC_TYPE_PREFACE, metadata.getType());
        keyValueMap.put(DOC_PURPOSE_PREFACE, metadata.getPurpose());

        return keyValueMap;
    }

    public static Map<String, String> createValueMap(LeosMetadata.MemorandumMetadata metadata) {
        Map<String, String> keyValueMap = new HashMap<>();
        keyValueMap.put(DOC_STAGE_META, metadata.getStage());
        keyValueMap.put(DOC_TYPE_META, metadata.getType());
        keyValueMap.put(DOC_PURPOSE_META, metadata.getPurpose());

        keyValueMap.put(DOC_STAGE_COVER, metadata.getStage());
        keyValueMap.put(DOC_TYPE_COVER, metadata.getType());
        keyValueMap.put(DOC_PURPOSE_COVER, metadata.getPurpose());

        return keyValueMap;
    }
}
