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
package eu.europa.ec.leos.annotate.repository;

import eu.europa.ec.leos.annotate.model.entity.Document;
import org.springframework.data.repository.CrudRepository;

/**
 * the repository for all {@link Document} objects hosting the documents being annotated
 */
public interface DocumentRepository extends CrudRepository<Document, Long> {

    /**
     * find a document given its URI
     * 
     * @param uri the URI of the document
     * @return found {@link Document} object, or null
     */
    Document findByUri(String uri);
}
