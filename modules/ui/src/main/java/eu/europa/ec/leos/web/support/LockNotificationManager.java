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
package eu.europa.ec.leos.web.support;

import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

public class LockNotificationManager {
    public static void notifyUser(MessageHelper messageHelper, LockActionInfo lockActionInfo) {
        LockData updatedLock = lockActionInfo.getLock();
        NotificationEvent notificationEvent = null;
        switch (lockActionInfo.getLock().getLockLevel()) {
            case READ_LOCK:
                notificationEvent = new NotificationEvent(NotificationEvent.Type.TRAY,
                        "document.locked.read." + lockActionInfo.getOperation().getValue(),
                        updatedLock.getUserName(), updatedLock.getUserLoginName());
                break;
            case DOCUMENT_LOCK:
                notificationEvent = new NotificationEvent(NotificationEvent.Type.TRAY,
                        "document.locked." + lockActionInfo.getOperation().getValue(),
                        updatedLock.getUserName(), updatedLock.getUserLoginName());
                break;
            case ELEMENT_LOCK:
                notificationEvent = new NotificationEvent(NotificationEvent.Type.TRAY,
                       "document.locked.article." + lockActionInfo.getOperation().getValue(),
                        updatedLock.getUserName(), updatedLock.getUserLoginName(), updatedLock.getElementId());
                break;
        }
        notificationEvent.setCaptionKey("document.message.tray.heading");
        // TODO : making direct call as eventbus is not available
        new NotificationManager(messageHelper).showNotification(notificationEvent);

    }
}
