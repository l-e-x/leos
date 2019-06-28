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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.websocket.Clause;
import eu.europa.ec.leos.annotate.model.web.websocket.Filter;
import eu.europa.ec.leos.annotate.model.web.websocket.SubscriptionRequest;
import eu.europa.ec.leos.annotate.services.AnnotationConversionService;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.UserService;
import eu.europa.ec.leos.annotate.services.impl.AnnotationConversionServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class MessageBrokerTest {

    @Mock
    private AnnotationService anotService;

    @Mock
    private AnnotationConversionService conversionService;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private MessageBroker messageBroker;

    private final static String ANN1 = "an1";
    private final static String ANN2 = "an2";
    private final static String ONEOF = "one_of";
    private final static String INCL_ANY = "include_any";
    private final static String URI = "/uri";
    private final static String TESTURI = "test-uri";
    private final static String CREATE = "create";
    private final static String TITLE = "title";
    
    @Before
    public void setUp() {
        // nothing to do
    }

    @Test
    public void testBroadcast_Do_Not_send_notification_to_same_client() throws Exception {
        // setup
        final SubscriptionRequest subscriptionRequest1 = Mockito.mock(SubscriptionRequest.class);
        final Clause clause1 = new Clause(URI, ONEOF, Collections.singleton(TESTURI), false, Collections.emptyList());
        final Filter filter1 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause1));
        Mockito.when(subscriptionRequest1.getFilter()).thenReturn(filter1);

        final SubscriptionRequest subscriptionRequest2 = Mockito.mock(SubscriptionRequest.class);
        final Clause clause2 = new Clause(URI, ONEOF, Collections.singleton(TESTURI), false, Collections.emptyList());
        final Filter filter2 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause2));
        Mockito.when(subscriptionRequest2.getFilter()).thenReturn(filter2);

        final WebSocketSession subscriber = Mockito.mock(WebSocketSession.class);
        Mockito.when(subscriber.getId()).thenReturn("s1");
        final UserInformation userInformation = Mockito.mock(UserInformation.class);
        Mockito.when(userInformation.getClientId()).thenReturn("x1");
        final WebSocketSession subscriber2 = Mockito.mock(WebSocketSession.class);
        Mockito.when(subscriber2.getId()).thenReturn("s2");
        final UserInformation userInformation2 = Mockito.mock(UserInformation.class);
        Mockito.when(userInformation2.getClientId()).thenReturn("x2");

        final Annotation annotation1 = Mockito.mock(Annotation.class);
        final Document doc = new Document(new URI(TESTURI), TITLE);
        Mockito.when(annotation1.getDocument()).thenReturn(doc);
        Mockito.when(annotation1.isShared()).thenReturn(true);
        Mockito.when(anotService.findAnnotationById(ANN1)).thenReturn(annotation1);
        final JsonAnnotation jsonAnnotation1 = new JsonAnnotation();
        jsonAnnotation1.setId(ANN1);
        Mockito.when(conversionService.convertToJsonAnnotation(Mockito.eq(annotation1), Mockito.any(UserInformation.class))).thenReturn(jsonAnnotation1);

        final Annotation annotation2 = Mockito.mock(Annotation.class);
        Mockito.when(annotation2.getDocument()).thenReturn(doc);// both are subscribed to same doc
        Mockito.when(annotation2.isShared()).thenReturn(true);
        Mockito.when(anotService.findAnnotationById(ANN2)).thenReturn(annotation2);
        final JsonAnnotation jsonAnnotation2 = new JsonAnnotation();
        jsonAnnotation2.setId(ANN2);
        Mockito.when(conversionService.convertToJsonAnnotation(Mockito.eq(annotation2), Mockito.any(UserInformation.class))).thenReturn(jsonAnnotation2);

        messageBroker.subscribe(subscriptionRequest1, subscriber, userInformation);
        messageBroker.subscribe(subscriptionRequest2, subscriber2, userInformation2);
        messageBroker.publish(ANN1, MessageBroker.ACTION.CREATE, "x1");
        messageBroker.publish(ANN2, MessageBroker.ACTION.CREATE, "x2");

        // call
        messageBroker.updateSubscribers();
        Thread.sleep(1000);

        // verify
        final ArgumentCaptor<TextMessage> argument = ArgumentCaptor.forClass(TextMessage.class);
        Mockito.verify(subscriber).sendMessage(argument.capture());
        assertEquals(ANN2, getId(argument.getValue()));

        final ArgumentCaptor<TextMessage> argument2 = ArgumentCaptor.forClass(TextMessage.class);
        Mockito.verify(subscriber2).sendMessage(argument2.capture());
        assertEquals(ANN1, getId(argument2.getValue()));
    }

    @Test
    public void testBroadcast_Annotation_is_anonymised() throws Exception {
        // setup
        final SubscriptionRequest subscriptionRequest1 = Mockito.mock(SubscriptionRequest.class);
        final Clause clause1 = new Clause(URI, ONEOF, Collections.singleton(TESTURI), false, Collections.emptyList());
        final Filter filter1 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause1));
        Mockito.when(subscriptionRequest1.getFilter()).thenReturn(filter1);

        final SubscriptionRequest subscriptionRequest2 = Mockito.mock(SubscriptionRequest.class);
        final Clause clause2 = new Clause(URI, ONEOF, Collections.singleton(TESTURI), false, Collections.emptyList());
        final Filter filter2 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause2));
        Mockito.when(subscriptionRequest2.getFilter()).thenReturn(filter2);

        final WebSocketSession subscriber = Mockito.mock(WebSocketSession.class);
        Mockito.when(subscriber.getId()).thenReturn("s1");
        final UserInformation userInformation = new UserInformation("login", Authorities.EdiT); // subscribing user is EdiT user
        userInformation.setClientId("x1");
        final WebSocketSession subscriber2 = Mockito.mock(WebSocketSession.class);
        Mockito.when(subscriber2.getId()).thenReturn("s2");
        final UserInformation userInformation2 = Mockito.mock(UserInformation.class);
        Mockito.when(userInformation2.getClientId()).thenReturn("x2");

        // create an ISC annotation
        final Annotation annotation1 = Mockito.mock(Annotation.class);
        final Document doc = new Document(new URI(TESTURI), TITLE);
        final Group grp = new Group("group", true);
        final Metadata meta = new Metadata(doc, grp, Authorities.ISC);
        final User user = new User("me");
        meta.setResponseStatus(Metadata.ResponseStatus.SENT);
        Mockito.when(annotation1.getDocument()).thenReturn(doc);
        Mockito.when(annotation1.getMetadata()).thenReturn(meta);
        Mockito.when(annotation1.getGroup()).thenReturn(grp);
        Mockito.when(annotation1.isShared()).thenReturn(true);
        Mockito.when(annotation1.isResponseStatusSent()).thenReturn(true); // status SENT
        Mockito.when(annotation1.getTargetSelectors()).thenReturn("[{\"selector\":null,\"source\":\"http://dummy\"}]");
        Mockito.when(annotation1.getUser()).thenReturn(user);
        Mockito.when(annotation1.getId()).thenReturn(ANN1);
        
        Mockito.when(anotService.findAnnotationById(ANN1)).thenReturn(annotation1);
        
        Mockito.when(userService.getHypothesisUserAccountFromUser(Mockito.any(User.class), Mockito.anyString())).thenReturn("acct:user@" + Authorities.EdiT);
        
        // let the published annotation be created - should be anonymised (LEOS user is seeing SENT ISC annotation) 
        final AnnotationConversionService realConvService = new AnnotationConversionServiceImpl(userService);
        final JsonAnnotation jsAnnot = realConvService.convertToJsonAnnotation(annotation1, userInformation);
        Mockito.when(conversionService.convertToJsonAnnotation(Mockito.any(Annotation.class), Mockito.any(UserInformation.class))).thenReturn(jsAnnot);

        messageBroker.subscribe(subscriptionRequest1, subscriber, userInformation);
        messageBroker.subscribe(subscriptionRequest2, subscriber2, userInformation2);
        messageBroker.publish(ANN1, MessageBroker.ACTION.CREATE, "x1");

        // call
        messageBroker.updateSubscribers();
        Thread.sleep(1000);

        // verify that sent annotation was the anonymised one
        final ArgumentCaptor<TextMessage> argument2 = ArgumentCaptor.forClass(TextMessage.class);
        Mockito.verify(subscriber2).sendMessage(argument2.capture()); // note: might not work when debugging this test...!?
        final Map<String, String> userInfoSent = getUserInfo(argument2.getValue());
        assertEquals("unknown ISC reference", userInfoSent.get("display_name"));
        assertEquals("unknown", userInfoSent.get("entity_name"));
    }
    
    @Test
    public void testBroadcast_Do_Not_send_notification_if_private() throws Exception {
        // setup
        final SubscriptionRequest subscriptionRequest1 = Mockito.mock(SubscriptionRequest.class);
        final Clause clause1 = new Clause(URI, ONEOF, Collections.singleton(TESTURI), false, Collections.emptyList());
        final Filter filter1 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause1));
        Mockito.when(subscriptionRequest1.getFilter()).thenReturn(filter1);

        final SubscriptionRequest subscriptionRequest2 = Mockito.mock(SubscriptionRequest.class);
        final Clause clause2 = new Clause(URI, ONEOF, Collections.singleton(TESTURI), false, Collections.emptyList());
        final Filter filter2 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause2));
        Mockito.when(subscriptionRequest2.getFilter()).thenReturn(filter2);

        final WebSocketSession subscriber = Mockito.mock(WebSocketSession.class);
        Mockito.when(subscriber.getId()).thenReturn("s1");
        final UserInformation userInformation = Mockito.mock(UserInformation.class);
        Mockito.when(userInformation.getClientId()).thenReturn("x1");
        final WebSocketSession subscriber2 = Mockito.mock(WebSocketSession.class);
        Mockito.when(subscriber2.getId()).thenReturn("s2");
        final UserInformation userInformation2 = Mockito.mock(UserInformation.class);
        Mockito.when(userInformation2.getClientId()).thenReturn("x2");

        final Annotation annotation1 = Mockito.mock(Annotation.class);
        final Document doc = new Document(new URI(TESTURI), TITLE);
        Mockito.when(annotation1.getDocument()).thenReturn(doc);
        Mockito.when(annotation1.isShared()).thenReturn(false);
        Mockito.when(anotService.findAnnotationById(ANN1)).thenReturn(annotation1);
        final JsonAnnotation jsonAnnotation1 = new JsonAnnotation();
        jsonAnnotation1.setId(ANN1);
        Mockito.when(conversionService.convertToJsonAnnotation(Mockito.eq(annotation1), Mockito.any(UserInformation.class))).thenReturn(jsonAnnotation1);

        final Annotation annotation2 = Mockito.mock(Annotation.class);
        Mockito.when(annotation2.getDocument()).thenReturn(doc);// both are subscribed to same doc
        Mockito.when(anotService.findAnnotationById(ANN2)).thenReturn(annotation2);
        final JsonAnnotation jsonAnnotation2 = new JsonAnnotation();
        jsonAnnotation2.setId(ANN2);
        Mockito.when(conversionService.convertToJsonAnnotation(Mockito.eq(annotation2), Mockito.any(UserInformation.class))).thenReturn(jsonAnnotation2);

        messageBroker.subscribe(subscriptionRequest1, subscriber, userInformation);
        messageBroker.subscribe(subscriptionRequest2, subscriber2, userInformation2);

        messageBroker.publish(ANN1, MessageBroker.ACTION.CREATE, "x1");

        // call
        messageBroker.updateSubscribers();
        Thread.sleep(1000);

        // verify
        Mockito.verify(subscriber, Mockito.never()).sendMessage(Mockito.any());
        Mockito.verify(subscriber2, Mockito.never()).sendMessage(Mockito.any());
    }

    @Test
    public void testBroadcast_send_notification_to_all_if_delete() throws Exception {
        // setup
        final SubscriptionRequest subscriptionRequest1 = Mockito.mock(SubscriptionRequest.class);
        final Clause clause1 = new Clause(URI, ONEOF, Collections.singleton(TESTURI), false, Collections.emptyList());
        final Filter filter1 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause1));
        Mockito.when(subscriptionRequest1.getFilter()).thenReturn(filter1);

        final SubscriptionRequest subscriptionRequest2 = Mockito.mock(SubscriptionRequest.class);
        final Clause clause2 = new Clause(URI, ONEOF, Collections.singleton(TESTURI), false, Collections.emptyList());
        final Filter filter2 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause2));
        Mockito.when(subscriptionRequest2.getFilter()).thenReturn(filter2);

        final WebSocketSession subscriber = Mockito.mock(WebSocketSession.class);
        Mockito.when(subscriber.getId()).thenReturn("s1");
        final UserInformation userInformation = Mockito.mock(UserInformation.class);
        Mockito.when(userInformation.getClientId()).thenReturn("x1");
        final WebSocketSession subscriber2 = Mockito.mock(WebSocketSession.class);
        Mockito.when(subscriber2.getId()).thenReturn("s2");
        final UserInformation userInformation2 = Mockito.mock(UserInformation.class);
        Mockito.when(userInformation2.getClientId()).thenReturn("x2");

        final Annotation annotation1 = Mockito.mock(Annotation.class);
        final Document doc = new Document(new URI(TESTURI), TITLE);
        Mockito.when(annotation1.getDocument()).thenReturn(doc);
        Mockito.when(anotService.findAnnotationById(ANN1)).thenReturn(annotation1);
        final JsonAnnotation jsonAnnotation1 = new JsonAnnotation();
        jsonAnnotation1.setId(ANN1);
        Mockito.when(conversionService.convertToJsonAnnotation(Mockito.eq(annotation1), Mockito.any(UserInformation.class))).thenReturn(jsonAnnotation1);

        final Annotation annotation2 = Mockito.mock(Annotation.class);
        Mockito.when(annotation2.getDocument()).thenReturn(doc);// both are subscribed to same doc
        Mockito.when(anotService.findAnnotationById(ANN2)).thenReturn(annotation2);
        final JsonAnnotation jsonAnnotation2 = new JsonAnnotation();
        jsonAnnotation2.setId(ANN2);
        Mockito.when(conversionService.convertToJsonAnnotation(Mockito.eq(annotation2), Mockito.any(UserInformation.class))).thenReturn(jsonAnnotation2);

        messageBroker.subscribe(subscriptionRequest1, subscriber, userInformation);
        messageBroker.subscribe(subscriptionRequest2, subscriber2, userInformation2);
        messageBroker.publish(ANN1, MessageBroker.ACTION.DELETE, "x1");
        messageBroker.publish(ANN2, MessageBroker.ACTION.DELETE, "x2");

        // call
        messageBroker.updateSubscribers();
        Thread.sleep(1000);

        // verify
        final ArgumentCaptor<TextMessage> argument = ArgumentCaptor.forClass(TextMessage.class);
        Mockito.verify(subscriber, Mockito.times(1)).sendMessage(argument.capture());
        assertEquals(ANN2, getId(argument.getValue()));

        final ArgumentCaptor<TextMessage> argument2 = ArgumentCaptor.forClass(TextMessage.class);
        Mockito.verify(subscriber2, Mockito.times(1)).sendMessage(argument2.capture());
        assertEquals(ANN1, getId(argument2.getValue()));
    }

    @Test
    public void testBroadcast_No_notification_if_different_urls() throws Exception {
        // setup
        final String user1_doc = "test-uri1";
        final String user2_doc = "test-uri2";

        final SubscriptionRequest subscriptionRequest1 = Mockito.mock(SubscriptionRequest.class);
        final Clause clause1 = new Clause(URI, ONEOF, Collections.singleton(user1_doc), false, Collections.emptyList());
        final Filter filter1 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause1));
        Mockito.when(subscriptionRequest1.getFilter()).thenReturn(filter1);

        final SubscriptionRequest subscriptionRequest2 = Mockito.mock(SubscriptionRequest.class);
        final Clause clause2 = new Clause(URI, ONEOF, Collections.singleton(user2_doc), false, Collections.emptyList());
        final Filter filter2 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause2));
        Mockito.when(subscriptionRequest2.getFilter()).thenReturn(filter2);

        final WebSocketSession subscriber = Mockito.mock(WebSocketSession.class);
        Mockito.when(subscriber.getId()).thenReturn("s1");
        final UserInformation userInformation = Mockito.mock(UserInformation.class);
        Mockito.when(userInformation.getClientId()).thenReturn("x1");
        final WebSocketSession subscriber2 = Mockito.mock(WebSocketSession.class);
        Mockito.when(subscriber2.getId()).thenReturn("s2");
        final UserInformation userInformation2 = Mockito.mock(UserInformation.class);
        Mockito.when(userInformation2.getClientId()).thenReturn("x2");

        final Annotation annotation1 = Mockito.mock(Annotation.class);
        Mockito.when(annotation1.getDocument()).thenReturn(new Document(new URI(user1_doc), TITLE));
        Mockito.when(anotService.findAnnotationById(ANN1)).thenReturn(annotation1);
        final JsonAnnotation jsonAnnotation1 = new JsonAnnotation();
        jsonAnnotation1.setId(ANN1);
        Mockito.when(conversionService.convertToJsonAnnotation(Mockito.eq(annotation1), Mockito.any(UserInformation.class))).thenReturn(jsonAnnotation1); 

        final Annotation annotation2 = Mockito.mock(Annotation.class);
        Mockito.when(annotation2.getDocument()).thenReturn(new Document(new URI(user2_doc), "title2"));// both are working on diff docs
        Mockito.when(anotService.findAnnotationById(ANN2)).thenReturn(annotation2);
        final JsonAnnotation jsonAnnotation2 = new JsonAnnotation();
        jsonAnnotation2.setId(ANN2);
        Mockito.when(conversionService.convertToJsonAnnotation(Mockito.eq(annotation2), Mockito.any(UserInformation.class))).thenReturn(jsonAnnotation2);

        messageBroker.subscribe(subscriptionRequest1, subscriber, userInformation);
        messageBroker.subscribe(subscriptionRequest2, subscriber2, userInformation2);
        messageBroker.publish(ANN1, MessageBroker.ACTION.CREATE, "x1");
        messageBroker.publish(ANN2, MessageBroker.ACTION.CREATE, "x2");

        // call
        messageBroker.updateSubscribers();
        Thread.sleep(1000);

        // verify
        Mockito.verifyNoMoreInteractions(subscriber);
        Mockito.verifyNoMoreInteractions(subscriber2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_Bad_Subscription() {
        // setup
        final SubscriptionRequest subscriptionRequest1 = Mockito.mock(SubscriptionRequest.class);
        final Clause clause1 = new Clause("/urx", ONEOF, Collections.singleton(TESTURI), false, Collections.emptyList());
        final Filter filter1 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause1));
        Mockito.when(subscriptionRequest1.getFilter()).thenReturn(filter1);

        final WebSocketSession subscriber = Mockito.mock(WebSocketSession.class);
        Mockito.when(subscriber.getId()).thenReturn("s1");
        final UserInformation userInformation = Mockito.mock(UserInformation.class);
        Mockito.when(userInformation.getClientId()).thenReturn("x1");

        final Annotation annotation1 = Mockito.mock(Annotation.class);
        Mockito.when(anotService.findAnnotationById(ANN1)).thenReturn(annotation1);
        final JsonAnnotation jsonAnnotation1 = new JsonAnnotation();
        jsonAnnotation1.setId(ANN1);
        Mockito.when(conversionService.convertToJsonAnnotation(Mockito.eq(annotation1), Mockito.any(UserInformation.class))).thenReturn(jsonAnnotation1);

        messageBroker.subscribe(subscriptionRequest1, subscriber, userInformation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_Bad_Subscription_operator() {
        // setup
        final SubscriptionRequest subscriptionRequest1 = Mockito.mock(SubscriptionRequest.class);
        final Clause clause1 = new Clause(URI, "any_of", Collections.singleton(TESTURI), false, Collections.emptyList());
        final Filter filter1 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause1));
        Mockito.when(subscriptionRequest1.getFilter()).thenReturn(filter1);

        final WebSocketSession subscriber = Mockito.mock(WebSocketSession.class);
        final UserInformation userInformation = Mockito.mock(UserInformation.class);

        messageBroker.subscribe(subscriptionRequest1, subscriber, userInformation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_Bad_Subscription_value_null() {
        // setup
        final SubscriptionRequest subscriptionRequest1 = Mockito.mock(SubscriptionRequest.class);
        final Clause clause1 = new Clause(URI, ONEOF, Collections.emptySet(), false, Collections.emptyList());
        final Filter filter1 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause1));
        Mockito.when(subscriptionRequest1.getFilter()).thenReturn(filter1);

        final WebSocketSession subscriber = Mockito.mock(WebSocketSession.class);
        final UserInformation userInformation = Mockito.mock(UserInformation.class);

        messageBroker.subscribe(subscriptionRequest1, subscriber, userInformation);
    }

    @Test
    public void testBroadcastUnsubscribe() throws Exception {
        // setup
        final SubscriptionRequest subscriptionRequest = Mockito.mock(SubscriptionRequest.class);
        final Clause clause1 = new Clause(URI, ONEOF, Collections.singleton(TESTURI), false, Collections.emptyList());
        final Filter filter1 = new Filter(INCL_ANY, Collections.singletonMap(CREATE, Boolean.TRUE), Collections.singletonList(clause1));
        Mockito.when(subscriptionRequest.getFilter()).thenReturn(filter1);

        final WebSocketSession subscriber = Mockito.mock(WebSocketSession.class);
        Mockito.when(subscriber.getId()).thenReturn("s1");
        final UserInformation userInformation = Mockito.mock(UserInformation.class);
        Mockito.when(userInformation.getClientId()).thenReturn("x1");
        final WebSocketSession subscriber2 = Mockito.mock(WebSocketSession.class);
        Mockito.when(subscriber2.getId()).thenReturn("s2");
        final UserInformation userInformation2 = Mockito.mock(UserInformation.class);
        Mockito.when(userInformation2.getClientId()).thenReturn("x2");

        final Annotation annotation1 = Mockito.mock(Annotation.class);
        final Document doc = new Document(new URI(TESTURI), TITLE);
        Mockito.when(annotation1.getDocument()).thenReturn(doc);
        Mockito.when(annotation1.isShared()).thenReturn(true);
        Mockito.when(anotService.findAnnotationById(ANN1)).thenReturn(annotation1);
        final JsonAnnotation jsonAnnotation1 = new JsonAnnotation();
        jsonAnnotation1.setId(ANN1);
        Mockito.when(conversionService.convertToJsonAnnotation(Mockito.eq(annotation1), Mockito.any(UserInformation.class))).thenReturn(jsonAnnotation1);

        final Annotation annotation2 = Mockito.mock(Annotation.class);
        Mockito.when(annotation2.getDocument()).thenReturn(doc);
        Mockito.when(annotation2.isShared()).thenReturn(true);
        Mockito.when(anotService.findAnnotationById(ANN2)).thenReturn(annotation2);
        final JsonAnnotation jsonAnnotation2 = new JsonAnnotation();
        jsonAnnotation2.setId(ANN2);
        Mockito.when(conversionService.convertToJsonAnnotation(Mockito.eq(annotation2), Mockito.any(UserInformation.class))).thenReturn(jsonAnnotation2);

        messageBroker.subscribe(subscriptionRequest, subscriber, userInformation);
        messageBroker.subscribe(subscriptionRequest, subscriber2, userInformation2);
        messageBroker.publish(ANN1, MessageBroker.ACTION.CREATE, "x1");
        messageBroker.publish(ANN2, MessageBroker.ACTION.CREATE, "x2");

        // call
        messageBroker.updateSubscribers();
        Thread.sleep(1000);
        messageBroker.unsubscribe("s1");
        messageBroker.publish(ANN2, MessageBroker.ACTION.UPDATE, "x2");

        // verify
        Mockito.verify(subscriber, Mockito.times(1)).sendMessage(Mockito.any(TextMessage.class));
        Mockito.verify(subscriber2, Mockito.times(1)).sendMessage(Mockito.any(TextMessage.class));
    }

    @SuppressWarnings("unchecked")
    private String getId(final TextMessage textMessage) throws JsonParseException, JsonMappingException, IOException {
        final Map<String, Object> message = (new ObjectMapper()).readValue(textMessage.getPayload(), Map.class);
        final Map<String, Object> annotation = (HashMap<String, Object>) ((ArrayList<Map<String, Object>>) (message.get("payload"))).get(0);
        return (String) annotation.get("id");
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, String> getUserInfo(final TextMessage textMessage) throws JsonParseException, JsonMappingException, IOException {
        final Map<String, Object> message = (new ObjectMapper()).readValue(textMessage.getPayload(), Map.class);
        final Map<String, Object> annotation = (HashMap<String, Object>) ((ArrayList<Map<String, Object>>) (message.get("payload"))).get(0);
        return (Map<String, String>) annotation.get("user_info");
    }
}