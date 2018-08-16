/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.cmis.mapping

internal enum class CmisProperties(
        val id: String
) {
    DOCUMENT_CATEGORY("leos:category"),
    DOCUMENT_TITLE("leos:title"),               // FIXME maybe move title property to metadata or remove it entirely
    DOCUMENT_TEMPLATE("leos:template"),
    DOCUMENT_LANGUAGE("leos:language"),
    METADATA_REF("metadata:ref"),
    METADATA_STAGE("metadata:docStage"),
    METADATA_TYPE("metadata:docType"),
    METADATA_PURPOSE("metadata:docPurpose"),
    METADATA_DOCTEMPLATE("metadata:docTemplate"),
    ANNEX_INDEX("annex:docIndex"),
    ANNEX_NUMBER("annex:docNumber"),
    ANNEX_TITLE("annex:docTitle"),
    COLLABORATORS("leos:collaborators")
}
