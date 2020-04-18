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
package eu.europa.ec.leos.repository.store;

import eu.europa.ec.leos.domain.cmis.document.ConfigDocument;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;

/**
 * Configuration Repository interface.
 * <p>
 * Represents collections of *Configuration* documents, with specific methods to persist and retrieve.
 * Allows CRUD operations based on strongly typed Business Entities: [ConfigDocument] and [XmlDocument].
 */
public interface ConfigurationRepository {

    /**
     * Finds a [ConfigDocument] document with the specified characteristics.
     *
     * @param path the path where to find the document
     * @param name the name of the document to retrieve.
     * @return the found configuration document.
     */
    ConfigDocument findConfiguration(String path, String name);

    /**
     * Finds a [XmlDocument] document with the specified characteristics.
     *
     * @param path the path where to find the document
     * @param name the name of the document to retrieve.
     * @return the found configuration document.
     */
    XmlDocument findTemplate(String path, String name);
}
