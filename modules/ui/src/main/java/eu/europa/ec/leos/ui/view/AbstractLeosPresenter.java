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
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.common.TocMode;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.store.WorkspaceService;
import eu.europa.ec.leos.services.support.xml.ref.Ref;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.support.UuidHelper;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

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
    protected final PackageService packageService;
    protected final WorkspaceService workspaceService;
    
    protected AbstractLeosPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
            EventBus leosApplicationEventBus, UuidHelper uuidHelper, PackageService packageService, WorkspaceService workspaceService) {
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
        this.packageService = packageService;
        this.workspaceService = workspaceService;
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

    protected String wrapXmlFragment(String xmlFragment) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aknFragment xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">" +
                xmlFragment + "</aknFragment>";
    }

}
