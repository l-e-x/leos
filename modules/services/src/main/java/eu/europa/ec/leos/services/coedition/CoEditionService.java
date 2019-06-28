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
package eu.europa.ec.leos.services.coedition;

import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.vo.coedition.CoEditionActionInfo;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.coedition.InfoType;

import java.util.List;

public interface CoEditionService {

    CoEditionActionInfo storeUserEditInfo(String sessionId, String presenterId, User user, String documentId, String elementId, InfoType infoType);

    CoEditionActionInfo removeUserEditInfo(String presenterId, String documentId, String elementId, InfoType infoType);

    CoEditionActionInfo removeUserEditInfo(String sessionId);

    List<CoEditionVO> getAllEditInfo();

    List<CoEditionVO> getCurrentEditInfo(String docId);
}
