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
package eu.europa.ec.leos.web.support.vaadin;

import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope("singleton")
@Component("authenticationSessionListener")
public class AuthenticationSessionListener implements SessionInitListener {

    private static final long serialVersionUID = -2240619531224616575L;

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationSessionListener.class);

    @Autowired
    private SecurityContext securityContext;

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        LOG.trace("Authentication session init...");
        LOG.trace("Session is new: {}", event.getSession().getSession().isNew());
        LOG.trace("Session id: {}", event.getSession().getSession().getId());

        // FIXME hardcoded user for demo purposes
        User user = new User(1L, "demo", "Demo User");
        LOG.warn("Hardcoded user: {}", user);
        securityContext.setPrincipalName(user.getName());
        securityContext.setUser(user);
    }
}
