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

import javax.annotation.Nonnull;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.vo.CommentVO;

public interface CommentService {

    /**
     * Retrieves all the comments from the given document
     * @param document The document containing comments
     * @return the xml string representation of the article
     */
    List<CommentVO> getAllComments(LeosDocument document);

    /**
     * Inserts a new comment before end or after start tag of the element with the given id. And saves the document
     * @param document The document to update
     * @param elementId  The id of the element inside which comment to be updated
     * @param commentId  The id of commentId to be updated
     * @param commentContent text of the content
     * @param start true if the new comment needs to be inserted in start, false if it needs to be inserted after.
     * @return The updated document or throws a LeosDocumentNotLockedByCurrentUserException if the given user doesn't have a lock on the document
     */
    LeosDocument insertNewComment(LeosDocument document, @Nonnull String elementId, @Nonnull String commentId, String commentContent, boolean start);

    /**
     * Saves the new comment content of an existing comment to the given document or deletes the comment if the given comment content is null
     * @param document The document to update
     * @param commentId  The id of commentId to be updated
     * @param newContent updated text of the content
     * * @return The updated document or throws a LeosDocumentNotLockedByCurrentUserException if the given user doesn't have a lock on the document
     */
    LeosDocument updateComment(LeosDocument document, @Nonnull String commentId, String newContent);

    /**
     * Deletes an existing comment from a document 
     * @param document The document to update
     * @param commentId The id of the comment which is to be deleted.
     * @return The updated document or throws a LeosDocumentNotLockedByCurrentUserException if the given user doesn't have a lock on the document
     */
    LeosDocument deleteComment(LeosDocument document, @Nonnull String commentId);
    /**
     * This method takes all the comments from addended XML and injects/updated them in base XML
     * @param baseXml   xml in which comments will be updated
     * @param addendXml xml from which comments will be taken and injected into base xml
     * @return aggregated XML =baseXML + comments from addendXML
     * @throws Exception in case of any error
     */
    public byte[] aggregateComments(byte[] baseXml, byte[] addendXml) throws Exception;

}