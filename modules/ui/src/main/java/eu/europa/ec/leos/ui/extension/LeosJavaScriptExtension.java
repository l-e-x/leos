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
package eu.europa.ec.leos.ui.extension;

import com.vaadin.server.AbstractJavaScriptExtension;
import eu.europa.ec.leos.ui.shared.js.LeosJavaScriptExtensionState;
import eu.europa.ec.leos.ui.shared.js.LeosJavaScriptServerRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LeosJavaScriptExtension extends AbstractJavaScriptExtension {

    private static final long serialVersionUID = 1L;

    protected final Logger LOG;

    protected LeosJavaScriptExtension() {
        super();
        LOG = LoggerFactory.getLogger(this.getClass());
        registerRpc((LeosJavaScriptServerRpc) () -> {
            LOG.trace("JavaScript extension dependencies are fully initialized.");
            getState().jsDepsInited = true;
        });
    }

    @Override
    protected LeosJavaScriptExtensionState getState() {
        return (LeosJavaScriptExtensionState) super.getState();
    }

    @Override
    protected LeosJavaScriptExtensionState getState(boolean markAsDirty) {
        return (LeosJavaScriptExtensionState) super.getState(markAsDirty);
    }

    protected void forceDirty() {
        // NOTE Vaadin 8 only notifies the client side if there are differential state changes
        final long ts = System.currentTimeMillis();
        LOG.trace("Setting dirty timestamp... [ts={}]", ts);
        getState(true).dirtyTimestamp = ts;
    }
}
