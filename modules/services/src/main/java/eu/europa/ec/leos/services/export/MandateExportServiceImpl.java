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
import eu.europa.ec.leos.services.store.PackageService;
import io.atlassian.fugue.Pair;
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
    MandateExportServiceImpl(PackageService packageService, SecurityContext securityContext,
            ExportHelper exportHelper, DocuWriteService docuwriteService) {
        super(packageService, securityContext, exportHelper);
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
        try {
            Pair<File, ExportResource> legPackage = packageService.createLegPackage(documentId, exportOptions, versionToCompare.orElse(null));
            File legFile = createZipFile(legPackage, jobFileName, exportOptions);
            byte[] docuwriteResponse = docuwriteService.convert(legFile);
            File outputFile = File.createTempFile("Proposal_" + documentId + "_" + System.currentTimeMillis(), ".zip");
            fileOuputStream = new FileOutputStream(outputFile);
            fileOuputStream.write(docuwriteResponse);
            return outputFile;
        } catch(Exception e) {
            LOG.error("An exception occoured while using the Docuwrite service: ", e);
            throw e;
        } finally {
            if(fileOuputStream != null) {
                fileOuputStream.close();
            }
            LOG.trace("createDocuWritePackage() end....");
        }
        
    }
}
