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
import com.vaadin.server.JsonCodec;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.JavaScriptFunction;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import eu.europa.ec.leos.web.model.UserVO;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import org.apache.commons.lang3.StringUtils;

@JavaScript({"vaadin://../js/editor/leosEditorConnector.js" + LeosCacheToken.TOKEN })
public class LeosEditorExtension<T extends AbstractComponent> extends LeosJavaScriptExtension {

    private EventBus eventBus;

    public LeosEditorExtension(T target, EventBus eventBus) {
        super();
        this.eventBus = eventBus;
        registerServerSideAPI();
        extend(target);
    }

    protected void extend(T target) {
        super.extend(target);
        target.addStyleName("leos-editing-pane");
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
    public void editElement(EditElementResponseEvent event) {
        LOG.trace("Editing element...");
        getState(false).user = new UserVO(event.getUser());
        getState(false).roles = event.getRoles();
        callFunction("editElement", event.getElementId(), event.getElementTagName(), event.getElementFragment(), event.getDocType());
    }

    @Subscribe
    public void refreshElement(RefreshElementEvent event) {
        LOG.trace("Refreshing element...");
        callFunction("refreshElement",  event.getElementId(), event.getElementTagName(), event.getElementFragment());
    }

    @Subscribe
    public void receiveElement(FetchElementResponseEvent event) {
        LOG.trace("Receiving element...");
        callFunction("receiveElement",  event.getElementId(), event.getElementTagName(), event.getElementFragment());
    }

    @Subscribe
    public void receiveToc(FetchCrossRefTocResponseEvent event) {
        LOG.trace("Receiving table of content...");
        callFunction("receiveToc", convertToJson(event.getTocAndAncestorsVO()));
    }

    @Override
    protected LeosEditorState getState() {
        return (LeosEditorState) super.getState();
    }

    @Override
    protected LeosEditorState getState(boolean markAsDirty) {
        return (LeosEditorState) super.getState(markAsDirty);
    }

    private void registerServerSideAPI() {
        addFunction("insertElementAction", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                LOG.trace("Insert element action...");
                JsonObject data = arguments.get(0);
                String elementId = data.getString("elementId");
                String elementType = data.getString("elementType");
                String position = data.getString("position");
                try {
                    eventBus.post(
                            new InsertElementRequestEvent(elementId, elementType,
                                    InsertElementRequestEvent.POSITION.valueOf(StringUtils.upperCase(position))));
                } catch (Exception ex) {
                    LOG.error("Exception when inserting element!", ex);
                }
            }
        });
        addFunction("editElementAction", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                LOG.trace("Edit element action...");
                JsonObject data = arguments.get(0);
                String elementId = data.getString("elementId");
                String elementType = data.getString("elementType");
                eventBus.post(new EditElementRequestEvent(elementId, elementType));
            }
        });
        addFunction("deleteElementAction", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                LOG.trace("Delete element action...");
                JsonObject data = arguments.get(0);
                String elementId = data.getString("elementId");
                String elementType = data.getString("elementType");
                eventBus.post(new DeleteElementRequestEvent(elementId, elementType));
            }
        });
        addFunction("releaseElement", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                LOG.trace("Releasing element...");
                JsonObject data = arguments.get(0);
                String elementId = data.getString("elementId");
                eventBus.post(new CloseElementEditorEvent(elementId));
            }
        });
        addFunction("saveElement", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                LOG.trace("Saving element...");
                JsonObject data = arguments.get(0);
                String elementId = data.getString("elementId");
                String elementType = data.getString("elementType");
                String elementFragment = data.getString("elementFragment");
                eventBus.post(new SaveElementRequestEvent(elementId, elementType, elementFragment));
            }
        });
        addFunction("requestElement", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                LOG.trace("Element request...");
                JsonObject data = arguments.get(0);
                String elementId = data.getString("elementId");
                String elementType = data.getString("elementType");
                eventBus.post(new FetchElementRequestEvent(elementId, elementType));
            }
        });
        addFunction("requestToc", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                LOG.trace("Toc request...");
                JsonObject data = arguments.get(0);
                String elementId = data.hasKey("elementId")? data.getString("elementId") : null;
                eventBus.post(new FetchCrossRefTocRequestEvent(elementId)); // along with TOC, the ancester tree for elementId passed will be fetched
            }
        });
    }

    private JsonValue convertToJson(Object bean) {
        //Ref to https://dev.vaadin.com/ticket/15446
        return JsonCodec.encode(bean, null, bean.getClass(), null).getEncodedValue();
    }
}
