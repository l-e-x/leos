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
package eu.europa.ec.leos.web.ui.component;

import com.vaadin.ui.AbstractJavaScriptComponent;
import eu.europa.ec.leos.web.shared.js.LeosJavaScriptComponentState;
import eu.europa.ec.leos.web.shared.js.LeosJavaScriptServerRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LeosJavaScriptComponent extends AbstractJavaScriptComponent {

    public LeosJavaScriptComponent() {
        super();
        final Logger log = LoggerFactory.getLogger(this.getClass());
        registerRpc(new LeosJavaScriptServerRpc() {
            @Override
            public void clientJSDepsInited() {
                log.trace("JavaScript component dependencies are fully initialized.");
                getState().jsDepsInited = true;
            }
        });
    }

    @Override
    protected LeosJavaScriptComponentState getState() {
        return (LeosJavaScriptComponentState) super.getState();
    }

    @Override
    protected LeosJavaScriptComponentState getState(boolean markAsDirty) {
        return (LeosJavaScriptComponentState) super.getState(markAsDirty);
    }
}