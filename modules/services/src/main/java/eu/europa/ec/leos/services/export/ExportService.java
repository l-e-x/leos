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

import java.io.File;
import java.util.Optional;

public interface ExportService {

    String exportToToolboxCoDe(String documentId, ExportOptions exportOptions) throws Exception;

    byte[] exportToToolboxCoDe(File legFile, ExportOptions exportOptions) throws Exception;

    String exportLegPackage(String proposalId, LegPackage legPackage) throws Exception;

    File createCollectionPackage(String jobFileName, String documentId, ExportOptions exportOptions) throws Exception;

    File createDocuWritePackage(String jobFileName, String documentId, ExportOptions exportOptions, Optional<XmlDocument> versionToCompare) throws Exception;

    File exportToPdf(String proposalId) throws Exception;

}
