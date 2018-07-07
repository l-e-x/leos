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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import eu.europa.ec.leos.model.content.LeosFile;
import eu.europa.ec.leos.repositories.content.ContentRepository;
import eu.europa.ec.leos.services.content.converter.DescriptionMapConverter;
import eu.europa.ec.leos.services.content.converter.LanguageMapConverter;
import eu.europa.ec.leos.services.content.converter.NameMapConverter;
import eu.europa.ec.leos.vo.catalog.Catalog;
import eu.europa.ec.leos.vo.catalog.CatalogItem;

@Component
public class TemplateServiceImpl implements TemplateService {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateServiceImpl.class);

    @Autowired
    private ContentRepository contentRepository;

    @Value("${leos.templates.path}")
    private String templatesPath;

    @Value("${leos.templates.catalog}")
    private String templatesCatalog;

    @Override
    public List<CatalogItem> getAllTemplates(InputStream xmlCatalog) throws IOException {
        Catalog catalog = loadCatalog(xmlCatalog);
        return catalog.getItems();
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
    public LeosFile getTemplatesCatalog() {
        LOG.trace("Getting templates catalog...");
        String catalogPath = templatesPath + "/" + templatesCatalog;
        LOG.trace("Templates catalog path = {}", catalogPath);
        return contentRepository.retrieveByPath(catalogPath, LeosFile.class);
    }

}
