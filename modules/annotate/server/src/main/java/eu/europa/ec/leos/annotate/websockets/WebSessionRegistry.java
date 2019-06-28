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
package eu.europa.ec.leos.annotate.websockets;

import eu.europa.ec.leos.annotate.model.UserInformation;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class WebSessionRegistry {
    /* lookup that holds all users and their sessions */
    private final ConcurrentMap<String, UserInformation> sessionUserInfo = new ConcurrentHashMap<>();

    /* lookup across all sessions by id */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final Object lock = new Object();

    public WebSocketSession getSession(final String sessionId) {
        return this.sessions.get(sessionId);
    }

    public UserInformation getUserInfo(final String sessionId) {
        return this.sessionUserInfo.get(sessionId);
    }

    public void registerSession(final WebSocketSession session, final UserInformation userInformation) {
        Assert.notNull(session, "Session ID must not be null");
        Assert.notNull(session.getId(), "Session ID must not be null");

        synchronized (this.lock) {
            final WebSocketSession ses = this.sessions.get(session.getId());
            if (ses == null) {
                this.sessions.put(session.getId(), session);
                this.sessionUserInfo.put(session.getId(), userInformation);
            }
            //else Do nothing. Don't register second time.
        }
    }

    public void unregisterSession(final String sessionId) {
        Assert.notNull(sessionId, "Session ID must not be null");
        synchronized (lock) {
            this.sessions.remove(sessionId);
            this.sessionUserInfo.remove(sessionId);
        }
    }
}
