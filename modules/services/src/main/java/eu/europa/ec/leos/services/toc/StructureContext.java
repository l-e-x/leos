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
package eu.europa.ec.leos.services.toc;

import eu.europa.ec.leos.vo.toc.NumberingConfig;
import eu.europa.ec.leos.vo.toc.TocItem;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

@Component
@Scope(WebApplicationContext.SCOPE_REQUEST)
public class StructureContext {

    private static final Logger LOG = LoggerFactory.getLogger(StructureContext.class);

    private final StructureService structureService;
    private String docTemplate;

    StructureContext(StructureService structureService) {
        this.structureService = structureService;
    }

    public void useDocumentTemplate(String docTemplate) {
        LOG.trace("Using docTemplate... [docTemplate={}]", docTemplate);
        this.docTemplate = docTemplate;
    }

    public List<TocItem> getTocItems() {
        Validate.notNull(docTemplate, "Document template is required!");
        LOG.trace("Retrieving toc items configuration...");
        return structureService.getTocItems(docTemplate);
    }

    public Map<TocItem, List<TocItem>> getTocRules() {
        Validate.notNull(docTemplate, "Document template is required!");
        LOG.trace("Retrieving toc rules configuration...");
        return structureService.getTocRules(docTemplate);
    }

    public List<NumberingConfig> getNumberingConfigs() {
        Validate.notNull(docTemplate, "Document template is required!");
        LOG.trace("Retrieving numbering configuration...");
        return structureService.getNumberingConfigs(docTemplate);
    }
}
