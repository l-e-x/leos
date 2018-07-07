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
package eu.europa.ec.leos.web.support.vaadin;

import com.vaadin.server.*;

import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.model.user.Department;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.user.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Scope("singleton")
@Component("authenticationSessionListener")
public class AuthenticationSessionListener implements SessionInitListener {

    private static final long serialVersionUID = -2240619531224616575L;

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationSessionListener.class);

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityContext securityContext;

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        LOG.trace("Authentication session init...");

        WrappedSession session = event.getSession().getSession();
        LOG.trace("Session is new: {}", session.isNew());
        LOG.trace("Session id: {}", session.getId());

        Principal userPrincipal = event.getRequest().getUserPrincipal();
        
        if (userPrincipal != null) {
            securityContext.setPrincipalName(userPrincipal.getName());
            LOG.trace("User principal name: '{}'", securityContext.getPrincipalName());

            User user = userService.getUser(securityContext.getPrincipalName());
            user.setDepartment(resolveDepartment(userPrincipal));
            securityContext.setUser(user);

            if (securityContext.isUserAuthenticated()) {
                LOG.info("User authentication succeeded! [{}]", securityContext.getUser());
            } else {
                LOG.warn("User authentication failed! [login={}]", securityContext.getPrincipalName());
            }
        } else {
            LOG.warn("No user principal found in request!");
            securityContext.setPrincipalName(null);
            securityContext.setUser(null);
        }
    }

    protected Department resolveDepartment(Principal principal) {
        //FIXME hardcoded for demo purposes
        return new Department(null);
    }
}