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
package eu.europa.ec.leos.services.coedition.handler;

import eu.europa.ec.leos.vo.coedition.CoEditionActionInfo;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;

import java.util.List;

public interface InfoHandler {

    /**
     * This method checks and store the info of the user as passed in input
     * @param coEditionVo this object contains the information about the user which is to be stored 
     * @return information about if operation was successful
     */
    CoEditionActionInfo storeInfo(CoEditionVO coEditionVo);
    /**
     * This method removes the info for the user passed in input
     * @param coEditionVo this object contains the information about the user which is to be removed
     * @return information about if operation was successful
     */
    CoEditionActionInfo removeInfo(CoEditionVO coEditionVo);

    /**
     * This method checks if the info with same presenter on same document and same element exists
     * @param coEditionVo this object contains the information about the info which is to be checked
     * @return information about if operation was successful
     */
    CoEditionActionInfo checkIfInfoExists(CoEditionVO coEditionVo);

    /**
     * This method checks if exists info for the session given and returns first info found
     * @param sessionId this object contains the session id about the info which is to be checked
     * @return information about if operation was successful
     */
    CoEditionActionInfo checkIfInfoExists(String sessionId);

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
