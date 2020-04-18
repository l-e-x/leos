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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.model.UserInformation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class WebsocketRegistryTest {

    private static final String SESSIONNAME = "testSession";
    
    @InjectMocks
    private WebSessionRegistry registry;

    @Before
    public void setUp() {
        // nothing to do
    }

    @Test
    public void testRegister() {
        //setup
        final WebSocketSession mockSession = Mockito.mock(WebSocketSession.class);
        Mockito.when(mockSession.getId()).thenReturn(SESSIONNAME);
        final UserInformation userInformation = Mockito.mock(UserInformation.class);

        //call
        registry.registerSession(mockSession, userInformation);

        //verify
        assertEquals(mockSession, registry.getSession(SESSIONNAME));
    }


    @Test
    public void testUnRegister() {
        //setup
        final WebSocketSession mockSession = Mockito.mock(WebSocketSession.class);
        Mockito.when(mockSession.getId()).thenReturn(SESSIONNAME);
        final UserInformation userInformation = Mockito.mock(UserInformation.class);
        registry.registerSession(mockSession, userInformation);
        assertEquals(mockSession, registry.getSession(SESSIONNAME));

        registry.unregisterSession(SESSIONNAME);

        //verify
        assertNull(registry.getSession(SESSIONNAME));
    }

    @Test
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void testUserInfo() {
        //setup
        final WebSocketSession mockSession = Mockito.mock(WebSocketSession.class);
        Mockito.when(mockSession.getId()).thenReturn(SESSIONNAME);
        final UserInformation userInformation = Mockito.mock(UserInformation.class);
        registry.registerSession(mockSession, userInformation);
        assertEquals(mockSession, registry.getSession(SESSIONNAME));

        //verify
        assertEquals(userInformation, registry.getUserInfo(SESSIONNAME));
    }

}