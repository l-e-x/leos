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
package eu.europa.ec.leos.web.ui.navigation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import com.google.common.eventbus.EventBus;
import com.vaadin.spring.navigator.SpringNavigator;
import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class NavigationManagerTest extends LeosTest {
    @Mock
    private EventBus eventBus;

    @Mock
    private SpringNavigator navigator;

    @InjectMocks
    private NavigationManager navigationManager;

    @Test
    public void test_homeView_navigationRequest() {

        // Actual call
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.HOME));

        // verify
        verify(navigator).navigateTo(Target.HOME.getViewId());
    }

    @Test
    public void test_viewWithSingleParams_navigationRequest() {
        String parameter1 = "param1";

        // Actual call
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.LEGALTEXT, parameter1));

        // verify
        verify(navigator).navigateTo(Target.LEGALTEXT.getViewId() + "/" + parameter1);
    }

    @Test
    public void test_viewWithMultipleParams_navigationRequest() {
        String parameter1 = "param1";
        String parameter2 = "param2";

        // Actual call
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.LEGALTEXT, parameter1, parameter2));

        // verify
        verify(navigator).navigateTo(Target.LEGALTEXT.getViewId() + "/" + parameter1 + "/" + parameter2);
    }

    @Test
    public void test_viewWithEmptyParam_navigationRequest() {
        String parameter1 = "";
        String parameter2 = "";
        String parameter3 = "test";

        // Actual call
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.LEGALTEXT, parameter1, parameter2, parameter3));

        // verify
        verify(navigator).navigateTo(Target.LEGALTEXT.getViewId() + "///" + parameter3);// two blanks followed by actual param. No trim
    }

    @Test
    public void test_viewWithNullParam_navigationRequest() {
        String parameter1 = "param1";

        // Actual call
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.LEGALTEXT, null, parameter1, null));

        // verify
        verify(navigator).navigateTo(Target.LEGALTEXT.getViewId() + "//" + parameter1 + "/");// No trim
    }

    @Test
    public void test_viewWithMultipleNullParamInEnd_navigationRequest() {
        String parameter1 = "param1";

        // Actual call
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.LEGALTEXT, parameter1, null, null));

        // verify
        verify(navigator).navigateTo(Target.LEGALTEXT.getViewId() + "/" + parameter1 + "//");// No trim
    }

    @Test
    public void test_previousView_navigationRequest() {
        // setup
        String parameter1 = "param1";
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.LEGALTEXT, parameter1));

        // Actual call
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.PREVIOUS));

        // verify
        verify(navigator).navigateTo(Target.LEGALTEXT.getViewId() + "/" + parameter1);
    }

    @Test
    public void test_previousTwoViewInStack_navigationRequest() {
        // setup some random calls
        String parameter1 = "param1";
        String parameter2 = "param2";
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.LEGALTEXT, parameter1));
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.PROPOSAL, parameter2));// previous view
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.HOME));// current view
        clearInvocations(navigator);//testing the stateful scenario so clear is needed

        // Actual call
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.PREVIOUS)); // go back should take us to proposal view

        // verify
        verify(navigator).navigateTo(Target.PROPOSAL.getViewId() + "/" + parameter2);
    }

    @Test
    public void test_previousMultipleViewInStack_navigationRequest() {
        // setup some random calls
        String parameter1 = "param1";
        String parameter2 = "param2";
        String parameter3 = "param3";
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.HOME));
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.PROPOSAL,  parameter1));
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.LEGALTEXT,  parameter2));
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.PREVIOUS));
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.PROPOSAL,  parameter3)); //current
        clearInvocations(navigator);        //testing the stateful scenario so clear is needed

        // Actual call
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.PREVIOUS)); // go back should take us to proposal view

        // verify
        verify(navigator).navigateTo(Target.HOME.getViewId());
    }

    @Test
    public void test_NavigateToSameViewInStack_navigationRequest() {
        // setup some random calls
        String parameter1 = "param1";
        String parameter2 = "param2";
        String parameter3 = "param3";
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.HOME));
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.PROPOSAL,  parameter1));
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.LEGALTEXT,  parameter2));
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.PREVIOUS));

        clearInvocations(navigator);        //testing the stateful scenario so clear is needed

        // Actual call - navigating to same view with different parameter
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.PROPOSAL,  parameter3)); //current

        // verify
        verify(navigator).navigateTo(Target.PROPOSAL.getViewId() + "/" + parameter3);
    }

    
    @Test
    public void test_previousWithNoViewInStack_navigationRequest() { //should take to home
        // Actual call
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.PREVIOUS));

        // verify
        verify(navigator).navigateTo(Target.HOME.getViewId());
    }

    @Test
    public void test_callToMultiplePrevious_navigationRequest() { //should take to home
        // setup some random calls
        String parameter1 = "param1";
        String parameter2 = "param2";
        String parameter3 = "param3";
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.LEGALTEXT, parameter1));
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.PREVIOUS));
        clearInvocations(navigator);//testing the stateful scenario so clear is needed

        // Actual call
        navigationManager.navigationRequest(new NavigationRequestEvent(Target.PREVIOUS)); // go back should take us to proposal view

        // verify
        verify(navigator).navigateTo(Target.HOME.getViewId());
    }
}
