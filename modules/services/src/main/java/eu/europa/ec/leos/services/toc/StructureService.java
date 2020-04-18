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

import java.util.List;
import java.util.Map;

public interface StructureService {
    
    List<TocItem> getTocItems(String docTemplate);
    
    Map<TocItem, List<TocItem>> getTocRules(String docTemplate);
    
    List<NumberingConfig> getNumberingConfigs(String docTemplate);
    
    String getStructureName(String docTemplate);
    
    String getStructureVersion(String docTemplate);

    String getStructureDescription(String docTemplate);
    
}
