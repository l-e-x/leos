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
package eu.europa.ec.leos.annotate.services.impl;

import eu.europa.ec.leos.annotate.model.entity.Document;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.repository.MetadataRepository;
import eu.europa.ec.leos.annotate.services.MetadataService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateMetadataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetadataServiceImpl implements MetadataService {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataServiceImpl.class);

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private MetadataRepository metadataRepos;

    // -------------------------------------
    // Service functionality
    // -------------------------------------
    
    /**
     * find a {@link Metadata} object given its linked {@link Document}, {@link Group} and system ID
     * 
     * @param document the linked document
     * @param group    the associate group
     * @param systemId the system ID via which the metadata was given
     * 
     * @return found Metadata object
     */
    @Override
    public Metadata findByDocumentAndGroupAndSystemId(Document document, Group group, String systemId) {

        return metadataRepos.findByDocumentAndGroupAndSystemId(document, group, systemId);
    }

    /**
     * save a given {@link Metadata} set
     * 
     * @param metadata the metadata set to be saved
     * 
     * @return saved metadata, with IDs filled in
     * @throws CannotCreateMetadataException 
     */
    @Override
    public Metadata saveMetadata(Metadata metadata) throws CannotCreateMetadataException {

        if(metadata == null) {
            LOG.error("Received metadata for saving is null!");
            throw new CannotCreateMetadataException(new IllegalArgumentException("Metadata is null"));
        }
        
        try {
            metadata = metadataRepos.save(metadata);
        } catch (Exception e) {
            LOG.error("Exception upon saving metadata");
            throw new CannotCreateMetadataException(e);
        }

        return metadata;
    }

}
