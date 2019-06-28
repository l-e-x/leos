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

import java.util.List;
import eu.europa.ec.leos.domain.common.Result;

public interface ReferenceLabelService {
    /**
     * Generates the multi references label based on:
     * - element id being edited {@param referenceLocation}
     * - list of references, element ids chosen by the user {@param refs}
     * - byte content being edited {@param xmlBytes}
     *
     *
     * @param refs element ids chosen by the user
     * @param xmlBytes byte content being edited
     * @param referenceLocation element id being edited
     * @return: returns the label if multi ref is valid or an error code otherwise.
     */
    Result<String> generateLabel(List<String> refs, String referenceLocation, byte[] xmlBytes) throws Exception;
}
