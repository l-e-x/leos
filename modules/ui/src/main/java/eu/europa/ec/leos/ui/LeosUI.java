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
package eu.europa.ec.leos.ui;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.EnableVaadinNavigation;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.vaadin.LeosVaadinErrorHandler;
import eu.europa.ec.leos.web.ui.component.Banner;
import eu.europa.ec.leos.web.ui.component.layout.Footer;
import eu.europa.ec.leos.web.ui.component.layout.HeaderComponent;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;

/**
 * LEOS web application main entry point.
 */
@SpringUI
@EnableVaadinNavigation
@PreserveOnRefresh
@DependsOn({"navigationManager", "stateManager", "notificationManager", "deadEventHandler"})
@Theme(LeosTheme.NAME)
public class LeosUI extends UI {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LeosUI.class);

    @Autowired
    private ConfigurationHelper cgfHelper;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private SecurityContext securityContext;

    @Autowired
    private MessageHelper messageHelper;

    @Autowired
    private LanguageHelper langHelper;

    @Autowired
    private LeosViewDisplay viewDisplay ;

    @Autowired
    private LeosVaadinErrorHandler leosErrorHandler;

    @Autowired
    private HeaderComponent header;

    @Value("${leos.pollingInterval}")
    protected int pollInterval;

    @Override
    protected void init(VaadinRequest request) {
        LOG.trace("Leos UI initializing...");

        // initialize UI layout
        initLayout();

        // initialize error handler
        initErrorHandler();

        setPollInterval();

        LOG.trace("Leos UI is initialized!");
    }

    private void initLayout() {
        LOG.trace("Initializing layout...");

        // ui will use all available space
        addStyleName("leos-ui");
        setSizeFull();

        // ui layout will use all available space
        final VerticalLayout leosLayout = new VerticalLayout();
        leosLayout.setMargin(false);
        leosLayout.setSpacing(false);
        leosLayout.addStyleName("leos-ui-layout");
        leosLayout.setSizeFull();
        setContent(leosLayout);

        final Banner banner = new Banner(messageHelper);
        leosLayout.addComponent(banner);

        leosLayout.addComponent(header);

        leosLayout.addComponent(viewDisplay);

        final Footer footer = new Footer(messageHelper);
        leosLayout.addComponent(footer);

        // expand body to use all available space
        leosLayout.setExpandRatio(viewDisplay, 1.0f);
    }

    // Error handler method to customize vaadin error messages
    private void initErrorHandler() {
        // Override the DefaultErrorHandler
        setErrorHandler(leosErrorHandler);
    }

    private void setPollInterval() {
        LOG.trace("Setting Leos UI poll interval: {} millis", pollInterval);
        setPollInterval(pollInterval);
    }
}
