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

import com.google.common.eventbus.Subscribe;
import com.vaadin.server.Page;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

public class NotificationManager {
    public static final String NOTFICATION_STYLE = "dark";

    private MessageHelper messageHelper;

    public NotificationManager(MessageHelper messageHelper) {
        this.messageHelper = messageHelper;
    }

    @Subscribe
    public void showNotification(NotificationEvent notificationEvent) {
        switch (notificationEvent.getType()) {
            case TRAY:
                handleTrayNotification(notificationEvent, Type.TRAY_NOTIFICATION);
                break;
            case DISCLAIMER:
                handleDisclaimer(notificationEvent, Type.HUMANIZED_MESSAGE);
                break;
            case INFO:
                handleNotification(notificationEvent, Type.HUMANIZED_MESSAGE);
                break;
            case WARNING:
                handleNotification(notificationEvent, Type.WARNING_MESSAGE);
                break;
            case ERROR:
                handleErrorNotification(notificationEvent, Type.ERROR_MESSAGE);
                break;
            default:
                handleNotification(notificationEvent, Type.ERROR_MESSAGE);
                break;
        }
    }

    private void handleTrayNotification(NotificationEvent notificationEvent, Notification.Type notificationType) {
        String message = messageHelper.getMessage(notificationEvent.getMessageKey(), notificationEvent.getArgs());
        String caption = messageHelper.getMessage(notificationEvent.getCaptionKey());
        Notification notfn = new Notification(caption, message, notificationType);
        notfn.setStyleName(NOTFICATION_STYLE);
        notfn.setDelayMsec(3000);
        notfn.show(Page.getCurrent());
    }

    private void handleDisclaimer(NotificationEvent notificationEvent, Notification.Type notificationType) {
        String message = messageHelper.getMessage(notificationEvent.getMessageKey(), notificationEvent.getArgs());
        String caption = messageHelper.getMessage(notificationEvent.getCaptionKey());
        Notification disclaimer = new Notification(caption, message, notificationType, true);
        disclaimer.setDelayMsec(Notification.DELAY_FOREVER);
        disclaimer.setStyleName("leos-disclaimer");
        disclaimer.show(Page.getCurrent());
    }

    private void handleNotification(NotificationEvent notificationEvent, Notification.Type notificationType) {
        String message = messageHelper.getMessage(notificationEvent.getMessageKey(), notificationEvent.getArgs());
        Notification notfn = new Notification(message, notificationType);
        notfn.setStyleName(NOTFICATION_STYLE);
        notfn.show(Page.getCurrent());
    }

    private void handleErrorNotification(NotificationEvent notificationEvent, Notification.Type notificationType) {
        String message = messageHelper.getMessage(notificationEvent.getMessageKey(), notificationEvent.getArgs());
        Notification notfn = new Notification(message, notificationType);
        notfn.show(Page.getCurrent());
    }
}
