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
package eu.europa.ec.leos.cmis.extensions

import eu.europa.ec.leos.cmis.mapping.CmisProperties.*
import eu.europa.ec.leos.domain.document.LeosMetadata.*
import io.atlassian.fugue.Option
import org.apache.chemistry.opencmis.client.api.Document
import java.math.BigInteger

val Document.proposalMetadataOption: Option<ProposalMetadata>               // FIXME add check for leos:proposal secondary type???
    get() {
        val stage = this.metadataStage
        val type = this.metadataType
        val purpose = this.metadataPurpose
        val docTemplate = this.metadataDocTemplate
        return if ((stage != null) && (type != null) && (purpose != null) && (docTemplate != null)) {
            Option.some(ProposalMetadata(stage, type, purpose, docTemplate))
        } else {
            Option.none()
        }
    }

val Document.memorandumMetadataOption: Option<MemorandumMetadata>           // FIXME add check for leos:memorandum secondary type???
    get() {
        val stage = this.metadataStage
        val type = this.metadataType
        val purpose = this.metadataPurpose
        val docTemplate = this.metadataDocTemplate
        return if ((stage != null) && (type != null) && (purpose != null) && (docTemplate != null)) {
            Option.some(MemorandumMetadata(stage, type, purpose, docTemplate))
        } else {
            Option.none()
        }
    }

val Document.billMetadataOption: Option<BillMetadata>                   // FIXME add check for leos:bill secondary type???
    get() {
        val stage = this.metadataStage
        val type = this.metadataType
        val purpose = this.metadataPurpose
        val docTemplate = this.metadataDocTemplate
        return if ((stage != null) && (type != null) && (purpose != null) && (docTemplate != null)) {
            Option.some(BillMetadata(stage, type, purpose, docTemplate))
        } else {
            Option.none()
        }
    }

val Document.annexMetadataOption: Option<AnnexMetadata>                     // FIXME add check for leos:annex secondary type???
    get() {
        val stage = this.metadataStage
        val type = this.metadataType
        val purpose = this.metadataPurpose
        val docTemplate = this.metadataDocTemplate
        val index = this.annexIndex
        val number = this.annexNumber
        val title = this.annexTitle ?: ""
        return if ((stage != null) && (type != null) && (purpose != null) && (docTemplate != null) &&
                   (index != null) && (number != null)) {
            Option.some(AnnexMetadata(stage, type, purpose, docTemplate, index, number, title))
        } else {
            Option.none()
        }
    }

private val Document.metadataStage: String?                              // FIXME make this property mandatory???
    get() = this.getPropertyValue<String>(METADATA_STAGE.id)

private val Document.metadataType: String?                               // FIXME make this property mandatory???
    get() = this.getPropertyValue<String>(METADATA_TYPE.id)

private val Document.metadataPurpose: String?                            // FIXME make this property mandatory???
    get() = this.getPropertyValue<String>(METADATA_PURPOSE.id)

private val Document.metadataDocTemplate: String?                            // FIXME make this property mandatory???
    get() = this.getPropertyValue<String>(METADATA_DOCTEMPLATE.id)

private val Document.annexIndex: Int?                                   // FIXME make this property mandatory???
    get() = this.getPropertyValue<BigInteger>(ANNEX_INDEX.id)?.intValueExact()

private val Document.annexNumber: String?                               // FIXME make this property mandatory???
    get() = this.getPropertyValue<String>(ANNEX_NUMBER.id)

private val Document.annexTitle: String?                                // FIXME make this property mandatory???
    get() = this.getPropertyValue<String>(ANNEX_TITLE.id)
