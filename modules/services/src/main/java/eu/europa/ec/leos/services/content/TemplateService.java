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

import eu.europa.ec.leos.model.content.LeosFile;
import eu.europa.ec.leos.vo.catalog.CatalogItem;

public interface TemplateService {

    List<CatalogItem> getAllTemplates(InputStream xmlCatalog) throws IOException;

    /**
     * Get the document with the templates catalog.
     *
     * @return the retrieved document, including content.
     */
    LeosFile getTemplatesCatalog();

}