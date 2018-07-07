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
package eu.europa.ec.leos.web.ui.component.layout;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.event.MouseEvents;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NavigationUpdateEvent;
import eu.europa.ec.leos.web.event.component.HeaderResizeEvent;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import eu.europa.ec.leos.web.ui.themes.Themes;
import eu.europa.ec.leos.web.view.LogoutView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.teemu.VaadinIcons;

import javax.annotation.Nonnull;
import java.util.Locale;

public class Header extends CustomLayout {
    private static final long serialVersionUID = 3924630817693865467L;

    private static final Logger LOG = LoggerFactory.getLogger(Header.class);
    private static final String HEADER_HEIGHT = "123px";
    private static final String HEADER_BAR_HEIGHT = "48px";
    private static  String TEMPLATE_NAME="HeaderTemplate.html";
	
    public enum Action {
        MAXIMIZE,
        MINIMIZE
    }

    private LanguageHelper langHelper;
    private MessageHelper messageHelper;
    private EventBus eventBus;
    private SecurityContext securityContext;

    public Header(final LanguageHelper langHelper, final MessageHelper msgHelper, final EventBus eventBus, final SecurityContext securityContext) {
        this.langHelper = langHelper;
        this.messageHelper = msgHelper;
        this.eventBus = eventBus;
        this.securityContext = securityContext;
        LOG.trace("Initializing header...");
        initLayout();
    }

    private void initLayout() {
        setTemplate();
        setHeight(HEADER_HEIGHT);

        addComponent(buildLanguageSelector(), "languagesDropdown");
        addComponent(buildUser(), "user");
        addComponent(buildLogout(), "logoutButton");
        addComponent(buildTitle(), "applicationName");
        addComponent(buildLogo(), "headerLogo");
        addComponent(buildHomeButton(), "homeIcon");
        addComponent(buildNavigationPath(), "breadcrumb");
        addComponent(buildResizeTool(), "resizeButton");
    }

    private void setTemplate() {
        try { //TODO hack fix to read file from a jar
			initTemplateContentsFromInputStream(getClass().getResourceAsStream("/VAADIN/themes/"+Themes.DECIDE+"/layouts/" + TEMPLATE_NAME));
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }			
	}
	
    private @Nonnull Component buildLogo() {
        // logo image
        return new Image(null, LeosTheme.LEOS_HEADER_LOGO_RESOURCE_NEW);
    }

    private @Nonnull Component buildLanguageSelector() {
        final ListSelect langSelector = new ListSelect();
        langSelector.setNullSelectionAllowed(false);
        langSelector.setRows(1);

        // fill selection with configured languages
        for (Locale locale : langHelper.getConfiguredLocales()) {
            String itemId = locale.toLanguageTag();
            String itemCaption = locale.getDisplayLanguage(locale) + " (" + locale.getLanguage() + ")";
            langSelector.addItem(itemId);
            langSelector.setItemCaption(itemId, itemCaption);
        }
        // selection of the current language
        langSelector.select(langHelper.getCurrentLocale().toLanguageTag());
        return langSelector;
    }

    private Component buildTitle() {
        final Label title = new Label();
        title.setContentMode(ContentMode.HTML);
        title.setValue(messageHelper.getMessage("leos.ui.header.title"));
        title.setWidthUndefined();
        return title;
    }

    private @Nonnull Component buildUser() {
        // user (user may be authenticated or not)
        Label user = new Label();
        user.setIcon(VaadinIcons.USER);

        if (securityContext.isUserAuthenticated()) {
            user.setValue(messageHelper.getMessage("leos.ui.header.user.authenticated.info", securityContext.getUser().getName(),
                    securityContext.getUser().getLogin()));
        } else {
            user.setValue(messageHelper.getMessage("leos.ui.header.user.unauthenticated.info", securityContext.getPrincipalName()));
        }
        return user;
    }

    private @Nonnull Component buildLogout() {
        String logoutCaptionKey = securityContext.isUserAuthenticated()
                ? "leos.ui.header.user.authenticated.logout"
                : "leos.ui.header.user.unauthenticated.exit";
        final Button logoutButton = new Button();
        logoutButton.setStyleName(ValoTheme.BUTTON_ICON_ONLY);
        logoutButton.setIcon(FontAwesome.POWER_OFF);
        logoutButton.setDescription(messageHelper.getMessage(logoutCaptionKey));
        logoutButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = -6562136893288814670L;

            @Override
            public void buttonClick(Button.ClickEvent event) {
                LOG.debug("Firing navigation request event... [viewId={}]", LogoutView.VIEW_ID);
                eventBus.post(new NavigationRequestEvent(LogoutView.VIEW_ID));
            }
        });
        return logoutButton;
    }

    private @Nonnull Component buildHomeButton() {
        final Image home = new Image(null, LeosTheme.LEOS_HEADER_HOME_ICON);
        home.setData(""); // resolved to default view at runtime
        home.addClickListener(new MouseEvents.ClickListener() {
            private static final long serialVersionUID = 3388459558980465073L;

            public void click(MouseEvents.ClickEvent event) {
                String viewId = (String) home.getData();
                LOG.debug("Firing navigation request event... [viewId={}]", viewId);
                eventBus.post(new NavigationRequestEvent(viewId));
            }
        });
        return home;
    }

    private @Nonnull Component buildNavigationPath() {
        // current view
        final Button viewLink = new Button();
        viewLink.setPrimaryStyleName("leos-header-breadcrumb");
        viewLink.setStyleName("link");
        viewLink.setIcon(VaadinIcons.ANGLE_RIGHT);

        // create button click listener to navigate to current view
        viewLink.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = -5591847213901701357L;

            @Override
            public void buttonClick(Button.ClickEvent event) {
                String viewId = (String) viewLink.getData();
                LOG.debug("Firing navigation request event... [viewId={}]", viewId);
                eventBus.post(new NavigationRequestEvent(viewId));
            }
        });

        // create event bus subscriber to handle navigation updates
        final Object navigationUpdateSubscriber = new Object() {

            @Subscribe
            public void navigationUpdate(NavigationUpdateEvent event) {
                LOG.trace("Handling navigation update event... [viewId={}, viewKey={}]", event.getViewId(), event.getViewKey());
                viewLink.setData(event.getViewId());
                viewLink.setCaption(messageHelper.getMessage(event.getViewKey()));
            }
        };
        eventBus.register(navigationUpdateSubscriber);

        return viewLink;
    }

    private @Nonnull Component buildResizeTool() {
        // create button to resize the header
        // initial state is expanded by default
        final Button resizeButton = new Button();
        resizeButton.setPrimaryStyleName(ValoTheme.BUTTON_ICON_ONLY);
        resizeButton.setIcon(VaadinIcons.CARET_SQUARE_UP_O);

        resizeButton.setData(Action.MINIMIZE);
        resizeButton.setDescription(messageHelper.getMessage("leos.ui.header.resize.minimize"));

        // create button click listener to resize the header
        resizeButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = -2687518819714534752L;

            @Override
            public void buttonClick(Button.ClickEvent event) {
                if (Action.MINIMIZE.equals(resizeButton.getData())) {
                    LOG.debug("Firing header minimize event...");
                    eventBus.post(new HeaderResizeEvent(Action.MINIMIZE));
                    resizeButton.setData(Action.MAXIMIZE);
                    resizeButton.setIcon(VaadinIcons.CARET_SQUARE_DOWN_O);
                    resizeButton.setDescription(messageHelper.getMessage("leos.ui.header.resize.maximize"));
                } else if (Action.MAXIMIZE.equals(resizeButton.getData())) {
                    LOG.debug("Firing header maximize event...");
                    eventBus.post(new HeaderResizeEvent(Action.MAXIMIZE));
                    resizeButton.setData(Action.MINIMIZE);
                    resizeButton.setIcon(VaadinIcons.CARET_SQUARE_UP_O);
                    resizeButton.setDescription(messageHelper.getMessage("leos.ui.header.resize.minimize"));
                } else {
                    LOG.warn("Ignoring unknown header resize action! [action={}]", resizeButton.getData());
                }
            }
        });

        // create event bus subscriber to resize the header
        final Object headerResizeSubscriber = new Object() {
            @Subscribe
            public void headerResize(HeaderResizeEvent event) {
                Action action = event.getAction();
                LOG.trace("Handling header resize event... [action={}]", action);
                if (Action.MINIMIZE.equals(action)) {
                    LOG.debug("Minimizing the header...");
                    addStyleName(Action.MINIMIZE.toString().toLowerCase());
                    setHeight(HEADER_BAR_HEIGHT);
                } else if (Action.MAXIMIZE.equals(action)) {
                    LOG.debug("Expanding the header...");
                    removeStyleName(Action.MINIMIZE.toString().toLowerCase());
                    setHeight(HEADER_HEIGHT);
                } else {
                    LOG.debug("Ignoring unknown header resize action! [action={}]", action);
                }
            }
        };
        eventBus.register(headerResizeSubscriber);
        return resizeButton;
    }
}
