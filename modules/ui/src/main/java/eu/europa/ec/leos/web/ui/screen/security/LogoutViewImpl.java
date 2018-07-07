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
package eu.europa.ec.leos.web.ui.screen.security;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Label;
import eu.europa.ec.leos.web.event.view.logout.EnterLogoutViewEvent;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.screen.LeosScreen;
import eu.europa.ec.leos.web.view.LogoutView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import ru.xpoft.vaadin.VaadinView;

import javax.annotation.PostConstruct;

@Scope("session")
@VaadinView(LogoutView.VIEW_ID)
@org.springframework.stereotype.Component(LogoutView.VIEW_ID)
public class LogoutViewImpl extends LeosScreen implements LogoutView {

    private static final long serialVersionUID = 1719600562771101552L;
    private static final Logger LOG = LoggerFactory.getLogger(LogoutViewImpl.class);

    @Autowired
    private MessageHelper messageHelper;

    @PostConstruct
    protected void init() {
        // view will use all available space
        addStyleName("leos-ui-layout");
        setMargin(true);
        setSizeFull();

        addComponent(new Label(messageHelper.getMessage("leos.logout.message")));
    }

    @Override
    public void enter(ViewChangeEvent event) {
        LOG.debug("Entering {} view...", VIEW_ID);
        eventBus.post(new EnterLogoutViewEvent());
    }

    @Override
    public String getViewId() {
        return VIEW_ID;
    }

}
