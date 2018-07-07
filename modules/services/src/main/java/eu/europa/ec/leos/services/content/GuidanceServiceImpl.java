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
package eu.europa.ec.leos.services.content;

import eu.europa.ec.leos.domain.document.Content;
import eu.europa.ec.leos.domain.document.LeosDocument.ConfigDocument;
import eu.europa.ec.leos.repository.store.ConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
class GuidanceServiceImpl implements GuidanceService {

    private static final Logger LOG = LoggerFactory.getLogger(GuidanceServiceImpl.class);

    private final ConfigurationRepository configurationRepository;

    //Templates and guidance are related to so they are kept in same folder
    @Value("${leos.templates.path}")
    private String templatesPath;

    private static final Map<String, String> templateGuidanceMap = new HashMap<>();
    static {
        //FIXME find alternative way to store mapping
        templateGuidanceMap.put("EM", "EM-Guidance.json");
        templateGuidanceMap.put("BL", "BL-Guidance.json");
    }

    @Autowired
    GuidanceServiceImpl(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    @Override
    public String getGuidance(String templateId) {
        LOG.trace("Getting guidance... [templateId={}]", templateId);
        String guidance = null;

        String guidanceFile = templateGuidanceMap.get(templateId.substring(0,2));

        ConfigDocument guidanceConfig =
                configurationRepository.findConfiguration(templatesPath, guidanceFile);

        if (guidanceConfig.getContent().isDefined()) {
            Content content = guidanceConfig.getContent().get();
            guidance = content.getSource().getByteString().utf8();
            LOG.debug("Retrieved guidance length {} for template {}", content.getLength(), templateId);
        }

        return guidance;
    }
}
