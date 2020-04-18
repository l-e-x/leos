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
import eu.europa.ec.leos.repository.LeosRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Configuration Repository implementation.
 *
 * @constructor Creates a specific Configuration Repository, injected with a generic LEOS Repository.
 */
@Repository
public class ConfigurationRepositoryImpl implements ConfigurationRepository {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationRepositoryImpl.class);

    private final LeosRepository leosRepository;

    @Autowired
    public ConfigurationRepositoryImpl(LeosRepository leosRepository) {
        this.leosRepository = leosRepository;
    }

    @Override
    public ConfigDocument findConfiguration(String path, String name) {
        logger.debug("Finding configuration... [path=" + path + ", name=" + name + "]");
        return leosRepository.findDocumentByParentPath(path, name, ConfigDocument.class);
    }

    @Override
    public XmlDocument findTemplate(String path, String name) {
        logger.debug("Finding template... [path=" + path + ", name=" + name + "]");
        return leosRepository.findDocumentByParentPath(path, name, XmlDocument.class);
    }
}
