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
package eu.europa.ec.leos.usecases.document;

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Memorandum;
import eu.europa.ec.leos.domain.document.LeosMetadata.MemorandumMetadata;
import eu.europa.ec.leos.domain.document.LeosPackage;
import eu.europa.ec.leos.services.document.MemorandumService;
import io.atlassian.fugue.Option;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class MemorandumContext {

    private static final Logger LOG = LoggerFactory.getLogger(MemorandumContext.class);

    private final MemorandumService memorandumService;

    private LeosPackage leosPackage = null;
    private Memorandum memorandum = null;
    private String purpose = null;

    MemorandumContext(MemorandumService memorandumService) {
        this.memorandumService = memorandumService;
    }

    public void usePackage(LeosPackage leosPackage) {
        Validate.notNull(leosPackage, "Memorandum package is required!");
        LOG.trace("Using Memorandum package... [id={}, path={}]", leosPackage.getId(), leosPackage.getPath());
        this.leosPackage = leosPackage;
    }

    public void useTemplate(Memorandum memorandum) {
        Validate.notNull(memorandum, "Memorandum template is required!");
        LOG.trace("Using Memorandum template... [id={}, name={}]", memorandum.getId(), memorandum.getName());
        this.memorandum = memorandum;
    }

    public void usePurpose(String purpose) {
        Validate.notNull(purpose, "Memorandum purpose is required!");
        LOG.trace("Using Memorandum purpose: {}", purpose);
        this.purpose = purpose;
    }

    public Memorandum executeCreateMemorandum() {
        LOG.trace("Executing 'Create Memorandum' use case...");
        Validate.notNull(leosPackage, "Memorandum package is required!");
        Validate.notNull(memorandum, "Memorandum template is required!");

        Option<MemorandumMetadata> metadataOption = memorandum.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Memorandum metadata is required!");

        Validate.notNull(purpose, "Memorandum purpose is required!");
        MemorandumMetadata metadata = metadataOption.get().withPurpose(purpose);

        return memorandumService.createMemorandum(memorandum.getId(), leosPackage.getPath(), metadata);
    }

    public void executeUpdateMemorandum() {
        LOG.trace("Executing 'Update Memorandum' use case...");

        Validate.notNull(leosPackage, "Memorandum package is required!");
        Memorandum memorandum = memorandumService.findMemorandumByPackagePath(leosPackage.getPath());

        Option<MemorandumMetadata> metadataOption = memorandum.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Memorandum metadata is required!");

        Validate.notNull(purpose, "Memorandum purpose is required!");
        MemorandumMetadata metadata = metadataOption.get().withPurpose(purpose);

        memorandumService.updateMemorandum(memorandum, metadata, false, "Metadata updated.");
    }
}
