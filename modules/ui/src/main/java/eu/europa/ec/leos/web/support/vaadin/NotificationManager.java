/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.web.support.vaadin;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.server.Page;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@UIScope
@Component
public class NotificationManager {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationManager.class);

    private static final String NOTIFICATION_STYLE = "dark";

    private MessageHelper messageHelper;
    private EventBus eventBus;

    @Autowired
    public NotificationManager(MessageHelper messageHelper, EventBus eventBus) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
    }

    @PostConstruct
    private void init(){
        eventBus.register(this);
    }

    @Subscribe
    public void showNotification(NotificationEvent notificationEvent) {
        // NOTE notification exceptions should not be propagated. We don't know how to handle them somewhere else.
        try {
            switch (notificationEvent.getType()) {
                case TRAY:
                    handleTrayNotification(notificationEvent, Type.TRAY_NOTIFICATION);
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
        } catch (Exception ex) {
            LOG.error("Unable to handle notification event!", ex);
        }
    }

    private void handleTrayNotification(NotificationEvent notificationEvent, Notification.Type notificationType) {
        String message = messageHelper.getMessage(notificationEvent.getMessageKey(), notificationEvent.getArgs());
        String caption = messageHelper.getMessage(notificationEvent.getCaptionKey());
        Notification notfn = new Notification(caption, message, notificationType, true);
        notfn.setStyleName(NOTIFICATION_STYLE);
        notfn.setDelayMsec(5000);
        notfn.show(Page.getCurrent());
    }

    private void handleNotification(NotificationEvent notificationEvent, Notification.Type notificationType) {
        String message = messageHelper.getMessage(notificationEvent.getMessageKey(), notificationEvent.getArgs());
        Notification notfn = new Notification(message, null, notificationType, true);
        notfn.setStyleName(NOTIFICATION_STYLE);
        notfn.show(Page.getCurrent());
    }

    private void handleErrorNotification(NotificationEvent notificationEvent, Notification.Type notificationType) {
        String message = messageHelper.getMessage(notificationEvent.getMessageKey(), notificationEvent.getArgs());
        Notification notfn = new Notification(message, null, notificationType, true);
        notfn.show(Page.getCurrent());
    }
}
