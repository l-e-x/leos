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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener
@Component
public class LeosSessionListener implements HttpSessionListener {

    private static final Logger LOG = LoggerFactory.getLogger(LeosSessionListener.class);

    private static Set<HttpSession> sessionInstances = Collections.synchronizedSet(new HashSet<HttpSession>());

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        LOG.info("Creating http session");
        sessionInstances.add(event.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        sessionInstances.remove(event.getSession());
    }

    @Scheduled(cron = "${maintenance.session.invalid.cron}")
    public void invalidateAllSessions() {
        LOG.info("Invalidating all http sessions");
        Set<HttpSession> sessionsToRemove = new HashSet<HttpSession>(sessionInstances);
        for (HttpSession session : sessionsToRemove) {
            try {
                session.invalidate();
            } catch (Exception e) {
                LOG.info("The session is already invalidated.");
            }
        }
        LOG.trace("Nr. of remaining sessionInstances - " + sessionInstances.size());
    }

}
