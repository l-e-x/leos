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
import eu.europa.ec.leos.services.coedition.handler.InfoHandler;
import eu.europa.ec.leos.vo.coedition.CoEditionActionInfo;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.coedition.InfoType;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.StampedLock;

@Service
public class CoEditionServiceImpl implements CoEditionService {

    @Autowired
    private InfoHandler infoHandler;

    private final StampedLock infoHandlerLock = new StampedLock();

    @Override
    public CoEditionActionInfo storeUserEditInfo(String sessionId, String presenterId, User user, String documentId, String elementId, InfoType infoType) {
        Validate.notNull(sessionId, "sessionId must not be null");
        Validate.notNull(presenterId, "presenterId must not be null");
        Validate.notNull(user, "user must not be null");
        Validate.notNull(documentId, "documentId must not be null");

        CoEditionVO coEditionVo = new CoEditionVO(sessionId, presenterId, user.getLogin(), user.getName(), user.getDefaultEntity() != null ? user.getDefaultEntity().getOrganizationName() : "", user.getEmail(), documentId,
                elementId, infoType, System.currentTimeMillis());
        long stamp = infoHandlerLock.writeLock();
        try {
            CoEditionActionInfo actionInfo = infoHandler.checkIfInfoExists(coEditionVo);
            if (!actionInfo.sucesss()) {
                actionInfo = infoHandler.storeInfo(coEditionVo);
            }
            return actionInfo;
        } finally {
            infoHandlerLock.unlockWrite(stamp);
        }
    }

    @Override
    public CoEditionActionInfo removeUserEditInfo(String presenterId, String documentId, String elementId, InfoType infoType) {
        Validate.notNull(presenterId, "presenterId must not be null");
        Validate.notNull(documentId, "documentId must not be null");

        CoEditionVO coEditionVo = new CoEditionVO(null, presenterId, null, null, null, null, documentId, elementId, infoType, null);
        long stamp = infoHandlerLock.writeLock();
        try {
            CoEditionActionInfo actionInfo = infoHandler.checkIfInfoExists(coEditionVo);
            if (actionInfo.sucesss()) {
                actionInfo = infoHandler.removeInfo(actionInfo.getInfo());
            }
            return actionInfo;
        } finally {
            infoHandlerLock.unlockWrite(stamp);
        }
    }

    @Override
    public CoEditionActionInfo removeUserEditInfo(String sessionId) {
        Validate.notNull(sessionId, "sessionId must not be null");

        long stamp = infoHandlerLock.writeLock();
        try {
            CoEditionActionInfo actionInfo = infoHandler.checkIfInfoExists(sessionId);
            if (actionInfo.sucesss()) {
                actionInfo = infoHandler.removeInfo(actionInfo.getInfo());
            }
            return actionInfo;
        } finally {
            infoHandlerLock.unlockWrite(stamp);
        }
    }

    @Override
    public List<CoEditionVO> getAllEditInfo() {
        long stamp = infoHandlerLock.readLock();
        try {
            return infoHandler.getAllEditInfo();
        } finally {
            infoHandlerLock.unlockRead(stamp);
        }
    }

    @Override
    public List<CoEditionVO> getCurrentEditInfo(String docId) {
        Validate.notNull(docId, "The document id must not be null!");
        long stamp = infoHandlerLock.readLock();
        try {
            return infoHandler.getCurrentEditInfo(docId);
        } finally {
            infoHandlerLock.unlockRead(stamp);
        }
    }
}
