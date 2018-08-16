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

import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
class AttachmentProcessorImpl implements AttachmentProcessor {
    private XmlContentProcessor xmlContentProcessor;

    AttachmentProcessorImpl(XmlContentProcessor xmlContentProcessor) {
        this.xmlContentProcessor = xmlContentProcessor;
    }

    @Override
    public byte[] addAttachmentInBill(byte[] xmlContent, String href, String showAs) {
        Validate.notNull(xmlContent, "Xml content is required.");
        Validate.notNull(href, "href is required.");
        byte[] updatedContent = xmlContent;

        String attachments = xmlContentProcessor.getElementByNameAndId(xmlContent, "attachments", null);
        if (attachments == null) {
            String attachmentsTag = new StringBuilder("<attachments>")
                    .append(createAttachmentTag(href, showAs))
                    .append("</attachments>").toString();
            updatedContent = xmlContentProcessor.appendElementToTag(xmlContent, "bill", attachmentsTag);
        } else {
            updatedContent = xmlContentProcessor.appendElementToTag(xmlContent, "attachments", createAttachmentTag(href, showAs));
        }
        return xmlContentProcessor.doXMLPostProcessing(updatedContent);
    }

    @Override
    public byte[] removeAttachmentFromBill(byte[] xmlContent, String href) {
        String attachments = xmlContentProcessor.getElementByNameAndId(xmlContent, "attachments", null);
        byte[] updatedContent = xmlContent;

        if (attachments == null) {
            return updatedContent;
        }

        updatedContent = xmlContentProcessor.removeElements(xmlContent, String.format("//attachments/attachment/documentRef[@href=\"%s\"]", href), 1);
        String attachment = xmlContentProcessor.getElementByNameAndId(updatedContent, "attachment", null);
        if (attachment == null) { // if no other attachment tag is present, remove it
            updatedContent = xmlContentProcessor.removeElements(updatedContent, "//attachments", 0);
        }
        return updatedContent;
    }

    @Override
    public Map<String, String> getAttachmentsIdFromBill(byte[] xmlContent) {
        Map<String, String> attachmentsId = new HashMap();
        List<Map<String, String>> attrsElts = xmlContentProcessor.getElementsAttributesByPath(xmlContent, "//attachments/attachment/documentRef");
        attrsElts.forEach(element -> {
            if (element.containsKey("GUID") && element.containsKey("href")) {
                attachmentsId.put(element.get("href"), element.get("GUID"));
            }
        });
        return attachmentsId;
    }

    private String createAttachmentTag(String href, String showAs) {
        return String.format("<attachment><documentRef href=\"%s\" showAs=\"%s\"/></attachment>", href, showAs);
    }

    private String createDocumentRefTag(String guid, String href, String showAs) {
        return String.format("<documentRef GUID=\"%s\" href=\"%s\" showAs=\"%s\"/>", guid, href, showAs);
    }

    @Override
    public byte[] updateAttachmentsInBill(byte[] xmlContent, HashMap<String, String> attachmentsElements) {

        for (String elementRef : attachmentsElements.keySet()) {
            String elementId = xmlContentProcessor.getElementIdByPath(xmlContent,
                    String.format("//attachments/attachment/documentRef[@href=\"%s\"]", elementRef));
            String updatedElement = createDocumentRefTag(elementId, elementRef, attachmentsElements.get(elementRef));
            xmlContent = xmlContentProcessor.replaceElementByTagNameAndId(xmlContent, updatedElement, "documentRef", elementId);
        }

        return xmlContent;
    }
}
