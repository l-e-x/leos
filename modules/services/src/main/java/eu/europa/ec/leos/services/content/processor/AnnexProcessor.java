/*
 * Copyright 2017 European Commission
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

public interface AnnexProcessor {

    /**
     * Inserts a new annex block before or after the current block with the given id. And saves the document
     * @param document The document to update
     * @param elementId  The id of the annex block before or after
     * @param before true if the new block needs to be inserted before the given block, false if it needs to be inserted after.
     * @return The updated document
     */
    byte[] insertAnnexBlock(byte[] content, String elementId, boolean before);
}
