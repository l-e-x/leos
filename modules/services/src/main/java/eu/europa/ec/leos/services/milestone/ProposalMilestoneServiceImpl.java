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
package eu.europa.ec.leos.services.milestone;

import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.document.LegDocument;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.services.export.ExportOptions;
import eu.europa.ec.leos.services.export.ExportService;
import eu.europa.ec.leos.services.export.LegPackage;
import eu.europa.ec.leos.services.store.LegService;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.io.IOException;

@Service
@Instance(InstanceType.COMMISSION)
public class ProposalMilestoneServiceImpl extends AbstractMilestoneService {

    protected ExportService exportService;

    @Autowired
    public ProposalMilestoneServiceImpl(ExportService exportService, LegService legService) {
        super(legService);
        this.exportService = exportService;
    }

    @Override
    @Nonnull
    public LegDocument createLegDocument(String proposalId, LegPackage legPackage) throws Exception {
        Validate.notNull(exportService, "Export Service is not available!!");
        Validate.notNull(legService, "LegService Service is not available!!");
        String jobId = exportService.exportLegPackage(proposalId, legPackage);
        return legService.createLegDocument(proposalId, jobId, legPackage, LeosLegStatus.IN_PREPARATION);
    }

    @Override
    protected LegPackage createLegPackage(String proposalId) throws IOException {
        return legService.createLegPackage(proposalId, ExportOptions.TO_WORD_MILESTONE_LW);
    }
}
