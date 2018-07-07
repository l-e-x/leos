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
package eu.europa.ec.leos.web.support;

import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import eu.europa.ec.leos.services.locking.LockUpdateBroadcastListener;
import eu.europa.ec.leos.services.locking.LockingService;

@WebListener
public class SessionExpiredListener implements HttpSessionListener {

    private static final Logger LOG = LoggerFactory.getLogger(SessionExpiredListener.class);

    protected final AtomicReference<LockingService> autoWiredDelegate = new AtomicReference<>();

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        HttpSession httpSession = httpSessionEvent.getSession();

        //leave all views from all presenters
        try {
            LockingService lockingService = getLockingService(httpSession);
            unregisterListeners(httpSession, lockingService);
            unlockDocument(httpSession, lockingService);
        } catch (Exception e) {
            LOG.warn("Unable to handle the session destroy!", e);
        }

    }

    protected void unregisterListeners(HttpSession httpSession, LockingService lockingService) {
        Enumeration<String> attributeNames = httpSession.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object sessionAttribute = httpSession.getAttribute(attributeName);
            if (sessionAttribute instanceof LockUpdateBroadcastListener) {
                lockingService.unregisterLockInfoBroadcastListener((LockUpdateBroadcastListener) sessionAttribute);
            }
        }
    }

    protected void unlockDocument(HttpSession httpSession, LockingService lockingService) {
        String documentId = (String) httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name());
        //in single session, a user can work only on one docuemnt.
        if (documentId != null) {
            LOG.debug("release lock on session expired for documentId: " + documentId);
            lockingService.releaseLocksForSession(documentId, httpSession.getId());
        }
    }

    /**
     * Get the LockingService from the context. As we don;t have autowiring support here, get it manually and store its reference
     */
    protected LockingService getLockingService(HttpSession httpSession) {
        LockingService existingValue = autoWiredDelegate.get();
        if (existingValue == null) {
            WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(httpSession.getServletContext());
            LockingService lockingService = webApplicationContext.getBean(LockingService.class);

            autoWiredDelegate.compareAndSet(null, lockingService);
        }

        return autoWiredDelegate.get();
    }
}
