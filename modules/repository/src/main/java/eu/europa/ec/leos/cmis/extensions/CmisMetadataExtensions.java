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
import eu.europa.ec.leos.domain.cmis.metadata.*;
import io.atlassian.fugue.Option;
import org.apache.chemistry.opencmis.client.api.Document;

import java.math.BigInteger;
import java.util.function.Function;

class CmisMetadataExtensions {

    private static class CommonMetadataProperties {
        String stage, type, purpose, template, language, docTemplate, ref;
    }

    // FIXME add check for leos:proposal secondary type???
    static Option<ProposalMetadata> getProposalMetadataOption(Document document) {
        return buildMetadata(document, props -> Option.some(
                new ProposalMetadata(props.stage, props.type, props.purpose, props.template,
                        props.language, props.docTemplate, props.ref, null, "0.1.0")));
    }

    static Option<StructureMetaData> getStructureMetadataOption(Document document) {
        return buildMetadata(document, props -> Option.some(
                new StructureMetaData(props.stage, props.type, props.purpose, props.template,
                        props.language, props.docTemplate, props.ref, null, "0.1.0")));
    }

    // FIXME add check for leos:memorandum secondary type???
    static Option<MemorandumMetadata> getMemorandumMetadataOption(Document document) {
        return buildMetadata(document, props -> Option.some(
                new MemorandumMetadata(props.stage, props.type, props.purpose, props.template,
                        props.language, props.docTemplate, props.ref, null, "0.1.0")));
    }

    // FIXME add check for leos:bill secondary type???
    static Option<BillMetadata> getBillMetadataOption(Document document) {
        return buildMetadata(document, props -> Option.some(
                new BillMetadata(props.stage, props.type, props.purpose, props.template,
                        props.language, props.docTemplate, props.ref, null, "0.1.0")));
    }

    // FIXME add check for leos:annex secondary type???
    static Option<AnnexMetadata> getAnnexMetadataOption(Document document) {
        Integer index = getAnnexIndex(document);
        String number = getAnnexNumber(document);
        String title = getAnnexTitle(document);
        String annexTitle = title == null ? "" : title;

        return buildMetadata(document, props -> {
            if (index != null && number != null) {
                return Option.some(
                        new AnnexMetadata(props.stage, props.type, props.purpose, props.template,
                                props.language, props.docTemplate, props.ref, index, number, annexTitle, null, "0.1.0"));
            } else {
                return Option.none();
            }

        });
    }

    private static <T extends LeosMetadata> Option<T> buildMetadata(Document doc, Function<CommonMetadataProperties, Option<T>> leosMetadataBuilder) {
        CommonMetadataProperties props = new CommonMetadataProperties();
        props.stage = getMetadataStage(doc);
        props.type = getMetadataType(doc);
        props.purpose = getMetadataPurpose(doc);
        props.template = getTemplate(doc);
        props.language = getLanguage(doc);
        props.docTemplate = getMetadataDocTemplate(doc);
        props.ref = getMetadataRef(doc);

        Option<T> result;
        if (props.stage != null && props.type != null && props.purpose != null && props.template != null && props.language != null && props.docTemplate != null) {
            result = leosMetadataBuilder.apply(props);
        } else {
            result = Option.none();
        }
        return result;
    }

    // FIXME make this property mandatory???
    private static String getMetadataStage(Document document) {
        return (String) document.getPropertyValue(CmisProperties.METADATA_STAGE.getId());
    }

    // FIXME make this property mandatory???
    private static String getMetadataType(Document document) {
        return (String) document.getPropertyValue(CmisProperties.METADATA_TYPE.getId());
    }

    // FIXME make this property mandatory???
    private static String getMetadataPurpose(Document document) {
        return (String) document.getPropertyValue(CmisProperties.METADATA_PURPOSE.getId());
    }

    // FIXME make this property mandatory???
    private static String getMetadataDocTemplate(Document document) {
        return (String) document.getPropertyValue(CmisProperties.METADATA_DOCTEMPLATE.getId());
    }

    // FIXME make this property mandatory???
    private static Integer getAnnexIndex(Document document) {
        BigInteger value = document.getPropertyValue(CmisProperties.ANNEX_INDEX.getId());
        return value != null ? value.intValueExact() : null;
    }

    // FIXME make this property mandatory???
    private static String getAnnexNumber(Document document) {
        return (String) document.getPropertyValue(CmisProperties.ANNEX_NUMBER.getId());
    }

    // FIXME make this property mandatory???
    private static String getAnnexTitle(Document document) {
        return (String) document.getPropertyValue(CmisProperties.ANNEX_TITLE.getId());
    }

    // FIXME make this property mandatory???
    private static String getMetadataRef(Document document) {
        return (String) document.getPropertyValue(CmisProperties.METADATA_REF.getId());
    }

    private static String getTemplate(Document document) {
        return (String) document.getPropertyValue(CmisProperties.DOCUMENT_TEMPLATE.getId());
    }

    private static String getLanguage(Document document) {
        return (String) document.getPropertyValue(CmisProperties.DOCUMENT_LANGUAGE.getId());
    }
}
