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
package eu.europa.ec.leos.web.ui.extension;

import com.vaadin.server.AbstractJavaScriptExtension;
import eu.europa.ec.leos.web.shared.js.LeosJavaScriptExtensionState;
import eu.europa.ec.leos.web.shared.js.LeosJavaScriptServerRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LeosJavaScriptExtension extends AbstractJavaScriptExtension {

    public LeosJavaScriptExtension() {
        super();
        final Logger log = LoggerFactory.getLogger(this.getClass());
        registerRpc(new LeosJavaScriptServerRpc() {
            @Override
            public void clientJSDepsInited() {
                log.trace("JavaScript extension dependencies are fully initialized.");
                getState().jsDepsInited = true;
            }
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
}