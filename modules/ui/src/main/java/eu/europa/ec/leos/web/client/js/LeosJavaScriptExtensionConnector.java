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
import com.vaadin.client.ServerConnector;
import com.vaadin.client.communication.HasJavaScriptConnectorHelper;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.shared.ui.Connect;
import eu.europa.ec.leos.web.shared.js.LeosJavaScriptExtensionState;
import eu.europa.ec.leos.web.ui.extension.LeosJavaScriptExtension;

@Connect(LeosJavaScriptExtension.class)
public final class LeosJavaScriptExtensionConnector extends AbstractExtensionConnector implements HasJavaScriptConnectorHelper {

    private final JavaScriptConnectorHelper helper = new LeosJavaScriptConnectorHelper(this);

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
    public LeosJavaScriptExtensionState getState() {
        return (LeosJavaScriptExtensionState) super.getState();
    }

    @Override
    public JavaScriptConnectorHelper getJavascriptConnectorHelper() {
        return helper;
    }

    @Override
    protected void extend(ServerConnector target) {
        // Nothing to do for JS extensions.
        // Everything is done in JavaScript.
    }
}