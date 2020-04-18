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
package eu.europa.ec.leos.services.content.processor;


import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.security.SecurityContext;

import java.io.InputStream;

public interface DocumentContentService {

    default InputStream getContentInputStream(XmlDocument xmlDocument) {
        final Content content = xmlDocument.getContent().getOrError(() -> "Document content is required!");
        return content.getSource().getInputStream();

    }

    String toEditableContent(XmlDocument xmlDocument, String contextPath, SecurityContext securityContext);
}
