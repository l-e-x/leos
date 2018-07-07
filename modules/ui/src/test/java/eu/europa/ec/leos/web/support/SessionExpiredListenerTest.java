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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.WebApplicationContext;

import eu.europa.ec.leos.services.locking.LockUpdateBroadcastListener;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.test.support.LeosTest;

public class SessionExpiredListenerTest extends LeosTest {

    private SessionExpiredListener sessionExpiredListener = new SessionExpiredListener();

    @Before
    public void init() {
        sessionExpiredListener.autoWiredDelegate.set(null);
    }

    @Test
    public void test_sessionDestroyed() {

        HttpSession httpSession = mock(HttpSession.class);
        LockingService lockingService = mock(LockingService.class);

        SessionExpiredListener spySessionExpiredListener = spy(sessionExpiredListener);

        doReturn(lockingService).when(spySessionExpiredListener).getLockingService(httpSession);
        doNothing().when(spySessionExpiredListener).unlockDocument(httpSession, lockingService);
        doNothing().when(spySessionExpiredListener).unregisterListeners(httpSession, lockingService);

        HttpSessionEvent httpSessionEvent = new HttpSessionEvent(httpSession);
        //DO THE ACTUAL CALL
        spySessionExpiredListener.sessionDestroyed(httpSessionEvent);

        verify(spySessionExpiredListener).sessionDestroyed(httpSessionEvent);
        verify(spySessionExpiredListener).getLockingService(httpSession);
        verify(spySessionExpiredListener).unregisterListeners(httpSession, lockingService);
        verify(spySessionExpiredListener).unlockDocument(httpSession, lockingService);
        verifyNoMoreInteractions(spySessionExpiredListener);
    }

    @Test
    public void test_unlockDocument_should_unlockDocument_when_workingOnDocument() {

        HttpSession httpSession = mock(HttpSession.class);
        LockingService lockingService = mock(LockingService.class);

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn("document_id_1234");
        when(httpSession.getId()).thenReturn("session_id_12345");

        SessionExpiredListener spySessionExpiredListener = spy(sessionExpiredListener);
        doReturn(lockingService).when(spySessionExpiredListener).getLockingService(httpSession);

        // DO THE ACTUAL CALL
        spySessionExpiredListener.unlockDocument(httpSession, lockingService);

        verify(lockingService).releaseLocksForSession("document_id_1234", "session_id_12345");
        verifyNoMoreInteractions(lockingService);
    }

    @Test
    public void test_unlockDocument_should_notTryToUnlock_when_notWorkingOnDocument() {

        HttpSession httpSession = mock(HttpSession.class);
        LockingService lockingService = mock(LockingService.class);

        SessionExpiredListener spySessionExpiredListener = spy(sessionExpiredListener);
        doReturn(lockingService).when(spySessionExpiredListener).getLockingService(httpSession);

        // DO THE ACTUAL CALL
        spySessionExpiredListener.unlockDocument(httpSession, lockingService);

        verify(spySessionExpiredListener).unlockDocument(httpSession, lockingService);
        verifyNoMoreInteractions(spySessionExpiredListener);
    }

    @Test
    public void test_unregisterListeners_should_leaveAll() {
        HttpSession httpSession = mock(HttpSession.class);
        LockingService lockingService = mock(LockingService.class);

        LockUpdateBroadcastListener listener1 = mock(LockUpdateBroadcastListener.class);
        LockUpdateBroadcastListener listener2 = mock(LockUpdateBroadcastListener.class);

        when(httpSession.getAttributeNames()).thenReturn(Collections.enumeration(Arrays.asList("aaa", "bbb", "ccc", "ddd", "eee")));

        when(httpSession.getAttribute("aaa")).thenReturn("aaa");
        when(httpSession.getAttribute("bbb")).thenReturn(listener1);
        when(httpSession.getAttribute("ccc")).thenReturn(111);
        when(httpSession.getAttribute("ddd")).thenReturn(listener2);
        when(httpSession.getAttribute("eee")).thenReturn(Boolean.FALSE);

        SessionExpiredListener spySessionExpiredListener = spy(sessionExpiredListener);
        doReturn(lockingService).when(spySessionExpiredListener).getLockingService(httpSession);

        // DO THE ACTUAL CALL
        spySessionExpiredListener.unregisterListeners(httpSession, lockingService);

        verify(lockingService).unregisterLockInfoBroadcastListener(listener1);
        verify(lockingService).unregisterLockInfoBroadcastListener(listener2);
        verifyNoMoreInteractions(lockingService);
    }

    @Test
    public void test_getLockingService_should_returnExistingValue_when_alreadyInitialized() {

        LockingService lockingService = mock(LockingService.class);
        sessionExpiredListener.autoWiredDelegate.set(lockingService);

        HttpSession httpSession = mock(HttpSession.class);

        // DO THE ACTUAL CALL
        LockingService actualLockingService = sessionExpiredListener.getLockingService(httpSession);

        assertThat(actualLockingService, is(sameInstance(lockingService)));
        verifyZeroInteractions(httpSession);
    }

    @Test
    public void test_getLockingService_should_lookUp_when_notInitialized() {

        LockingService lockingService = mock(LockingService.class);

        HttpSession httpSession = mock(HttpSession.class);
        ServletContext servletContext = mock(ServletContext.class);
        WebApplicationContext webApplicationContext = mock(WebApplicationContext.class);

        when(httpSession.getServletContext()).thenReturn(servletContext);
        when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(webApplicationContext);
        when(webApplicationContext.getBean(LockingService.class)).thenReturn(lockingService);

        // DO THE ACTUAL CALL
        LockingService actualLockingService = sessionExpiredListener.getLockingService(httpSession);

        assertThat(actualLockingService, is(sameInstance(lockingService)));

        verify(webApplicationContext).getBean(LockingService.class);
        verifyNoMoreInteractions(webApplicationContext);
    }
}
