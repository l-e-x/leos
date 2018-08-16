/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.services.support.xml;

import com.ximpleware.VTDNav;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.services.support.xml.ref.Ref;

import java.util.List;

interface ReferenceLabelProcessor {
    /**
     * generates the multi references label thanks to the list of references (separated format(id,ref) for a single multireference) {@param refs}, the VTD navigator {@param vtdNav} 
     * using a specific language code {@param language}.
     * @param refs
     * @param vtdNav
     * @param language
     * @return: returns the label if mutil ref is valid or an error code if not.
     */
    Result<String> generateLabel(List<Ref> refs, String referenceLocation, VTDNav vtdNav, String language) throws Exception;
}
