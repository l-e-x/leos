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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.annotations.JavaScript;
import com.vaadin.server.ClientConnector;
import com.vaadin.ui.JavaScriptFunction;
import com.vaadin.ui.Label;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.vo.UserVO;
import eu.europa.ec.leos.web.event.view.document.CreateSuggestionRequestEvent;
import eu.europa.ec.leos.web.event.view.document.CreateSuggestionResponseEvent;
import eu.europa.ec.leos.web.event.view.document.SaveSuggestionRequestEvent;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import eu.europa.ec.leos.web.support.LeosCacheToken;

@JavaScript({"vaadin://../js/editor/suggestion/suggestionConnector.js" + LeosCacheToken.TOKEN })
public class SuggestionExtension extends LeosJavaScriptExtension {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(SuggestionExtension.class);
    
    private EventBus eventBus;
    
    public SuggestionExtension(EventBus eventBus) {
        super();
        this.eventBus = eventBus;
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

    
    public void extend(Label target) {
        super.extend(target);
    }
    
    @Subscribe
    public void createSuggestion(CreateSuggestionResponseEvent event) {
        getState(false).user = createUserVO(event.getUser());
        callFunction("initSuggestionEditor", event.getElementId(), event.getElementFragment());
    }

    private void registerServerSideAPI() {
        addFunction("createSuggestion", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                LOG.trace("Create Suggestion...");
                JsonObject data = arguments.get(0);
                String elementId = data.getString("elementId");
                String suggestionId = data.hasKey("suggestionId")? data.getString("suggestionId") : null;
                eventBus.post(new CreateSuggestionRequestEvent(elementId, suggestionId));
            }
        });
        addFunction("saveSuggestion", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                LOG.trace("Saving suggestion...");
                JsonObject data = arguments.get(0);
                String elementId = data.getString("elementId");
                String suggestionId = data.getString("suggestionId");
                String suggestionFragment = data.getString("suggestionFragment");
                eventBus.post(new SaveSuggestionRequestEvent(elementId, suggestionId, suggestionFragment));
            }
        });
    }
    
    @Override
    protected Class<? extends ClientConnector> getSupportedParentType() {
        return Label.class;
    }
    
    @Override
    protected SuggestionState getState() {
        return (SuggestionState) super.getState();
    }

    @Override
    protected SuggestionState getState(boolean markAsDirty) {
        return (SuggestionState) super.getState(markAsDirty);
    }

    private UserVO createUserVO(User user){
        return new UserVO(user);
    }

}