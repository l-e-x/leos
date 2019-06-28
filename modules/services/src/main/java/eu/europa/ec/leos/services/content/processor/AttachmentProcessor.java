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

import java.util.HashMap;
import java.util.Map;

public interface AttachmentProcessor {

    /** This method adds a new "attachment/documentRef" section to 'attachments' tag if present else it would create an attachment tag.
     * @param xmlContent
     * @param href
     * @return udpated xml content
     */
    byte[] addAttachmentInBill(byte[] xmlContent, String href, String showAs);

    /** This method removes "attachment/documentRef" section to attachments tag if present.
     *  if there are no more attachments, it removes attachments tag also to keep xml valid.
     * @param xmlContent
     * @param href
     * @return udpated xml content
     */
    byte[] removeAttachmentFromBill(byte[] xmlContent, String href);

    /** This method gets "attachment/documentRef" section from attachments tag if present.
     * @param xmlContent
     * @return documentRef xml:id values with href as keys
     */
    Map<String, String> getAttachmentsIdFromBill(byte[] xmlContent);

    /** This method update existing elements "attachment/documentRef" section to 'attachments' tag.
     * @param xmlContent
     * @param attachments
     * @return udpated xml content
     */
    byte[] updateAttachmentsInBill(byte[] xmlContent, HashMap<String, String> attachments);
}