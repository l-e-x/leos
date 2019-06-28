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

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonDeleteSuccessResponse;
import eu.europa.ec.leos.annotate.model.web.websocket.*;
import eu.europa.ec.leos.annotate.services.AnnotationConversionService;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class MessageBroker {
    private static final Logger LOG = LoggerFactory.getLogger(MessageBroker.class);
    private final AbstractQueue<Subscription> subscriptions = new ConcurrentLinkedQueue<>();
    private final AbstractQueue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor = messageExecutor();

    final private AnnotationService annotationService;

    final private AnnotationConversionService conversionService;
    
    @Autowired
    public MessageBroker(final AnnotationService annotationService, final AnnotationConversionService conversionService) {
        this.annotationService = annotationService;
        this.conversionService = conversionService;
    }

    public enum ACTION {
        CREATE, UPDATE, DELETE
    }

//    @PostConstruct
//    public void init() {
//    }

    public void subscribe(final SubscriptionRequest subscriptionRequest, final WebSocketSession subscriber, final UserInformation userInformation)
            throws IllegalArgumentException {
        final Filter filter = subscriptionRequest.getFilter();
        filter.getClauses().forEach(clause -> {
            if (clause.getField() == null || !clause.getField().equalsIgnoreCase("/uri")) {
                throw new IllegalArgumentException("No subscription supported for :" + clause.getField());
            }

            if (clause.getOperator() == null || !clause.getOperator().equalsIgnoreCase("one_of")) {
                throw new IllegalArgumentException("No subscription supported for :" + clause.getOperator());
            }

            if (clause.getValue() == null || clause.getValue().size() == 0) {
                throw new IllegalArgumentException("No subscription supported for null url");
            }
        });
        subscriptions.add(new Subscription(filter, subscriber, userInformation));
    }

    public void unsubscribe(final String sessionId) {
        for (final Subscription subs : subscriptions) {
            if (subs.getSubscriber().getId().equals(sessionId)) {
                subscriptions.remove(subs);
            }
        }
    }

    public void publish(final String annotationId, final ACTION action, final String sender) {
        if (action != null && annotationId != null) {
            pendingMessages.add(new Message(action, annotationId, sender));
        }
    }

    @Scheduled(fixedDelay = 3000, initialDelay = 15000)
    public void updateSubscribers() {
        for (final Message message : pendingMessages) {
            LOG.debug("updating clients for pending messages");
            pendingMessages.remove(message);

            // Create the message in format
            final JsonNotification notification = new JsonNotification(message.getAction().toString().toLowerCase(Locale.ENGLISH));
            Annotation annotation = null;
            if (message.getAction().equals(ACTION.DELETE)) {
                notification.addPayload(new JsonDeleteSuccessResponse(message.getId()));
            } else {
                annotation = annotationService.findAnnotationById(message.getId());
            }

            for (final Subscription subs : subscriptions) {
                if (checkSubscription(subs, message, annotation)) {
                    if (annotation != null) {
                        // the conversion of the annotation to JSON was moved here for two reasons:
                        // a) we only do it when it is actually required
                        // b) we need the subscriber's authority and user info
                        notification.addPayload(conversionService.convertToJsonAnnotation(annotation, subs.getUser()));
                    }
                    threadPoolTaskExecutor.submit(new SendTask(notification, subs.getSubscriber()));
                }
            }
        }
    }

    // annotation would be null for delete case
    @SuppressWarnings("PMD.ConfusingTernary")
    private boolean checkSubscription(final Subscription subscription, final Message message, final Annotation annotation) {
        // order of checks is important as we are using elimination method to simplify checks
        if (message.getClientId().equals(subscription.getUser().getClientId())) {
            return false;
        } else if (message.getAction().equals(ACTION.DELETE)) {
            return true;
        } else if (annotation != null && !annotation.isShared()) {
            return false;
        } else
            return annotation != null && subscription.getFilter().matches(annotation);
    }

    private ThreadPoolTaskExecutor messageExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();// we can use scheduler also to send notifications in batch
        executor.setThreadNamePrefix("MessageBroker-");
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setQueueCapacity(1000);
        executor.initialize();
        return executor;
    }

    // This is async not to block the main http request
    @SuppressWarnings("PMD.DoNotUseThreads")
    private static class SendTask implements Runnable {

        private final JsonNotification inputMessage;
        private final WebSocketSession messageHandler;

        public SendTask(final JsonNotification message, final WebSocketSession messageHandler) {
            this.inputMessage = message;
            this.messageHandler = messageHandler;
        }

        @SuppressWarnings("PMD.AccessorMethodGeneration")
        @Override
        public void run() {
            try {
                this.messageHandler.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(this.inputMessage), true));
            } catch (Exception ex) {
                LOG.error("Failure while sending message", ex);
            }
        }
    }

    private static class Subscription {
        private final Filter filter;
        private final WebSocketSession subscriber;
        private final UserInformation user;

        public Subscription(final Filter filter, final WebSocketSession subscriber, final UserInformation user) {
            this.filter = filter;
            this.subscriber = subscriber;
            this.user = user;
        }

        public Filter getFilter() {
            return filter;
        }

        public WebSocketSession getSubscriber() {
            return subscriber;
        }

        public UserInformation getUser() {
            return user;
        }
    }

    private static class Message {
        private final ACTION action;
        
        @SuppressWarnings("PMD.ShortVariable")
        private final String id;
        private final String clientId;

        public Message(final ACTION action, final String msgId, final String clientId) {
            this.action = action;
            this.id = msgId;
            this.clientId = clientId;
        }

        public ACTION getAction() {
            return action;
        }

        public String getId() {
            return id;
        }

        public String getClientId() {
            return clientId;
        }
    }
}
