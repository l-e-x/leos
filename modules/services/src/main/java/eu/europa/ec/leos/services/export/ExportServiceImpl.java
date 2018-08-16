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
package eu.europa.ec.leos.services.export;

import eu.europa.ec.leos.integration.ToolBoxService;
import eu.europa.ec.leos.integration.toolbox.ExportResource;
import eu.europa.ec.leos.integration.utils.zip.ZipPackageUtil;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.store.PackageService;
import io.atlassian.fugue.Pair;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.ws.WebServiceException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
class ExportServiceImpl implements ExportService {
    private static final Logger LOG = LoggerFactory.getLogger(ExportServiceImpl.class);

    private final PackageService packageService;
    private final ToolBoxService toolBoxService;
    private final SecurityContext securityContext;
    private final ExportHelper exportHelper;

    @Autowired
    ExportServiceImpl(PackageService packageService, ToolBoxService toolBoxService,
            SecurityContext securityContext, ExportHelper exportHelper) {
        this.packageService = packageService;
        this.toolBoxService = toolBoxService;
        this.securityContext = securityContext;
        this.exportHelper = exportHelper;
    }

    @Override
    public String exportToToolboxCoDe(String proposalId, ExportOptions exportOptions) throws Exception {
        File legisWritePackage = null;
        String jobId;
        try {
            legisWritePackage = createLegisWritePackage("job.zip", proposalId, exportOptions);
            String destinationEmail = securityContext.getUser().getEmail();
            jobId = toolBoxService.createJob(legisWritePackage, "AkomaNtoso2LegisWrite", destinationEmail);
        } catch(WebServiceException wse) {
            LOG.error("Webservice error occurred in method exportToToolboxCoDe(): {}", wse.getMessage());
            throw wse;
        } catch (Exception ex) {
            LOG.error("Unexpected error occurred in method exportToToolboxCoDe(): {}", ex.getMessage());
            throw ex;
        }
        finally {
            if (legisWritePackage != null && legisWritePackage.exists()) {
                legisWritePackage.delete();
            }
        }
        return jobId;
    }

    @Override
    public File createProposalLegisWritePackage(String jobFileName, String proposalId, ExportOptions exportOptions) throws Exception {
        return createLegisWritePackage(jobFileName, proposalId, exportOptions);
    }

    private File createLegisWritePackage(String jobFileName, String documentId, ExportOptions exportOptions) throws Exception {
        Validate.notNull(jobFileName);
        Validate.notNull(exportOptions);
        Validate.notNull(documentId);

        Map<String, Object> contentToZip = new HashMap<String, Object>();

        File legFile = null;
        ByteArrayOutputStream contentFileContent = null;

        try {
            Pair<File, ExportResource> resultLegPackage = packageService.createLegPackage(documentId);
            legFile = resultLegPackage.left();
            ExportResource exportRootNode = resultLegPackage.right();

            contentFileContent = exportHelper.createContentFile(exportOptions, exportRootNode);
            contentToZip.put("content.xml", contentFileContent);

            contentToZip.put(legFile.getName(), legFile);

            return ZipPackageUtil.zipFiles(jobFileName, contentToZip);
        }
        finally {
            if (contentFileContent != null) {
                contentFileContent.close();
            }
            if (legFile != null && legFile.exists()) {
                legFile.delete();
            }
        }
    }
}
