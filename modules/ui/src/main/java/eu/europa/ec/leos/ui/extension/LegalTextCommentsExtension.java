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
import com.vaadin.annotations.StyleSheet;
import com.vaadin.ui.AbstractField;
import eu.europa.ec.leos.web.event.view.document.LegalTextCommentToggleEvent;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@StyleSheet({"vaadin://../lib/bootstrap_3.3.6/css/bootstrap.css" + LeosCacheToken.TOKEN})
@JavaScript({"vaadin://../js/ui/extension/comments/legalTextCommentsConnector.js"+ LeosCacheToken.TOKEN })
public class LegalTextCommentsExtension<T extends AbstractField<V>, V> extends LeosJavaScriptExtension {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(LegalTextCommentsExtension.class);

    private EventBus eventBus;

    public LegalTextCommentsExtension(T target, EventBus eventBus) {
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
    
    @Subscribe
    public void toggleCommentState(LegalTextCommentToggleEvent event) {
        LOG.trace("LegalTextCommentToggleEvent listener...");
        getState().commentsVisible = !getState().commentsVisible;
    }

    @Override
    protected LegalTextCommentsState getState() {
        return (LegalTextCommentsState) super.getState();
    }
}
