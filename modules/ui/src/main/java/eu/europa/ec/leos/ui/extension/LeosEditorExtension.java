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
import com.vaadin.server.JsonCodec;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.JavaScriptFunction;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import eu.europa.ec.leos.ui.event.CloseBrowserRequestEvent;
import eu.europa.ec.leos.ui.event.MergeElementRequestEvent;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.view.document.CheckElementCoEditionEvent.Action;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import eu.europa.ec.leos.web.model.UserVO;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

@JavaScript({"vaadin://../js/editor/leosEditorConnector.js" + LeosCacheToken.TOKEN})
public class LeosEditorExtension<T extends AbstractComponent> extends LeosJavaScriptExtension {

    private EventBus eventBus;

    public LeosEditorExtension(T target, EventBus eventBus, ConfigurationHelper cfgHelper) {
        super();
        this.eventBus = eventBus;

        getState().isImplicitSaveEnabled = Boolean.valueOf(cfgHelper.getProperty("implicitSaveAndClose.enabled"));
        getState().isSpellCheckerEnabled = Boolean.valueOf(cfgHelper.getIntegrationProperty("leos.spell.checker.enabled"));
        getState().spellCheckerServiceUrl = cfgHelper.getIntegrationProperty("leos.spell.checker.service.url");
        getState().spellCheckerSourceUrl = cfgHelper.getIntegrationProperty("leos.spell.checker.source.url");

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
        getState(false).permissions = event.getPermissions();
        callFunction("editElement", event.getElementId(), event.getElementTagName(), event.getElementFragment(), event.getDocType(), event.getInstanceType(), event.getAlternatives());
    }

    @Subscribe
    public void refreshElement(RefreshElementEvent event) {
        LOG.trace("Refreshing element...");
        callFunction("refreshElement", event.getElementId(), event.getElementTagName(), event.getElementFragment());
    }

    @Subscribe
    public void receiveElement(FetchElementResponseEvent event) {
        LOG.trace("Receiving element...");
        callFunction("receiveElement", event.getElementId(), event.getElementTagName(), event.getElementFragment());
    }

    @Subscribe
    public void receiveToc(FetchCrossRefTocResponseEvent event) {
        LOG.trace("Receiving table of content...");
        callFunction("receiveToc", convertToJson(event.getTocAndAncestorsVO()));
    }

    @Subscribe
    public void receiveReferenceLabel(ReferenceLabelResponseEvent event) {
        LOG.trace("Receiving references...");
        callFunction("receiveRefLabel", event.getLabel().replaceAll("xml:id=", "id="));
    }

    @Subscribe
    public void closeElement(CloseElementEvent event) {
        LOG.trace("Closing element...");
        callFunction("closeElement");
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
                eventBus.post(new CheckElementCoEditionEvent(elementId, elementType, Action.EDIT, new EditElementRequestEvent(elementId, elementType)));
            }
        });
        addFunction("deleteElementAction", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                LOG.trace("Delete element action...");
                JsonObject data = arguments.get(0);
                String elementId = data.getString("elementId");
                String elementType = data.getString("elementType");
                eventBus.post(new CheckElementCoEditionEvent(elementId, elementType, Action.DELETE, new DeleteElementRequestEvent(elementId, elementType)));
            }
        });
        addFunction("releaseElement", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                LOG.trace("Releasing element...");
                JsonObject data = arguments.get(0);
                String elementId = data.getString("elementId");
                String elementType = data.getString("elementType");
                eventBus.post(new CloseElementEditorEvent(elementId, elementType));
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
                Boolean isSaveAndClose = data.getBoolean("isSaveAndClose");
                Validate.isTrue(elementFragment.contains(elementType), String.format("Element must contain %s", elementType));
                eventBus.post(new SaveElementRequestEvent(elementId, elementType, elementFragment, isSaveAndClose));
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
                JsonArray nodeIds = data.hasKey("elementIds") ? data.getArray("elementIds") : null;
                List<String> elementIds = new ArrayList<>();
                for (int index = 0; nodeIds != null && index < nodeIds.length(); index++) {
                    elementIds.add(nodeIds.getString(index));
                }
                eventBus.post(new FetchCrossRefTocRequestEvent(elementIds)); // along with TOC, the ancester tree for elementId passed will be fetched
            }
        });
        addFunction("requestRefLabel", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) {
                LOG.trace("Reference Label request...");
                JsonObject data = arguments.get(0);
                String currentElementId = data.hasKey("currentEditPosition") ? data.getString("currentEditPosition") : null;
                JsonArray refs = data.hasKey("references") ? data.getArray("references") : null;
                List<String> references = new ArrayList<>();
                for (int index = 0; refs != null && index < refs.length(); index++) {
                    references.add(refs.getString(index));
                }
                eventBus.post(new ReferenceLabelRequestEvent(references, currentElementId));
            }
        });
        addFunction("closeBrowser", arguments -> {
            LOG.trace("Close browser request...");
            eventBus.post(new CloseBrowserRequestEvent());
        });
        addFunction("mergeElement", arguments -> {
            LOG.trace("Merge element request...");
            JsonObject data = arguments.get(0);
            String elementId = data.getString("elementId");
            String elementType = data.getString("elementType");
            String elementFragment = data.getString("elementFragment");
            Validate.isTrue(elementFragment.contains(elementType), String.format("Element must contain %s", elementType));
            eventBus.post(new CheckElementCoEditionEvent(elementId, elementType, elementFragment, Action.MERGE,
                    new MergeElementRequestEvent(elementId, elementType, elementFragment)));
        });

    }

    private JsonValue convertToJson(Object bean) {
        // Ref to https://dev.vaadin.com/ticket/15446
        return JsonCodec.encode(bean, null, bean.getClass(), null).getEncodedValue();
    }
}