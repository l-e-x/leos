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
package eu.europa.ec.leos.services.export;

import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.integration.DocuWriteService;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.services.document.AnnexService;
import eu.europa.ec.leos.services.document.BillService;
import eu.europa.ec.leos.services.store.LegService;
import eu.europa.ec.leos.services.store.PackageService;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Optional;

@Service
@Instance(InstanceType.COUNCIL)
public class MandateExportServiceImpl extends ExportServiceImpl {
    private static final Logger LOG = LoggerFactory.getLogger(MandateExportServiceImpl.class);

    protected final DocuWriteService docuwriteService;
    
    @Autowired
    MandateExportServiceImpl(LegService legService, PackageService packageService, SecurityContext securityContext,
                             ExportHelper exportHelper, DocuWriteService docuwriteService,
                             BillService billService, AnnexService annexService,
                             TransformationService transformationService) {
        super(legService, packageService, securityContext, exportHelper, billService, annexService, transformationService);
        this.docuwriteService = docuwriteService;
    }
    
    @Override
    public File createDocuWritePackage(String jobFileName, String documentId, ExportOptions exportOptions,
            Optional<XmlDocument> versionToCompare) throws Exception {
        LOG.trace("calling createDocuWritePackage()....");
        Validate.notNull(jobFileName);
        Validate.notNull(exportOptions);
        Validate.notNull(documentId);
        FileOutputStream fileOuputStream = null;
        LegPackage legPackage = null;
        try {
            legPackage = legService.createLegPackage(documentId, exportOptions, versionToCompare.orElse(null));
            File zipFile = createZipFile(legPackage, jobFileName, exportOptions);
            byte[] docuwriteResponse = docuwriteService.convert(zipFile);
            File outputFile = File.createTempFile("Proposal_" + documentId + "_AKN2DW_" + System.currentTimeMillis(), ".zip");
            fileOuputStream = new FileOutputStream(outputFile);
            fileOuputStream.write(docuwriteResponse);
            return outputFile;
        } catch(Exception e) {
            LOG.error("An exception occurred while using the Docuwrite service: ", e);
            throw e;
        } finally {
            if(fileOuputStream != null) {
                fileOuputStream.close();
            }
            // removal of the leg file only, the zip file will be kept in temp folder in case the converter fails and we need manual run.
            if(legPackage.getFile() != null && legPackage.getFile() .exists())
            {
                legPackage.getFile().delete();
            }
            LOG.trace("createDocuWritePackage() end....");
        }
        
    }
}
