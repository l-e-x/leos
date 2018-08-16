/*
 * Copyright 2018 European Commission
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
import eu.europa.ec.leos.security.SecurityContext;
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

    protected AbstractLeosPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus) {
        LOG.trace("Initializing presenter...");
        Validate.notNull(securityContext, "SecurityContext must not be null!");
        this.securityContext = securityContext;
        Validate.notNull(httpSession, "HttpSession must not be null!");
        this.httpSession = httpSession;
        Validate.notNull(eventBus, "EventBus must not be null!");
        this.eventBus = eventBus;
    }

    @Override
    public void attach() {
        LOG.trace("Attaching presenter...");
        eventBus.register(this);
    }

    @Override
    public void detach() {
        LOG.trace("Detaching presenter...");
        eventBus.unregister(this);
    }

    @Override
    public void enter() {
        LOG.trace("Entering presenter...");
    }
}
