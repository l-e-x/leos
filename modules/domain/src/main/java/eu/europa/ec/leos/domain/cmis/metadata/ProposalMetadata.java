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
package eu.europa.ec.leos.domain.cmis.metadata;

import eu.europa.ec.leos.domain.cmis.LeosCategory;

public final class ProposalMetadata extends LeosMetadata {

    public ProposalMetadata(String stage, String type, String purpose, String template, String language, String docTemplate, String ref, String objectId, String docVersion) {
        super(LeosCategory.PROPOSAL, stage, type, purpose, template, language, docTemplate, ref, objectId, docVersion);
    }

    public final ProposalMetadata withPurpose(String p) {
        return new ProposalMetadata(stage, type, p, template, language, docTemplate, ref, objectId, docVersion);
    }

    public final ProposalMetadata withRef(String ref) {
        return new ProposalMetadata(stage, type, purpose, template, language, docTemplate, ref, objectId, docVersion);
    }

    public final ProposalMetadata withObjectId(String objectId) {
        return new ProposalMetadata(stage, type, purpose, template, language, docTemplate, ref, objectId, docVersion);
    }

    public final ProposalMetadata withDocVersion(String docVersion) {
        return new ProposalMetadata(stage, type, purpose, template, language, docTemplate, ref, objectId, docVersion);
    }
}
