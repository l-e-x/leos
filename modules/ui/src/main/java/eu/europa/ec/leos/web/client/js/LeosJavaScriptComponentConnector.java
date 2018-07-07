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
package eu.europa.ec.leos.web.client.js;

import com.vaadin.client.JavaScriptConnectorHelper;
import com.vaadin.client.communication.HasJavaScriptConnectorHelper;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.client.ui.JavaScriptWidget;
import com.vaadin.shared.ui.Connect;
import eu.europa.ec.leos.web.shared.js.LeosJavaScriptComponentState;
import eu.europa.ec.leos.web.ui.component.LeosJavaScriptComponent;

import java.util.ArrayList;

@Connect(LeosJavaScriptComponent.class)
public final class LeosJavaScriptComponentConnector extends AbstractComponentConnector implements HasJavaScriptConnectorHelper {

    private final JavaScriptConnectorHelper helper = new LeosJavaScriptConnectorHelper(this) {
        @Override
        protected void showInitProblem(ArrayList<String> attemptedNames) {
            getWidget().showNoInitFound(attemptedNames);
        }
    };

    @Override
    protected void init() {
        super.init();
        helper.init();
    }

    @Override
    public void onUnregister() {
        super.onUnregister();
        helper.onUnregister();
    }

    @Override
    public LeosJavaScriptComponentState getState() {
        return (LeosJavaScriptComponentState) super.getState();
    }

    @Override
    public JavaScriptConnectorHelper getJavascriptConnectorHelper() {
        return helper;
    }

    @Override
    public JavaScriptWidget getWidget() {
        return (JavaScriptWidget) super.getWidget();
    }
}