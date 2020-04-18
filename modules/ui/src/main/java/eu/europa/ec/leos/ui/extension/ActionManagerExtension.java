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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.annotations.JavaScript;
import com.vaadin.ui.AbstractComponent;
import eu.europa.ec.leos.vo.toc.TocItem;
import eu.europa.ec.leos.web.event.view.document.CancelActionElementRequestEvent;
import eu.europa.ec.leos.web.support.LeosCacheToken;

import java.util.List;

@JavaScript({"vaadin://../js/ui/extension/actionManagerConnector.js" + LeosCacheToken.TOKEN })
public class ActionManagerExtension<T extends AbstractComponent> extends LeosJavaScriptExtension {

    private static final long serialVersionUID = 1L;
    private EventBus eventBus;

    public ActionManagerExtension(T target, String instanceType, EventBus eventBus, List<TocItem> tocItemList) {
        super();
        getState().instanceType = instanceType;
        getState().tocItemsJsonArray = toJsonString(tocItemList);
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
    protected ActionManagerState getState() {
        return (ActionManagerState)super.getState();
    }

    @Subscribe
    public void cancelActionElement(CancelActionElementRequestEvent event) {
    	LOG.trace("Cancel action element...");
        callFunction("enableActions", event.getElementId());
    }

    private String toJsonString(Object o) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "null";
        }
    }
}
