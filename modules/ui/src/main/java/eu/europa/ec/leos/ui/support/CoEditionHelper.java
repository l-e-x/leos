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
package eu.europa.ec.leos.ui.support;

import com.google.common.eventbus.EventBus;
import eu.europa.ec.leos.model.event.UpdateUserInfoEvent;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.coedition.CoEditionService;
import eu.europa.ec.leos.vo.coedition.CoEditionActionInfo;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.coedition.InfoType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CoEditionHelper {

    private static final Logger LOG = LoggerFactory.getLogger(CoEditionHelper.class);

    @Autowired
    private CoEditionService coEditionService;

    @Autowired
    private EventBus leosApplicationEventBus;

    public void storeUserEditInfo(String sessionId, String presenterId, User user, String documentId, String elementId, InfoType infoType) {
        LOG.debug("storeUserEditInfo Called in CoEditionHelper......");
        CoEditionActionInfo actionInfo = coEditionService.storeUserEditInfo(sessionId, presenterId, user, documentId, elementId, infoType);
        if (actionInfo.sucesss() && !CoEditionActionInfo.Operation.EXISTS.equals(actionInfo.getOperation())) {
            leosApplicationEventBus.post(new UpdateUserInfoEvent(actionInfo));
        }
    }

    public void removeUserEditInfo(String presenterId, String documentId, String elementId, InfoType infoType) {
        LOG.debug("removeUserEditInfo Called in CoEditionHelper......");
        CoEditionActionInfo actionInfo = coEditionService.removeUserEditInfo(presenterId, documentId, elementId, infoType);
        if (actionInfo.sucesss()) {
            leosApplicationEventBus.post(new UpdateUserInfoEvent(actionInfo));
        }
    }

    public void removeUserEditInfo(String sessionId) {
        LOG.debug("removeUserEditInfo for Session Called in CoEditionHelper......");
        CoEditionActionInfo actionInfo = null;
        do {
            actionInfo = coEditionService.removeUserEditInfo(sessionId);
            if (actionInfo.sucesss()) {
                leosApplicationEventBus.post(new UpdateUserInfoEvent(actionInfo));
            }
        } while (actionInfo != null && actionInfo.sucesss());
    }

    public List<CoEditionVO> getAllEditInfo() {
        return coEditionService.getAllEditInfo();
    }

    public List<CoEditionVO> getCurrentEditInfo(String docId) {
        return coEditionService.getCurrentEditInfo(docId);
    }

}
