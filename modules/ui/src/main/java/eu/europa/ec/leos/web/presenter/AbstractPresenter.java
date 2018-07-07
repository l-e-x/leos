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
package eu.europa.ec.leos.web.presenter;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.view.LeaveViewEvent;
import eu.europa.ec.leos.web.view.LeosView;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpSession;

/**
 *
 */
public abstract class AbstractPresenter<T extends LeosView> {

    @Autowired
    protected EventBus eventBus;

    @Autowired
    protected HttpSession session;

    @Autowired
    protected SecurityContext leosSecurityContext;

    public abstract T getView();

    /**
     * Rejects the display of the current view and forwards to another target view.
     * In addition, the method accepts an explanatory message with arguments to format this message.
     *
     * @param targetView the view to which the call will be forwarded
     * @param messageKey the explanatory message key
     * @param args       the arguments to the message key if any
     */
    protected void rejectView(String targetView, String messageKey, Object... args) {
        eventBus.post(new NavigationRequestEvent(targetView));
        eventBus.post(new NotificationEvent(NotificationEvent.Type.WARNING, messageKey, args));
    }

    @Subscribe
    public void leaveView(LeaveViewEvent event) {
        if (getView().getViewId().equals(event.getViewId())) {
            onViewLeave();
        }
    }

    public void onViewLeave() {
    }

}
