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
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import eu.europa.ec.leos.web.event.view.unauthorized.EnterUnauthorizedViewEvent;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.screen.LeosScreen;
import eu.europa.ec.leos.web.view.UnauthorizedView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import ru.xpoft.vaadin.VaadinView;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

@Scope("session")
@VaadinView(UnauthorizedView.VIEW_ID)
@org.springframework.stereotype.Component(UnauthorizedView.VIEW_ID)
public class UnauthorizedViewImpl extends LeosScreen implements UnauthorizedView {

    private static final long serialVersionUID = 4560654865831706296L;

    private static final Logger LOG = LoggerFactory.getLogger(UnauthorizedViewImpl.class);

    @Autowired
    private MessageHelper msgHelper;

    private Label infoLabel;

    @PostConstruct
    private void init() {
        LOG.trace("Initializing {} view...", VIEW_ID);

        // initialize view layout
        initLayout();
    }

    private void initLayout() {
        // create view layout
        addStyleName("leos-unauthorized-layout");

        // layout will use all available space
        setSizeFull();
        setMargin(true);

        // set view layout as composition root
        addStyleName("leos-unauthorized");

        // view will use all available space
        setSizeFull();

        // build unauthorized info
        infoLabel = new Label("", ContentMode.HTML);
        addComponent(infoLabel);
    }

    @Override
    public void buildUnauthorizedInfo(String userName) {
        final String info = msgHelper.getMessage("leos.ui.unauthorized.info", userName);
        infoLabel.setValue(info);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        LOG.debug("Entering {} view...", VIEW_ID);
        eventBus.post(new EnterUnauthorizedViewEvent());
    }

    @Override
    public @Nonnull
    String getViewId() {
        return VIEW_ID;
    }

}
