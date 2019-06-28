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
package eu.europa.ec.leos.services.store;

import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.document.LegDocument;
import eu.europa.ec.leos.domain.cmis.document.LeosDocument;
import eu.europa.ec.leos.domain.cmis.document.Proposal;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.vo.LegDocumentVO;
import eu.europa.ec.leos.services.export.ExportOptions;
import eu.europa.ec.leos.services.export.ExportResource;
import io.atlassian.fugue.Pair;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface PackageService {

    String NOT_AVAILABLE = "Not available";

    LeosPackage createPackage();

    void deletePackage(LeosPackage leosPackage);

    LeosPackage findPackageByDocumentId(String documentId);

    // TODO consider using package id instead of path
    <T extends LeosDocument> List<T> findDocumentsByPackagePath(String path, Class<T> filterType, Boolean fetchContent);
    
    <T extends LeosDocument> T findDocumentByPackagePathAndName(String path, String name, Class<T> filterType);

    <T extends LeosDocument> List<T> findDocumentsByPackageId(String id, Class<T> filterType, Boolean allVersions, Boolean fetchContent);
    
    Pair<File, ExportResource> createLegPackage(String proposalId, ExportOptions exportOptions) throws IOException;
    
    Pair<File, ExportResource> createLegPackage(String proposalId, ExportOptions exportOptions, XmlDocument intermediateMajor) throws IOException;

    Pair<File,ExportResource> createLegPackage(File legFile, ExportOptions exportOptions) throws IOException;

    <T extends Proposal> List<T> findDocumentsByUserId(String userId, Class<T> filterType, String leosAuthority);

    List<LegDocumentVO> getLegDocumentDetailsByUserId(String userId);

    LegDocument createLegDocument(String proposalId, String jobId, String milestoneComment, File file, LeosLegStatus status) throws IOException;

    LegDocument updateLegDocument(String id, LeosLegStatus status);

    LegDocument updateLegDocument(String id, byte[] pdfJobZip, byte[] wordJobZip);

    LegDocument findLegDocumentById(String id);

    /**
     * Finds the Leg document that has jobId and is in the same package with any document that has @documentId.
     * @param documentId the id of a document that is located in the same package as the Leg file
     * @param jobId the jobId of the Leg document
     * @return the Leg document if found, otherwise null
     */
    LegDocument findLegDocumentByAnyDocumentIdAndJobId(String documentId, String jobId);

    List<LegDocument>  findLegDocumentByStatus(LeosLegStatus leosLegStatus);
}
