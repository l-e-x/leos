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
package eu.europa.ec.leos.services.converter;

import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.services.store.TemplateService;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
@Instance(InstanceType.COUNCIL)
class ProposalConverterServiceForMandateImpl extends ProposalConverterServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(ProposalConverterServiceForMandateImpl.class);

    @Autowired
    ProposalConverterServiceForMandateImpl(
            XmlNodeProcessor xmlNodeProcessor,
            XmlNodeConfigHelper xmlNodeConfigHelper,
            XmlContentProcessor xmlContentProcessor,
            TemplateService templateService) {
        super(xmlNodeProcessor, xmlNodeConfigHelper, xmlContentProcessor, templateService);
    }

    @Override
    protected void updateSource(final DocumentVO document, File documentFile, boolean canModifySource) {
        try {
            byte[] xmlBytes = Files.readAllBytes(documentFile.toPath());
            if (canModifySource) {
                if (document.getCategory() == LeosCategory.BILL) {
                    xmlBytes = xmlContentProcessor.removeElements(xmlBytes, "//coverPage", 0);
                    // We have to remove the references to the annexes, we will add them when importing
                    xmlBytes = xmlContentProcessor.removeElements(xmlBytes, "//attachments", 0);
                }
                if (document.getCategory() == LeosCategory.ANNEX) {
                    xmlBytes = xmlContentProcessor.removeElements(xmlBytes, "//coverPage", 0);
                }
            }
            document.setSource(xmlBytes);
        } catch (IOException e) {
            LOG.error("Error updating the source of the document: {}", e);
            // the post validation will take care to analyse wether the source is there or not
            document.setSource(null);
        }
    }

}
