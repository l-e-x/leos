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
package eu.europa.ec.leos.services.store;

import eu.europa.ec.leos.domain.document.LeosDocument;
import eu.europa.ec.leos.domain.document.LeosPackage;
import eu.europa.ec.leos.integration.toolbox.ExportResource;
import io.atlassian.fugue.Pair;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface PackageService {

    LeosPackage createPackage();

    void deletePackage(LeosPackage leosPackage);

    LeosPackage findPackageByDocumentId(String documentId);

    // TODO consider using package id instead of path
    <T extends LeosDocument> List<T> findDocumentsByPackagePath(String path, Class<T> filterType, Boolean fetchContent);

    Pair<File, ExportResource> createLegPackage(String proposalId) throws IOException;
}
