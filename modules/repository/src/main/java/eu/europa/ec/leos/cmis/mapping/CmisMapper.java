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
package eu.europa.ec.leos.cmis.mapping;

import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.document.*;

import java.util.*;

import static eu.europa.ec.leos.domain.cmis.LeosCategory.*;
import static java.util.Collections.singleton;

public class CmisMapper {

    private static final Map<Class<? extends LeosDocument>, Set<LeosCategory>> documentCategoryMap;
    private static final Map<Class<? extends LeosDocument>, String> documentPrimaryTypeMap;

    static {
        documentCategoryMap = new HashMap<>();

        // FIXME move this mapping somewhere else or implement in better way?!!!
        documentCategoryMap.put(LeosDocument.class, EnumSet.of(PROPOSAL, MEMORANDUM, BILL, ANNEX, MEDIA, CONFIG, LEG));
        documentCategoryMap.put(XmlDocument.class, EnumSet.of(PROPOSAL, MEMORANDUM, BILL, ANNEX));
        documentCategoryMap.put(Proposal.class, singleton(PROPOSAL));
        documentCategoryMap.put(Memorandum.class, singleton(MEMORANDUM));
        documentCategoryMap.put(Bill.class, singleton(BILL));
        documentCategoryMap.put(Annex.class, singleton(ANNEX));
        documentCategoryMap.put(MediaDocument.class, singleton(MEDIA));
        documentCategoryMap.put(ConfigDocument.class, singleton(CONFIG));
        documentCategoryMap.put(LegDocument.class, singleton(LEG));

        // FIXME move this mapping somewhere else or implement in better way?!!!
        documentPrimaryTypeMap = new HashMap<>();
        documentPrimaryTypeMap.put(LeosDocument.class, "leos:document");
        documentPrimaryTypeMap.put(XmlDocument.class, "leos:xml");
        documentPrimaryTypeMap.put(Proposal.class, "leos:xml");
        documentPrimaryTypeMap.put(Memorandum.class, "leos:xml");
        documentPrimaryTypeMap.put(Bill.class, "leos:xml");
        documentPrimaryTypeMap.put(Annex.class, "leos:xml");
        documentPrimaryTypeMap.put(MediaDocument.class, "leos:media");
        documentPrimaryTypeMap.put(ConfigDocument.class, "leos:config");
        documentPrimaryTypeMap.put(LegDocument.class, "leos:leg");
    }

    // FIXME move this mapping somewhere else or implement in better way?!!!
    public static String cmisPrimaryType(Class<? extends LeosDocument> type) {
        String result = documentPrimaryTypeMap.get(type);
        if (result == null) {
            throw new IllegalArgumentException("Unknown CMIS primary type!");
        }
        return result;
    }

    // FIXME move this mapping somewhere else or implement in better way?!!!
    public static Set<LeosCategory> cmisCategories(Class<? extends LeosDocument> type) {
        return documentCategoryMap.getOrDefault(type, Collections.emptySet());
    }
}
