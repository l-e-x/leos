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
package eu.europa.ec.leos.services.store;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.document.ConfigDocument;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.repository.store.ConfigurationRepository;
import eu.europa.ec.leos.services.support.converter.DescriptionMapConverter;
import eu.europa.ec.leos.services.support.converter.LanguageMapConverter;
import eu.europa.ec.leos.services.support.converter.NameMapConverter;
import eu.europa.ec.leos.vo.catalog.Catalog;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import io.atlassian.fugue.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Service
class TemplateServiceImpl implements TemplateService {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateServiceImpl.class);

    private final ConfigurationRepository configRepository;

    @Value("${leos.templates.path}")
    private String templatesPath;

    @Value("${leos.templates.catalog}")
    private String templatesCatalog;

    TemplateServiceImpl(ConfigurationRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public List<CatalogItem> getTemplatesCatalog() throws IOException {
        LOG.trace("Getting templates catalog... [path={}, name={}]", templatesPath, templatesCatalog);
        ConfigDocument catalogDocument = configRepository.findConfiguration(templatesPath, templatesCatalog);
        Option<Content> optContent = catalogDocument.getContent();
        if (optContent.isDefined()) {
            // FIXME handle option functionally?
            LOG.trace("Parsing templates catalog XML content...");
            Catalog catalog = loadCatalog(optContent.get().getSource().getInputStream());
            // FIXME handle null catalog!!!
            return catalog.getItems();
        } else {
            LOG.trace("Templates catalog XML content is empty!");
            return Collections.emptyList();
        }
    }

    private Catalog loadCatalog(InputStream xmlCatalog) throws IOException {

        // configure XStream
        XStream xstream = new XStream(new StaxDriver());
        xstream.alias("catalog", Catalog.class);
        xstream.useAttributeFor(Catalog.class, "defaultLanguage");
        xstream.aliasAttribute(Catalog.class, "defaultLanguage", "lang");
        xstream.addImplicitCollection(Catalog.class, "itemList", CatalogItem.class);
        xstream.alias("item", CatalogItem.class);
        xstream.useAttributeFor(CatalogItem.class, "type");
        xstream.useAttributeFor(CatalogItem.class, "id");
        xstream.useAttributeFor(CatalogItem.class, "enabled");
        xstream.aliasField("names", CatalogItem.class, "nameMap");
        xstream.registerLocalConverter(CatalogItem.class, "nameMap", new NameMapConverter());
        xstream.aliasField("descriptions", CatalogItem.class, "descMap");
        xstream.registerLocalConverter(CatalogItem.class, "descMap", new DescriptionMapConverter());
        xstream.aliasField("languages", CatalogItem.class, "langMap");
        xstream.registerLocalConverter(CatalogItem.class, "langMap", new LanguageMapConverter());
        xstream.addImplicitCollection(CatalogItem.class, "itemList", CatalogItem.class);

        // parse XML
        Catalog catalog = (Catalog) xstream.fromXML(xmlCatalog);

        if (catalog == null) {
            LOG.warn("Unable to load catalog!");
        }

        return catalog;
    }

    @Override
    public XmlDocument getTemplate(String name) {
        LOG.trace("Getting template... [path={}, name={}]", templatesPath, name);
        return configRepository.findTemplate(templatesPath, name);
    }

    @Override
    public String getTemplateName(List<CatalogItem> catalogItems, String name, String language) {
        String templateName = "";
        for (CatalogItem item : catalogItems) {
            if (item.getId().contains(name)) {
                return item.getName(language);
            } else {
                templateName = getTemplateName(item.getItems(), name, language);
                if (!templateName.isEmpty()) {
                    return templateName;
                }
            }
        }
        return templateName;
    }

    @Override
    public CatalogItem getTemplateItem(String name) throws IOException {
        return getTemplateItem(getTemplatesCatalog(), name);
    }

    private CatalogItem getTemplateItem(List<CatalogItem> catalogItems, String name) {
        CatalogItem templateItem = null;
        for (CatalogItem item : catalogItems) {
            if (item.getId().contains(name)) {
                return item;
            } else {
                templateItem = getTemplateItem(item.getItems(), name);
                if (templateItem != null) {
                    return templateItem;
                }
            }
        }
        return templateItem;
    }
}
