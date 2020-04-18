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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.annotations.JavaScript;
import eu.europa.ec.leos.ui.component.LeosDisplayField;
import eu.europa.ec.leos.ui.event.EnableSyncScrollRequestEvent;
import eu.europa.ec.leos.web.support.LeosCacheToken;

@JavaScript({"vaadin://../js/ui/extension/scrollPaneConnector.js"+ LeosCacheToken.TOKEN})
public class ScrollPaneExtension extends LeosJavaScriptExtension {

    private static final long serialVersionUID = 1L;
    
    private EventBus eventBus;
    
    public ScrollPaneExtension(LeosDisplayField target, EventBus eventBus) {
        super();
        this.eventBus = eventBus;
        extend(target);
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.register(this);
    }
    
    @Override
    public void detach() {
        eventBus.unregister(this);
        super.detach();
    }

    @Override
    public ScrollPaneState getState() {
        return (ScrollPaneState) super.getState();
    }
    
    @Override
    protected ScrollPaneState getState(boolean markAsDirty) {
        return (ScrollPaneState) super.getState(markAsDirty);
    }
    
    @Subscribe
    void scrollPane(EnableSyncScrollRequestEvent event) {
        callFunction("setSyncState", event.isEnable());
    }
}
