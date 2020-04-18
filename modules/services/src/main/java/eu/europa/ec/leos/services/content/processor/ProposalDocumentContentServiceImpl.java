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

import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.security.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Instance(instances = {InstanceType.COMMISSION, InstanceType.OS})
public class ProposalDocumentContentServiceImpl implements DocumentContentService {

    private TransformationService transformationService;

    @Autowired
    public ProposalDocumentContentServiceImpl(TransformationService transformationService) {
        this.transformationService = transformationService;
    }

    @Override
    public String toEditableContent(XmlDocument xmlDocument, String contextPath, SecurityContext securityContext) {
        return transformationService.toEditableXml(getContentInputStream(xmlDocument), contextPath, xmlDocument.getCategory(), securityContext.getPermissions(xmlDocument));
    }
}
