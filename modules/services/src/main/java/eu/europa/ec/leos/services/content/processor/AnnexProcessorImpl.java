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

import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlHelper;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class AnnexProcessorImpl implements AnnexProcessor {

    @Autowired
    private XmlContentProcessor xmlContentProcessor;

    private String getAnnexTemplate() {
        return XmlHelper.getAnnexTemplate();
    }

    @Override
    public byte[] insertAnnexBlock(byte[] content, String elementId, String tagName, boolean before) {
        Validate.notNull(content, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");

        String annexTemplate = getAnnexTemplate();

        byte[] updatedXmlContent = xmlContentProcessor.insertElementByTagNameAndId(content, annexTemplate,
                tagName,
                elementId, before);

        return updatedXmlContent;
    }


}
