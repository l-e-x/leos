/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.services.content;

import java.util.List;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.vo.MetaDataVO;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.UserVO;

public interface DocumentService {

    /**
     * Get the document with the given leosId.
     * @param leosId the leosId of the document to retrieve.
     * @return the retrieved document, including content
     */
    LeosDocument getDocument(String leosId);

    /**
     * Get the exact document version with the given id.
     *
     * @param leosId the leosId of the document to retrieve. (Can pass null) 
     * @param versionId the exact versionId of the document to retrieve.
     * @return the retrieved document, including content
     */
    LeosDocument getDocument(String leosId, String versionId);

    /**
     * Update the content of the document with the given id.
     *
     * @param leosId the leosId of the document to update.
     * @param userLogin 
     * @param content the new content for the document.
     * @param checkinComment if any about the update
     * @return the updated document or throws a LeosDocumentNotLockedByCurrentUserException if the given user doesn't have a lock on the document
     */
    LeosDocument updateDocumentContent(String leosId, String userLogin, byte[] content , String checkinComment);

    /**
     * Create a document from the template with the given id.
     *
     * @param templateId the id of the source template.
     * @param metaDataVO the new documents meta data.
     * @return the created document.
     */
    LeosDocument createDocumentFromTemplate(String templateId, MetaDataVO metaDataVO);

    /**
     * Updates and saves the document with the new meta data
     * @param document The document to update
     * @param userLogin      * 
     * @param metaDataVO The new meta data
     * @return  the new document with the new meta data or throws a LeosDocumentNotLockedByCurrentUserException if the given user doesn't have a lock on the document
     */
    LeosDocument updateMetaData(LeosDocument document, String userLogin, MetaDataVO metaDataVO);

    /**
     * Get the meta data from the given document
     * @param document
     * @return The meta data
     */
    MetaDataVO getMetaData(LeosDocument document);

    /**
     * Get the table of content information from the document
     * @param document
     * @return the table of content
     */
    List<TableOfContentItemVO> getTableOfContent(LeosDocument document);

    /**
     * Get the ancestors ids for given element id
     * 
     * @param elementId
     * @return the ancestors ids.
     */
    List<String> getAncestorsIdsForElementId(LeosDocument document,
            String elementId);

    /**
     * Update the document with the new toc list
     * @param document 
     * @param userLogin      * 
     * @param tocList the list of table of content items
     * @return the updated document or throws a LeosDocumentNotLockedByCurrentUserException if the given user doesn't have a lock on the document
     */
    LeosDocument saveTableOfContent(LeosDocument document, String userLogin, List<TableOfContentItemVO> tocList);

    /**
     * @param leosId
     * @return
    */
    List<LeosDocumentProperties> getDocumentVersions(String leosId);

    /**
     * delete the document with the given leosId.
     * @param leosId the leosId of the document to delete.
     */
    void deleteDocument(String leosId);
    /**
     * @param leosId
     * @param List<UserVO> contributors
     * @return void
     */
    void setContributors (String leosId, List<UserVO> contributors) ;
    /**
     * Update Stage of the document
     *
     * @param document leosId
     * @return updated document
    */
     LeosDocument updateStage(String leosId, LeosDocumentProperties.Stage newStage);

}