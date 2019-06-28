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
import eu.europa.ec.leos.integration.ToolBoxService;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.store.PackageService;
import io.atlassian.fugue.Pair;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

abstract class ExportServiceImpl implements ExportService {

    protected final PackageService packageService;
    protected ToolBoxService toolBoxService;
    protected final SecurityContext securityContext;
    protected final ExportHelper exportHelper;

    @Autowired
    ExportServiceImpl(PackageService packageService, SecurityContext securityContext, ExportHelper exportHelper) {
        this.packageService = packageService;
        this.securityContext = securityContext;
        this.exportHelper = exportHelper;
    }

    @Override
    public File createDocuWritePackage(String jobFileName, String documentId, ExportOptions exportOptions, Optional<XmlDocument> versionToCompare)
            throws Exception {
        return null;
    }

    @Override
    public String exportToToolboxCoDe(String documentId, ExportOptions exportOptions) throws Exception {
        return null;
    }

    @Override
    public byte[] exportToToolboxCoDe(File legFile, ExportOptions exportOptions) throws Exception {
        return null;
    }

    @Override
    public String exportLegPackage(String proposalId, Pair<File, ExportResource> legPackage) throws Exception {
        return null;
    }

    @Override
    public File createCollectionPackage(String jobFileName, String documentId, ExportOptions exportOptions) throws Exception {
        Validate.notNull(jobFileName);
        Validate.notNull(exportOptions);
        Validate.notNull(documentId);
        File legFile = null;
        try {
            Pair<File, ExportResource> legPackage = packageService.createLegPackage(documentId, exportOptions);
            legFile = legPackage.left();
            return createZipFile(legPackage, jobFileName, exportOptions);
        } finally {
            if (legFile != null && legFile.exists()) {
                legFile.delete();
            }
        }
    }

    protected File createZipFile(Pair<File, ExportResource> legPackage, String jobFileName, ExportOptions exportOptions) throws Exception{
        Validate.notNull(legPackage);
        Validate.notNull(jobFileName);
        Validate.notNull(exportOptions);
        try (ByteArrayOutputStream contentFileContent = exportHelper.createContentFile(exportOptions, legPackage.right())) {
            Map<String, Object> contentToZip = new HashMap<>();
            contentToZip.put("content.xml", contentFileContent);
            contentToZip.put(legPackage.left().getName(), legPackage.left());
            return ZipPackageUtil.zipFiles(jobFileName, contentToZip);
        }
    }
}
