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
package eu.europa.ec.leos.web.ui.screen;

import com.google.common.eventbus.EventBus;
import com.vaadin.navigator.View;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.web.event.view.LeaveViewEvent;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Nonnull;

public abstract class LeosScreen extends VerticalLayout implements View {

    private static final long serialVersionUID = 7129459269528968931L;
    private static final Logger LOG = LoggerFactory.getLogger(LeosScreen.class);

    private static final String VIEW_KEY_PREFIX = "leos.view";

    @Autowired
    protected EventBus eventBus;

    @Autowired
    protected SecurityContext securityContext;

    @Autowired
    protected MessageHelper messageHelper;

    @Autowired
    protected LanguageHelper langHelper;

    public @Nonnull
    abstract String getViewId();

    @Value("${leos.pollingInterval}")
    protected int pollingInterval;
    
    public @Nonnull String getViewKey() {
        return VIEW_KEY_PREFIX + "." + getViewId();
    }

    @Override
    public void detach() {
        LOG.debug("Detaching {} view...", getViewId());
        LeaveViewEvent leaveViewEvent = new LeaveViewEvent(getViewId());
        try {
            eventBus.post(leaveViewEvent);
        } catch (BeanCreationException e) {
            //the eventBus is not going to be available when this cod is called on session expired
            LOG.warn("Could not post the event {} on the eventBus.", leaveViewEvent);
        }
        super.detach();
    }
    
    protected void setPollingStatus(boolean enabled) {
        int millis = enabled ? pollingInterval : -1;
        LOG.trace("View Id :{}, Setting Leos Screen polling interval: {} millis", getViewId(), millis);
        UI.getCurrent().setPollInterval(millis);
    }
}
