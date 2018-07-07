/**
 * Copyright 2015 European Commission
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
package eu.europa.ec.leos.repositories.content;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.net.MediaType;

import eu.europa.ec.leos.model.content.LeosFile;
import eu.europa.ec.leos.model.content.LeosFileProperties;
import eu.europa.ec.leos.model.content.LeosObject;
import eu.europa.ec.leos.model.content.LeosObjectProperties;

public interface ContentRepository {

    /**
     * Browse the contents of the folder with the given path.
     *
     * @param path the path of the folder to browse.
     * @return a list of object properties, folders or documents (excluding content).
     */
    @Nonnull 
    List<LeosObjectProperties> browse(@Nonnull String folderPath);

    /**
     * Retrieve the latest document with the given leosId.
     *
     * @param leosId the leosId of the document to retrieve.
     * @return the retrieved LeosObject, including content.
     */
    @Nonnull
    <T extends LeosObject> T retrieveById(@Nonnull String leosId, Class<T> type);

    /**
     * Retrieve the document with the given path.
     *
     * @param path the path of the document to retrieve.
     * @return the retrieved LeosObject, including content.
     */
    @Nonnull
    <T extends LeosObject> T retrieveByPath(@Nonnull String path, Class<T> type);

    /**
     * Retrieve the document with the given version id (verisonId != leosId).
     * @param leosId the leosId of the LeosFile to retrieve.
     * @param versionId the revision id/object Id of the document to retrieve.
     * @paran type Is the type of the leosFile/Document Expected
     * @return the retrieved LeosFile, including content.
     */
    @Nonnull
    <T extends LeosFile> T retrieveVersion(@Nonnull String versionId, Class<T> type);
    
    /**
     * Update the content of the document with the given id.
     *
     * @param leosId the leosId of the LeosFile to update.
     * @param streamName the name of the new content. If <code>null</code>, the LeosFile name will be used.
     * @param streamLength the length, in bytes, of the new content. If <code>null</code>, the value <code>-1</code> will be used.
     * @param streamMimeType the MimeType of the new content. If <code>null</code>, the value <code>application/octet-stream</code> will be used.
     * @param inputStream the new content stream for the document.
     * @param checkinComment checkinComment about the update
     * @return the updated document.
     */
    @Nonnull
    <T extends LeosFile> T updateContent(@Nonnull String leosId, @Nullable String streamName,
            @Nullable Long streamLength, @Nullable MediaType streamMimeType,
            @Nonnull InputStream inputStream, String checkinComment, Class<T> type);

    /**
     * Create a leosFile from another leosFile with the given path, including content.
     *
     * @param sourceFilePath the path of the source @{LeosFile}.
     * @param targetFileName the name of the new document.
     * @param targetFolderPath the path of the folder where to create the document.
     * @param checkinComment comment to be updated in repository
     * @return the created document.
     */
    @Nonnull
    <T extends LeosFile> T copy(@Nonnull String sourceFilePath,
            @Nonnull String targetFileName,
            @Nonnull String targetFolderPath, String checkinComment);

    
    /** rename the LeosObject with give leosId
     * @param leosId
     * @param name
     * @param checkinComment comment to be updated in repository
     * @return
     */
    <T extends LeosObject> T rename(String leosId, String name, String checkinComment);

    /**
     * Create a leosFile with the given path, including properties and content.
     *
     * @param folderPath the path of the new LeosFile.
     * @param documentName the name of the new LeosFile.
     * @param streamMimeType the MimeType of the content of the new LeosFile.
     * @param inputStream the content stream of the new document.
     * @param streamLength the length, in bytes, of the content of the new LeosFile.
     * @param type LeosDocument/LeosFile
     * @return the created LeosFile.
     */
    <T extends LeosFile> T create(String folderPath, String filetName, MediaType streamMimeType, InputStream inputStream, Long streamLength, Class<T> type);

    /**
     * Delete a object by leosId
     * 
     * @param leosId of the leos object to delete
     * @return void
     */
    void delete(String leosId);
    
    /**
     * get available versions of a leosFile by leosId
     * 
     * @param leosId of the leosFile 
     * @return list of versions
     */
    
    <T extends LeosFileProperties> List<T> getVersions(String leosId);

    /**
     * @param leosId
     * @param properties
     * @return
     */
    <T extends LeosObject> T updateProperties(String leosId, Map<String, Object> properties);

    
}
