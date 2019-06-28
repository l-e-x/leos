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
package eu.europa.ec.leos.services.coedition.repository;

import eu.europa.ec.leos.vo.coedition.CoEditionVO;

import java.util.List;

public interface EditionInfoRepository {
    /**This method stores the edit info in repository.
     * @param edit info
     * @return added edit info if the info is successfully added to repository else null
     */
    CoEditionVO store(CoEditionVO vo );

    /**This method removes the info for a user from repository.
     * @param edit info
     * @return removed edit info object if the info is successfully removed from repository else null
     */
    CoEditionVO removeInfo(CoEditionVO vo);
    
    /**this method returns existing edit info on given document
     * @param docId
     * @return unmodifiable list Of edit info
     */
    List<CoEditionVO> getCurrentEditInfo(String docId);

    /**this method returns existing edit info in the repository on all existing documents and elements
     * @return unmodifiable list Of all info
     */
    List<CoEditionVO> getAllEditInfo();
}
