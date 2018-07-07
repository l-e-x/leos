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
package eu.europa.ec.leos.web.ui.component.layout;

import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NavigationUpdateEvent;
import eu.europa.ec.leos.web.event.component.HeaderResizeEvent;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import eu.europa.ec.leos.web.view.LogoutView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import java.util.Locale;

public class Header extends CustomComponent {

    private static final long serialVersionUID = 3924630817693865467L;

    private static final Logger LOG = LoggerFactory.getLogger(Header.class);

    public enum Action {
        EXPAND,
        COLLAPSE
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

    // initialize header layout
    private void initLayout() {
        // create header layout
        final VerticalLayout headerLayout = new VerticalLayout();
        headerLayout.setStyleName("leos-header-layout");

        // set header layout as composition root
        setCompositionRoot(headerLayout);
        addStyleName("leos-header");

        // build site banner
        final Component siteBanner = buildSiteBanner();
        headerLayout.addComponent(siteBanner);

        // build tools banner
        final Component toolsBanner = buildToolsBanner();
        headerLayout.addComponent(toolsBanner);
    }

    private @Nonnull
    Component buildSiteBanner() {
        // create grid layout (3 columns by 3 rows)
        final GridLayout layout = new GridLayout(3, 3);

        // layout will use all available space
        layout.setSizeFull();

        // header center column (column 1) will expand
        layout.setColumnExpandRatio(1, 1.0f);

        // KLUGE: workaround for fixing rendering issue (3rd column with zero width)
        layout.setColumnExpandRatio(2, 0.5f);

        // logo takes 3 rows (top-bottom)
        final Component logo = buildLogo();
        layout.addComponent(logo, 0, 0, 0, 2);
        layout.setComponentAlignment(logo, Alignment.BOTTOM_RIGHT);

        // services takes 2 columns (center+right)
        final Component services = buildServices();
        layout.addComponent(services, 1, 0, 2, 0);
        layout.setComponentAlignment(services, Alignment.TOP_RIGHT);

        // title
        final Component title = buildTitle();
        layout.addComponent(title, 1, 1);
        layout.setComponentAlignment(title, Alignment.BOTTOM_LEFT);

        // user
        final Component user = buildUser();
        layout.addComponent(user, 2, 1);
        layout.setComponentAlignment(user, Alignment.BOTTOM_RIGHT);

        // subtitle
        final Component subtitle = buildSubtitle();
        layout.addComponent(subtitle, 1, 2);
        layout.setComponentAlignment(subtitle, Alignment.TOP_LEFT);

        // logout
        final Component logout = buildLogout();
        layout.addComponent(logout, 2, 2);
        layout.setComponentAlignment(logout, Alignment.TOP_RIGHT);

        // create event bus subscriber to resize the header
        final Object headerResizeSubscriber = new Object() {
            @Subscribe
            public void headerResize(HeaderResizeEvent event) {
                Action action = event.getAction();
                LOG.trace("Handling header resize event... [action={}]", action);
                if (Action.COLLAPSE.equals(action)) {
                    LOG.debug("Collapsing the header...");
                    layout.setVisible(false);
                } else if (Action.EXPAND.equals(action)) {
                    LOG.debug("Expanding the header...");
                    layout.setVisible(true);
                } else {
                    LOG.debug("Ignoring unknown header resize action! [action={}]", action);
                }
            }
        };
        eventBus.register(headerResizeSubscriber);

        return layout;
    }

    private @Nonnull
    Component buildLogo() {
        // logo image
        final Image logo = new Image(null, LeosTheme.LEOS_HEADER_LOGO_RESOURCE);
        logo.addStyleName("leos-header-logo");
        return logo;
    }

    private @Nonnull
    Component buildServices() {
        // create services layout
        final HorizontalLayout layout = new HorizontalLayout();
        layout.addStyleName("leos-header-services");

        // enable spacing between components
        layout.setSpacing(true);

        // language label
        final Label langLabel = new Label(messageHelper.getMessage("leos.ui.header.language"));
        langLabel.setSizeUndefined();
        layout.addComponent(langLabel);
        layout.setComponentAlignment(langLabel, Alignment.MIDDLE_RIGHT);

        // language selection
        final ListSelect langSelector = new ListSelect();
        langSelector.setNullSelectionAllowed(false);
        langSelector.setRows(1);
        layout.addComponent(langSelector);
        layout.setComponentAlignment(langSelector, Alignment.MIDDLE_RIGHT);

        // fill selection with configured languages
        for (Locale locale : langHelper.getConfiguredLocales()) {
            String itemId = locale.toLanguageTag();
            String itemCaption = locale.getDisplayLanguage(locale) + " (" + locale.getLanguage() + ")";
            langSelector.addItem(itemId);
            langSelector.setItemCaption(itemId, itemCaption);
        }

        // selection of the current language
        langSelector.select(langHelper.getCurrentLocale().toLanguageTag());

        return layout;
    }

    private Component buildTitle() {
        // title
        final Label title = new Label(messageHelper.getMessage("leos.ui.header.title"));
        title.addStyleName("leos-header-title");
        return title;
    }

    private Component buildSubtitle() {
        // subtitle
        final Label subtitle = new Label(messageHelper.getMessage("leos.ui.header.subtitle"));
        subtitle.addStyleName("leos-header-subtitle");

        // KLUGE: fixed height to fix rendering issue (white line appears at the bottom)
        subtitle.setHeight("26px");

        return subtitle;
    }

    private @Nonnull
    Component buildUser() {
        // create user placeholder
        final VerticalLayout layout = new VerticalLayout();
        layout.addStyleName("leos-header-user");

        // layout will use all available space
        layout.setSizeFull();

        // user (user may be authenticated or not)
        Button userButton = new Button();
        userButton.setStyleName("link");

        if (securityContext.isUserAuthenticated()) {
            userButton.setCaption(
                    messageHelper.getMessage(
                            "leos.ui.header.user.authenticated.info",
                            securityContext.getUser().getName(),
                            securityContext.getUser().getLogin()));
            userButton.setDescription(
                    messageHelper.getMessage("leos.ui.header.user.authenticated.tooltip"));
            // userButton.addClickListener(clickListener); // FIXME handle button click
            // userButton.setData(leosUser); // FIXME set user as button data ???
        } else {
            userButton.setCaption(
                    messageHelper.getMessage(
                            "leos.ui.header.user.unauthenticated.info",
                            securityContext.getPrincipalName()));
            userButton.setDescription(
                    messageHelper.getMessage("leos.ui.header.user.unauthenticated.tooltip"));
            userButton.setEnabled(false);
        }

        layout.addComponent(userButton);
        layout.setComponentAlignment(userButton, Alignment.BOTTOM_CENTER);
        return layout;
    }

    private @Nonnull
    Component buildLogout() {
        // create logout placeholder
        final VerticalLayout layout = new VerticalLayout();
        layout.addStyleName("leos-header-logout");

        // layout will use all available space
        layout.setSizeFull();

        // KLUGE: fixed height to fix rendering issue (white line appears at the bottom)
        layout.setHeight("26px");

        // logout (user may be authenticated or not)
        String logoutCaptionKey = securityContext.isUserAuthenticated() ? "leos.ui.header.user.authenticated.logout"
                : "leos.ui.header.user.unauthenticated.exit";
        final Button logoutButton = new Button(messageHelper.getMessage(logoutCaptionKey));
        logoutButton.addStyleName("small");
        logoutButton.addClickListener(new ClickListener() {

            private static final long serialVersionUID = -6562136893288814670L;

            @Override
            public void buttonClick(ClickEvent event) {
                LOG.debug("Firing navigation request event... [viewId={}]", LogoutView.VIEW_ID);
                eventBus.post(new NavigationRequestEvent(LogoutView.VIEW_ID));
            }
        });

        layout.addComponent(logoutButton);
        layout.setComponentAlignment(logoutButton, Alignment.MIDDLE_CENTER);
        return layout;
    }

    private @Nonnull
    Component buildToolsBanner() {
        // create layout for navigation and tools
        final HorizontalLayout layout = new HorizontalLayout();
        layout.addStyleName("leos-header-tools");

        // layout will use all available space
        layout.setSizeFull();

        // navigation path
        final Component navigation = buildNavigationPath();
        layout.addComponent(navigation);
        layout.setComponentAlignment(navigation, Alignment.MIDDLE_LEFT);

        // navigation path takes available space
        layout.setExpandRatio(navigation, 1.0f);

        // resize tool
        final Component resizeTool = buildResizeTool();
        layout.addComponent(resizeTool);
        layout.setComponentAlignment(resizeTool, Alignment.MIDDLE_RIGHT);
        return layout;
    }

    private @Nonnull
    Component buildNavigationPath() {
        // create layout for navigation path
        final HorizontalLayout layout = new HorizontalLayout();
        layout.addStyleName("leos-header-navigation");

        // path separator
        Image pathSeparator;

        // european commission
        final Label ecLabel = new Label(messageHelper.getMessage("leos.ui.header.path.ec"));
        ecLabel.setSizeUndefined();
        layout.addComponent(ecLabel);
        layout.setComponentAlignment(ecLabel, Alignment.MIDDLE_LEFT);

        // path separator
        pathSeparator = new Image(null, LeosTheme.LEOS_HEADER_BREADCRUMB_RESOURCE);
        layout.addComponent(pathSeparator);
        layout.setComponentAlignment(pathSeparator, Alignment.MIDDLE_LEFT);

        // leos home
        final Button leosLink = new Button(messageHelper.getMessage("leos.ui.header.path.leos"));
        leosLink.setPrimaryStyleName("leos-header-breadcrumb");
        leosLink.setStyleName("link");
        leosLink.setData(""); // resolved to default view at runtime
        layout.addComponent(leosLink);
        layout.setComponentAlignment(leosLink, Alignment.MIDDLE_LEFT);

        // create button click listener to navigate to leos home
        leosLink.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = 3388459558980465073L;

            @Override
            public void buttonClick(Button.ClickEvent event) {
                String viewId = (String) leosLink.getData();
                LOG.debug("Firing navigation request event... [viewId={}]", viewId);
                eventBus.post(new NavigationRequestEvent(viewId));
            }
        });

        // path separator
        pathSeparator = new Image(null, LeosTheme.LEOS_HEADER_BREADCRUMB_RESOURCE);
        layout.addComponent(pathSeparator);
        layout.setComponentAlignment(pathSeparator, Alignment.MIDDLE_LEFT);

        // current view
        final Button viewLink = new Button();
        viewLink.setPrimaryStyleName("leos-header-breadcrumb");
        viewLink.setStyleName("link");
        layout.addComponent(viewLink);
        layout.setComponentAlignment(viewLink, Alignment.MIDDLE_LEFT);

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

        return layout;
    }

    private @Nonnull
    Component buildResizeTool() {
        // create button to resize the header
        // initial state is expanded by default
        final Button resizeButton = new Button();
        resizeButton.setStyleName("link");
        resizeButton.setData(Action.COLLAPSE);
        resizeButton.setIcon(LeosTheme.CHEVRON_UP_ICON_16);
        // resizeButton.setDescription(msgHelper.getMessage("leos.ui.header.resize.collapse"));

        // create button click listener to resize the header
        resizeButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = -2687518819714534752L;

            @Override
            public void buttonClick(Button.ClickEvent event) {
                if (Action.COLLAPSE.equals(resizeButton.getData())) {
                    LOG.debug("Firing header collapse event...");
                    eventBus.post(new HeaderResizeEvent(Action.COLLAPSE));
                    resizeButton.setData(Action.EXPAND);
                    resizeButton.setIcon(LeosTheme.CHEVRON_DOWN_ICON_16);
                    // resizeButton.setDescription(msgHelper.getMessage("leos.ui.header.resize.expand"));
                } else if (Action.EXPAND.equals(resizeButton.getData())) {
                    LOG.debug("Firing header expand event...");
                    eventBus.post(new HeaderResizeEvent(Action.EXPAND));
                    resizeButton.setData(Action.COLLAPSE);
                    resizeButton.setIcon(LeosTheme.CHEVRON_UP_ICON_16);
                    // resizeButton.setDescription(msgHelper.getMessage("leos.ui.header.resize.collapse"));
                } else {
                    LOG.warn("Ignoring unknown header resize action! [action={}]", resizeButton.getData());
                }
            }
        });
        return resizeButton;
    }
}
