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

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.InjectMocks;

import eu.europa.ec.leos.test.support.web.presenter.LeosPresenterTest;
import eu.europa.ec.leos.web.event.view.logout.EnterLogoutViewEvent;

public class LogoutPresenterTest extends LeosPresenterTest {

    @InjectMocks
    private LogoutPresenter logoutPresenter = new LogoutPresenter();

    @Test
    public void testEnterLogoutView() {

        //DO THE ACTUAL CALL
        logoutPresenter.enterLogoutView(new EnterLogoutViewEvent());
        verify(httpSession).invalidate();
    }
}
