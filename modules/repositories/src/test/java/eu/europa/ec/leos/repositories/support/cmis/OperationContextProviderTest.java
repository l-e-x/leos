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
package eu.europa.ec.leos.repositories.support.cmis;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import eu.europa.ec.leos.test.support.LeosTest;

public class OperationContextProviderTest extends LeosTest {

    @Mock
    private Session session;

    @InjectMocks
    private OperationContextProvider operationContextProvider;

    @Test(expected = NullPointerException.class)
    public void test_getMinimalContext_should_throwNullPointerException_when_contextIsNull() {
        // setup
        OperationContext oc = null;
        when(session.createOperationContext()).thenReturn(oc);

        // exercise
        operationContextProvider.getMinimalContext();
    }

    @Test
    public void test_getMinimalContext_should_returnNonnull() {
        // setup
        OperationContext oc = mock(OperationContext.class);
        when(session.createOperationContext()).thenReturn(oc);

        // exercise
        OperationContext result = operationContextProvider.getMinimalContext();

        // verify
        verify(session).createOperationContext();
        assertThat(result, is(sameInstance(oc)));
    }
}
