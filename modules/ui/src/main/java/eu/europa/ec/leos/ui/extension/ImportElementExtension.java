/*
 * Copyright 2017 European Commission
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
import com.vaadin.ui.JavaScriptFunction;
import com.vaadin.ui.Label;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import eu.europa.ec.leos.web.event.view.document.ImportSelectionChangeEvent;
import eu.europa.ec.leos.web.event.view.document.SelectAllElementRequestEvent;
import eu.europa.ec.leos.web.event.view.document.SelectedElementRequestEvent;
import eu.europa.ec.leos.web.event.view.document.SelectedElementResponseEvent;
import eu.europa.ec.leos.web.support.LeosCacheToken;

import java.util.ArrayList;
import java.util.List;

@JavaScript({"vaadin://../js/ui/extension/importElementConnector.js"+ LeosCacheToken.TOKEN})
public class ImportElementExtension extends LeosJavaScriptExtension {

    private static final long serialVersionUID = 1L;
    
    private EventBus eventBus;
    
    public ImportElementExtension(EventBus eventBus, Label target) {
        super();
        this.eventBus = eventBus;
        extend(target);
        registerServerSideAPI();
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
    
    @Subscribe
    public void getSelectedElements(SelectedElementRequestEvent event) {
        callFunction("requestSelectedElements");
    }
    
    @Subscribe
    public void selectAllElements(SelectAllElementRequestEvent event) {
        callFunction("selectAllElements", event.isValue(), event.getElementName());
    }
    
    private void registerServerSideAPI() {
        addFunction("handleSelectionChange", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                JsonObject data = arguments.get(0);
                eventBus.post(new ImportSelectionChangeEvent((int)data.getNumber("count")));
            }
        });
        
        addFunction("receiveSelectedElements", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                LOG.trace("Received selected elements to import...");
                List<String> elementList = new ArrayList<>();
                JsonObject data = arguments.get(0);
                JsonArray elementIds = data.getArray("elementIds");
                //TODO: May be use Gson to convert array
                for(int index = 0; index < elementIds.length(); index++) {
                    elementList.add(elementIds.getString(index));
                }
                eventBus.post(new SelectedElementResponseEvent(elementList));
            }
        });
    }
}
