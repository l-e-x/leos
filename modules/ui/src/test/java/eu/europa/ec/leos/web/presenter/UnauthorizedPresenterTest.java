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
package eu.europa.ec.leos.web.presenter;

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.test.support.web.presenter.LeosPresenterTest;
import eu.europa.ec.leos.web.event.view.unauthorized.EnterUnauthorizedViewEvent;
import eu.europa.ec.leos.web.view.UnauthorizedView;

public class UnauthorizedPresenterTest extends LeosPresenterTest {

    @Mock
    private SecurityContext securityContext;

    @Mock
    private UnauthorizedView unauthorizedView;

    @InjectMocks
    private UnauthorizedPresenter unauthorizedPresenter = new UnauthorizedPresenter();

    @Test
    public void test_EnterUnauthorizedView() {
        when(securityContext.getPrincipalName()).thenReturn("testUser");

        //DO THE ACTUAL CALL
        unauthorizedPresenter.enterUnauthorizedView(new EnterUnauthorizedViewEvent());

        verify(unauthorizedView).buildUnauthorizedInfo("testUser");
        verifyNoMoreInteractions(unauthorizedView);
    }
}
