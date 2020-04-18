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
import com.vaadin.ui.AbstractField;
import elemental.json.Json;
import elemental.json.JsonObject;
import eu.europa.ec.leos.ui.event.StateChangeEvent;
import eu.europa.ec.leos.ui.event.metadata.DocumentMetadataRequest;
import eu.europa.ec.leos.ui.event.metadata.DocumentMetadataResponse;
import eu.europa.ec.leos.ui.event.metadata.SearchMetadataRequest;
import eu.europa.ec.leos.ui.event.metadata.SearchMetadataResponse;
import eu.europa.ec.leos.ui.event.security.SecurityTokenRequest;
import eu.europa.ec.leos.ui.event.security.SecurityTokenResponse;
import eu.europa.ec.leos.web.event.view.document.FetchUserPermissionsRequest;
import eu.europa.ec.leos.web.event.view.document.FetchUserPermissionsResponse;
import eu.europa.ec.leos.web.event.view.document.MergeSuggestionRequest;
import eu.europa.ec.leos.web.event.view.document.MergeSuggestionResponse;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;

@JavaScript({"vaadin://../js/ui/extension/annotateConnector.js" + LeosCacheToken.TOKEN})
public class AnnotateExtension<T extends AbstractField<V>, V> extends LeosJavaScriptExtension {

    private static final long serialVersionUID = 1L;

    public enum OperationMode {
        READ_ONLY, // Annotations open in read only. Cannot create edit or deleted new annotations
        PRIVATE,  // New annotations can only be private, i.e., only POST_TO_ME option is available - to be used in ISC context
        NORMAL // Annotations open in regular mode
    }

    private EventBus eventBus;

    public AnnotateExtension(T target, EventBus eventBus, ConfigurationHelper cfgHelper, String containerId, OperationMode operationMode, boolean showStatusFilter, boolean showGuideLinesButton) {
        super();
        this.eventBus = eventBus;
        registerServerSideAPI();
        getState().authority = cfgHelper.getProperty("annotate.authority");
        getState().anotClient = cfgHelper.getProperty("annotate.client.url");
        getState().anotHost = cfgHelper.getProperty("annotate.server.url");
        getState().oauthClientId = cfgHelper.getProperty("annotate.jwt.issuer.client.id");
        getState().operationMode = operationMode.name();
        getState().showStatusFilter = showStatusFilter;
        getState().showGuideLinesButton = showGuideLinesButton;
        containerId = (containerId == null) ? cfgHelper.getProperty("annotation.container") : ("#" + containerId);
        getState().annotationContainer = containerId;
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
    protected AnnotateState getState() {
        return (AnnotateState) super.getState();
    }

    @Override
    protected AnnotateState getState(boolean markAsDirty) {
        return (AnnotateState) super.getState(markAsDirty);
    }

    protected void extend(T target) {
        super.extend(target);
        // handle target's value change
        target.addValueChangeListener(event -> {
            LOG.trace("Target's value changed...");
            // Mark that this connector's state might have changed.
            // There is no need to send new data to the client-side,
            // since we just want to trigger a state change event...
            forceDirty();
        });
    }
    
    public void setoperationMode(OperationMode operationMode) {
        getState().operationMode = operationMode.name();
    }

    private void registerServerSideAPI() {
        addFunction("requestUserPermissions", arguments -> eventBus.post(new FetchUserPermissionsRequest()));
        addFunction("requestSecurityToken", arguments -> eventBus.post(new SecurityTokenRequest(getState(false).anotClient)));
        addFunction("requestDocumentMetadata", arguments -> eventBus.post(new DocumentMetadataRequest()));
        addFunction("requestSearchMetadata", arguments -> eventBus.post(new SearchMetadataRequest()));
        addFunction("requestMergeSuggestion", arguments -> {
            try {
                JsonObject selector = arguments.getObject(0);
                String origText = selector.getString("origText");
                String newText = selector.getString("newText");
                String elementId = selector.getString("elementId");
                int startOffset = (int) selector.getNumber("startOffset");
                int endOffset = (int) selector.getNumber("endOffset");
                if (origText == null || newText == null || elementId == null || startOffset < 0 || endOffset < 0 || startOffset == endOffset) {
                    throw new Exception("Invalid request parameters");
                }

                eventBus.post(new MergeSuggestionRequest(origText, newText, elementId, startOffset, endOffset));
            }
            catch (Exception e) {
                LOG.debug("Request merge suggestion stopped because of bad arguments in the request");
            }
        });
    }

    @Subscribe
    public void receiveTicket(SecurityTokenResponse event) {
        callFunction("receiveSecurityToken", event.getToken());
    }

    @Subscribe
    public void receiveDocumentMetadata(DocumentMetadataResponse event) {
        callFunction("receiveDocumentMetadata", event.getMetadata().toString());
    }

    @Subscribe
    public void receiveSearchMetadata(SearchMetadataResponse event) {
        callFunction("receiveSearchMetadata", event.getMetadataSets().toString());
    }

    @Subscribe
    public void stateChangeHandler(StateChangeEvent event) {
        callFunction("stateChangeHandler", event.getState());
    }

    @Subscribe
    public void permissionsAvailable(FetchUserPermissionsResponse event) {
        callFunction("receiveUserPermissions", event.getUserPermissions().toArray());
    }

    @Subscribe
    public void resultMergeSuggestion(MergeSuggestionResponse event) {
        JsonObject result = Json.createObject();
        result.put("result", event.getResult().name());
        result.put("message", event.getMessage());
        callFunction("receiveMergeSuggestion", result);
    }
}
