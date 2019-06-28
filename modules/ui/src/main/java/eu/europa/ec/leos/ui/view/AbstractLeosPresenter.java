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
package eu.europa.ec.leos.ui.view;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.UI;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.web.support.UuidHelper;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSession;

/**
 * Base class for all LEOS presenters.
 */
public abstract class AbstractLeosPresenter implements LeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractLeosPresenter.class);

    protected final SecurityContext securityContext;
    protected final HttpSession httpSession;
    protected final EventBus eventBus;
    protected final EventBus leosApplicationEventBus;
    protected final UI leosUI;
    protected final User user;
    protected final String id;

    protected AbstractLeosPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
            EventBus leosApplicationEventBus, UuidHelper uuidHelper) {
        LOG.trace("Initializing presenter...");
        Validate.notNull(securityContext, "SecurityContext must not be null!");
        this.securityContext = securityContext;
        User currentUser = securityContext.getUser();
        Validate.notNull(currentUser, "User must not be null!");
        this.user = currentUser;
        Validate.notNull(httpSession, "HttpSession must not be null!");
        this.httpSession = httpSession;
        Validate.notNull(eventBus, "EventBus must not be null!");
        this.eventBus = eventBus;
        Validate.notNull(leosApplicationEventBus, "Leos Application EventBus must not be null!");
        this.leosApplicationEventBus = leosApplicationEventBus;
        this.leosUI = UI.getCurrent();
        Validate.notNull(uuidHelper, "UuidHelper must not be null!");
        this.id = uuidHelper.getRandomUUID();
    }

    @Override
    public void attach() {
        LOG.trace("Attaching presenter...");
        eventBus.register(this);
        leosApplicationEventBus.register(this);
    }

    @Override
    public void detach() {
        LOG.trace("Detaching presenter...");
        eventBus.unregister(this);
        leosApplicationEventBus.unregister(this);
    }

    @Override
    public void enter() {
        LOG.trace("Entering presenter...");
    }

    @Override
    public final String getId() {
        return id;
    }
}
