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
package eu.europa.ec.leos.services.content.processor;

import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.security.LeosPermission;

import java.io.InputStream;
import java.util.List;

public interface TransformationService {

    String toEditableXml(InputStream documentStream, String contextPath, LeosCategory category, List<LeosPermission> permissions);

    String toXmlFragmentWrapper(InputStream documentStream, String contextPath, List<LeosPermission> permissions);
    
    String toImportXml(InputStream documentStream, String contextPath, List<LeosPermission> permissions);

    /**
     * This methods gets the document from repository, converts it in html format
     *
     * @param versionDocument version document to be returned in html format
     * @param contextPath the base path to be used while creating HTML for resources
     * @return document String in html format
     */
    String formatToHtml(XmlDocument versionDocument, String contextPath, List<LeosPermission> permissions);
}
