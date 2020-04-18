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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.web.websocket.SubscriptionRequest;
import eu.europa.ec.leos.annotate.services.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotateWebSocketHandler extends TextWebSocketHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AnnotateWebSocketHandler.class);

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    private final AuthenticationService authService;
    private final WebSessionRegistry webSessionRegistry;
    private final MessageBroker messageBroker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnnotateWebSocketHandler(final AuthenticationService authService, final WebSessionRegistry webSessionRegistry, final MessageBroker messageBroker) {
        this.authService = authService;
        this.webSessionRegistry = webSessionRegistry;
        this.messageBroker = messageBroker;
    }

    @Override
    public void handleTextMessage(final WebSocketSession session, final TextMessage message) throws IOException {
        try {
            if (session == null || session.getId() == null) {
                LOG.error("Invalid session ");
                if (session != null) {
                    session.close(CloseStatus.NOT_ACCEPTABLE);
                }
                return;
            }

            final Map<String, Object> jsonObject = getAsJsonObject(message);
            final String type = getMessageType(jsonObject);
            LOG.trace("{}: Message received :{}", session.getId(), jsonObject);

            if (session.isOpen() && type != null) {
                switch (type) {
                    case "access-token":
                        // this new message has to be added as requestParamaters are not passed along in websocket request
                        // AbstractTyrusRequestUpgradeStrategy(line 106)-->String path = "/" + random.nextLong();
                        // Where as for tomcat TomcatRequestUpgradeStrategy(line 62) -->String path = servletRequest.getRequestURI(); // shouldn't matter
                        handleAccessToken(jsonObject, session);
                        break;
                    case "ping":
                        handlePing(jsonObject, session);
                        break;
                    case "whoami":
                        handleWhoAmI(jsonObject, session);
                        break;
                    case "client_id":
                        handleClientId(jsonObject, session);
                        break;
                    case "filter":
                        handleFilter(message, session);
                        break;
                    default:
                        LOG.warn("Unknown text message received via websocket {}", type);
                        break;
                }
            }
        } catch (RuntimeException e) {
            LOG.error("Exception occurred", e);
            if (session != null) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        }
    }

    private void handleAccessToken(final Map<String, Object> jsonObject, final WebSocketSession session) throws IOException {
        if (webSessionRegistry.getSession(session.getId()) != null) {
            // user already authenticated. Do not do anything more
            // We may have to change this if client sends a different authtoken
            return;
        }
        final String token = (String) jsonObject.get("value");
        final boolean authenticated = authenticateAndStore(token, session);
        if (!authenticated) {
            LOG.error("Login failed !!");
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    private void handlePing(final Map<String, Object> jsonObject, final WebSocketSession session) throws JsonProcessingException, IOException {
        final HashMap<String, Object> response = new HashMap<>();
        response.put("type", "pong");
        response.put("ok", Boolean.TRUE);
        response.put("reply_to", jsonObject.get("id"));
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response), true));
    }

    private void handleClientId(final Map<String, Object> jsonObject, final WebSocketSession session) throws IOException {
        if (webSessionRegistry.getSession(session.getId()) == null) {
            // if user is not registered .. reject and close the session
            session.close(CloseStatus.PROTOCOL_ERROR);
            return;
        }
        webSessionRegistry.getUserInfo(session.getId()).setClientId((String) jsonObject.get("value"));
    }

    private void handleWhoAmI(final Map<String, Object> jsonObject, final WebSocketSession session) throws IOException {
        if (webSessionRegistry.getSession(session.getId()) == null) {
            // if user is not registered .. reject and close the session
            session.close(CloseStatus.PROTOCOL_ERROR);
            return;
        }
        final HashMap<String, Object> response = new HashMap<>();
        final UserInformation userInformation = webSessionRegistry.getUserInfo(session.getId());
        response.put("type", "whoyouare");
        response.put("ok", Boolean.TRUE);
        response.put("reply_to", jsonObject.get("id"));
        response.put("userid", String.format("acct:%s@%s", userInformation.getLogin(), userInformation.getAuthority()));
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response), true));
    }

    private void handleFilter(final TextMessage message, final WebSocketSession session) throws IOException {
        if (webSessionRegistry.getSession(session.getId()) == null) {
            // if user is not registered .. reject and close the session
            session.close(CloseStatus.PROTOCOL_ERROR);
            return;
        }
        // No Reply needed
        // Subscribe based on filter.actions.[create/delete/update]
        // [match_policy] [clauses.[field][operator][List{value}]]
        final SubscriptionRequest subscriptionRequest = objectMapper.readValue(message.asBytes(), SubscriptionRequest.class);
        messageBroker.subscribe(subscriptionRequest, session, webSessionRegistry.getUserInfo(session.getId()));

    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAsJsonObject(final TextMessage message) throws IOException {
        return objectMapper.readValue(message.asBytes(), Map.class);
    }

    private String getMessageType(final Map<String, Object> jsonObject) {
        String type = (String) jsonObject.get("type");
        type = (type == null) ? (String) jsonObject.get("messageType") : type;
        type = (type == null && jsonObject.get("filter") != null) ? "filter" : type;
        return type;
    }

    private boolean authenticateAndStore(final String accessToken, final WebSocketSession session) {
        // check if access token is valid for the user.
        final UserInformation userInformation = authService.findUserByAccessToken(accessToken);
        if (userInformation != null && userInformation.getLogin() != null) {
            webSessionRegistry.registerSession(session, userInformation);
            LOG.debug("Web socket connected :{}, {}", session.getId(), userInformation.getLogin());
            return true;
        }
        return false;
    }

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) {
        // the messages will be broadcasted to all users.
        LOG.trace("Websocket opened :{}", session.getId());
        if (webSessionRegistry.getSession(session.getId()) == null && session.getUri().getQuery() != null) {
            final String accessToken = decodeQueryString(session.getUri().getQuery()).get("access_token");
            authenticateAndStore(accessToken, session);
            // even if it is false...do nothing as for weblogic it would come later
        }
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
        LOG.trace("Websocket closed :{}", session.getId());
        webSessionRegistry.unregisterSession(session.getId());
        messageBroker.unsubscribe(session.getId());
    }

    public Map<String, String> decodeQueryString(final String query) {
        try {
            final Map<String, String> params = new ConcurrentHashMap<String, String>();
            for (final String param : query.split("&")) {
                final String[] keyValue = param.split("=", 2);
                final String key = URLDecoder.decode(keyValue[0], "UTF-8");
                final String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], "UTF-8") : "";
                if (!key.isEmpty()) {
                    params.put(key, value);
                }
            }
            return params;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e); // Cannot happen with UTF-8 encoding.
        }
    }
}