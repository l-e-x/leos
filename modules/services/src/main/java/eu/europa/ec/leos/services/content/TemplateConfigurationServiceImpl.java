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
package eu.europa.ec.leos.services.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.document.ConfigDocument;
import eu.europa.ec.leos.repository.store.ConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class TemplateConfigurationServiceImpl implements TemplateConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateConfigurationServiceImpl.class);

    private final ConfigurationRepository configurationRepository;

    // Templates and template configuration are related to so they are kept in same folder
    @Value("${leos.templates.path}")
    private String templatesPath;

    @Autowired
    TemplateConfigurationServiceImpl(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    @Override
    public String getTemplateConfiguration(String templateId, String confElement) {
        if (getConfElementJson(templateId, confElement) != null) {
            return getConfElementJson(templateId, confElement).toString();
        } else {
            return null;
        }
    }

    @Override
    public JsonNode getTemplateConfigurationJson(String templateId, String confElement) {
        return getConfElementJson(templateId, confElement);
    }

    private JsonNode getConfElementJson(String templateId, String confElement) {
        LOG.trace("Getting template configuration... [templateId={}] , [confElement={}]", templateId, confElement);
        String conf;
        String confFile = templateId + "-CONF";

        try {
            ConfigDocument confDocument = configurationRepository.findConfiguration(templatesPath, confFile);

            if (confDocument.getContent().isDefined()) {
                Content content = confDocument.getContent().get();
                conf = content.getSource().toString();
                ObjectMapper mapper = new ObjectMapper();

                JsonNode rootNode = mapper.readTree(conf);
                JsonNode templateConfJson = rootNode.get(confElement);
                if (templateConfJson == null) {
                    throw new IllegalArgumentException("Element '" + confElement + "' not present in the '" + templateId + "-CONF'");
                }
                LOG.debug("Retrieved template configuration length {} for template {}", content.getLength(), templateId);
                return templateConfJson;
            }
        } catch (Exception exception) {
            if (exception instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) exception;
            }
            LOG.error("Error occurred while fetching the conf for templateId: {}, confElement: {}, Error: {} ", templateId, confElement, exception.getMessage());
            throw new IllegalArgumentException("Error occurred while fetching the conf for templateId: " + templateId + ", confElement: " + confElement + ", Error: " + exception.getMessage());
        }
        return null;
    }

}