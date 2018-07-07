/**
 * Copyright 2015 European Commission
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
package eu.europa.ec.leos.web.ui;

import java.util.ArrayList;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.*;
import com.vaadin.ui.Notification.Type;

import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.web.event.LeosUIComponentsStateUpdateEvent;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NavigationUpdateEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.layout.Footer;
import eu.europa.ec.leos.web.ui.component.layout.Header;
import eu.europa.ec.leos.web.ui.screen.LeosScreen;
import eu.europa.ec.leos.web.view.UnauthorizedView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.xpoft.vaadin.DiscoveryNavigator;

/**
 * LEOS web application main entry point.
 */
@Scope("session")
@Component("leosUI")
@PreserveOnRefresh
@Theme("leos")
@JavaScript({"vaadin://js/web/bootstrapLeosEditor.js" + LeosCacheToken.TOKEN})
public class LeosUI extends UI {

    private static final long serialVersionUID = 3748690254562200999L;

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

    private ComponentContainer body;

    @Override
    protected void init(VaadinRequest request) {
        LOG.trace("Leos UI initializing...");

        // initialize UI layout
        initLayout();

        // initialize UI navigator
        initNavigator();

        // initialize error handler
        initErrorHandler();

        registerNotificationSubscriber();

        LOG.trace("Leos UI is initialized!");
    }

    private void initLayout() {
        LOG.trace("Initializing layout...");

        // ui will use all available space
        addStyleName("leos-ui");
        setSizeFull();

        // ui layout will use all available space
        final VerticalLayout leosLayout = new VerticalLayout();
        leosLayout.addStyleName("leos-ui-layout");
        leosLayout.setSizeFull();
        setContent(leosLayout);

        // create body container to display views
        body = new VerticalLayout();
        body.addStyleName("leos-body");
        body.setSizeFull();

        // classic header/body/footer layout
        final Header header = new Header(langHelper, messageHelper, eventBus, securityContext);
        leosLayout.addComponent(header);

        leosLayout.addComponent(body);

        final Footer footer = new Footer(messageHelper);
        leosLayout.addComponent(footer);

        // expand body to use all available space
        leosLayout.setExpandRatio(body, 1.0f);

        eventBus.register(new Object() {
            @Subscribe
            public void updateComponentsState(LeosUIComponentsStateUpdateEvent leosUIComponentsStateUpdateEvent) {
                boolean state = leosUIComponentsStateUpdateEvent.isDisabled();

                header.setEnabled(state);
                body.setEnabled(state);
                footer.setEnabled(state);
            }
        });
    }

    private void initNavigator() {
        LOG.trace("Initializing navigator...");

        // use body as the view display
        final ViewDisplay viewDisplay = new Navigator.ComponentContainerViewDisplay(body);

        // create a navigator to control the views
        final DiscoveryNavigator navigator = new DiscoveryNavigator(this, viewDisplay);

        // register default view
        final String defaultViewId = cgfHelper.getProperty("leos.view.default.id");
        LOG.trace("Registering default view ({})...", defaultViewId);

        // views are auto-discovered from the spring context, but we need to
        // manually register the default view as the navigator default entry point
        final View defaultView = navigator.getView(defaultViewId, defaultViewId, false);
        navigator.addView("", defaultView);

        // create event bus subscriber to handle navigation requests
        final Object navigationRequestSubscriber = new Object() {
            @Subscribe
            public void navigationRequest(NavigationRequestEvent event) {
                String viewId = event.getViewId();
                LOG.trace("Handling navigation request event... [viewId={}]", viewId);
                LOG.debug("Navigation request... [viewId={}]", viewId);
                UI.getCurrent().getNavigator().navigateTo(viewId);
            }
        };
        eventBus.register(navigationRequestSubscriber);

        // create view change listener to handle automatic-closing of the opened windows
        navigator.addViewChangeListener(new ViewChangeListener() {
            @Override
            public boolean beforeViewChange(ViewChangeEvent viewChangeEvent) {
            	ArrayList<Window> arrWindows = new ArrayList<Window>();
            	arrWindows.addAll(UI.getCurrent().getWindows());
            	for(Window window: arrWindows){
                    LOG.info("Automatic closing of the window {}", window.getCaption());
                    window.close();
                }
                return true;
            }

            @Override
            public void afterViewChange(ViewChangeEvent viewChangeEvent) {
                // nothing
            }
        });

        // create view change listener to handle navigation updates
        navigator.addViewChangeListener(new ViewChangeListener() {
            private static final long serialVersionUID = -806089256160487161L;

            @Override
            public boolean beforeViewChange(ViewChangeEvent event) {
                boolean allowViewChange;
                // unauthorized view id
                final String unauthorizedViewId = UnauthorizedView.VIEW_ID;

                // verify user authorization before allowing view navigation
                if (securityContext.isUserAuthenticated()) {
                    // disallow authenticated user from entering the unauthorized view
                    allowViewChange = !unauthorizedViewId.equals(event.getViewName());
                    if (!allowViewChange) {
                        if (event.getOldView() == null) {
                            // first access, redirect to default view
                            LOG.trace("Redirecting authenticated user '{}' to default view... [viewId={}]",
                                    securityContext.getPrincipalName(),
                                    defaultViewId);
                            navigator.navigateTo(defaultViewId);
                        } else {
                            LOG.trace("Denying view change to authenticated user '{}'... [viewId={}]", securityContext.getPrincipalName(), event.getViewName());
                        }
                    }
                } else {
                    // allow unauthenticated user to only enter the unauthorized view
                    allowViewChange = unauthorizedViewId.equals(event.getViewName());
                    if (!allowViewChange) {
                        // redirect to unauthorized view
                        LOG.trace("Redirecting unauthenticated user '{}' to unauthorized view... [viewId={}]", securityContext.getPrincipalName(),
                                unauthorizedViewId);
                        navigator.navigateTo(unauthorizedViewId);
                    }
                }

                if (allowViewChange) {
                    LOG.trace("User '{}' is authorized to access view! [viewId={}, allowViewChange={}]", securityContext.getPrincipalName(),
                            event.getViewName(), allowViewChange);
                } else {
                    LOG.trace("User '{}' was NOT authorized to access view! [viewId={}, allowViewChange={}]", securityContext.getPrincipalName(),
                            event.getViewName(), allowViewChange);
                }

                return allowViewChange;
            }

            @Override
            public void afterViewChange(ViewChangeEvent event) {
                LeosScreen newView = (LeosScreen) event.getNewView();
                LOG.debug("Firing navigation update event... [viewId={}]", newView.getViewId());
                eventBus.post(new NavigationUpdateEvent(newView.getViewId(), newView.getViewKey()));
            }
        });
    }

    // Error handler method to customize vaadin error messages
    private void initErrorHandler() {

        // Override the DefaultErrorHandler
        setErrorHandler(new DefaultErrorHandler() {
            private static final long serialVersionUID = 8592416549111863840L;

            @Override
            public void error(com.vaadin.server.ErrorEvent event) {
                // Find the final cause
                Throwable throwable = event.getThrowable();
                LOG.error("An error occurred!", throwable);
                eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "error.message", throwable.getMessage()));
            }
        });
    }

    private void registerNotificationSubscriber() {
        eventBus.register(new Object() {
            @Subscribe
            public void showNotificationMessage(NotificationEvent notificationEvent) {

                String message = messageHelper.getMessage(notificationEvent.getMessageKey(), notificationEvent.getArgs());
                Notification.Type notificationType;
                switch (notificationEvent.getType()) {
                    case INFO:
                        notificationType = Type.HUMANIZED_MESSAGE;
                        break;
                    case WARNING:
                        notificationType = Type.WARNING_MESSAGE;
                        break;
                    case ERROR:
                        notificationType = Type.ERROR_MESSAGE;
                        break;
                    default:
                        // default handle it as an error
                        notificationType = Type.ERROR_MESSAGE;
                }
                Notification.show(message, notificationType);
            }
        });
    }
}
