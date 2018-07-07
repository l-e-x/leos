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

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.JavaScriptConnectorHelper;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.communication.RpcProxy;
import eu.europa.ec.leos.web.shared.js.LeosJavaScriptServerRpc;

import java.util.logging.Logger;

public class LeosJavaScriptConnectorHelper extends JavaScriptConnectorHelper {

    private static Logger LOG = Logger.getLogger(LeosJavaScriptConnectorHelper.class.getName());

    private final LeosJavaScriptServerRpc serverRpc;

    public LeosJavaScriptConnectorHelper(ServerConnector connector) {
        super(connector);
        serverRpc = RpcProxy.create(LeosJavaScriptServerRpc.class, connector);
    }

    @Override
    public void init() {
        super.init();
        LOG.info("Ensuring JavaScript is initialized...");
        super.ensureJavascriptInited();
    }

    @Override
    public JavaScriptObject getConnectorWrapper() {
        JavaScriptObject wrapper = super.getConnectorWrapper();
        LOG.info("Augmenting JavaScript connector wrapper...");
        augmentConnectorWrapper(wrapper, this);
        return wrapper;
    }

    private void jsDepsInited() {
        LOG.info("Calling Server RPC for JavaScript dependencies initialized...");
        serverRpc.clientJSDepsInited();
    }

    private static native void augmentConnectorWrapper(JavaScriptObject obj, LeosJavaScriptConnectorHelper h)
    /*-{
        obj.jsDepsInited = function() {
            $entry(h.@eu.europa.ec.leos.web.client.js.LeosJavaScriptConnectorHelper::jsDepsInited(*)).call(h);
        };
    }-*/;
}