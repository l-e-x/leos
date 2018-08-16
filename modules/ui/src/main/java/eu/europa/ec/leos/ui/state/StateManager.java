/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.ui.state;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.spring.annotation.UIScope;
import eu.europa.ec.leos.ui.event.StateChangeEvent;
import eu.europa.ec.leos.web.event.view.document.EditElementRequestEvent;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.io.Serializable;

@UIScope
@Component
public class StateManager implements Serializable {
    private static final long serialVersionUID = -8359610199520688951L;
    
    private EventBus eventBus;
    
    @Autowired
    public StateManager(EventBus eventBus) {
        super();
        this.eventBus = eventBus;
    }
    
    @PostConstruct
    private void init() {
       eventBus.register(this);
    }
    
    @Subscribe
    public void handleEditorOpenState(EditElementRequestEvent event) {
        eventBus.post(new StateChangeEvent(State.OPEN));
    }

    @Subscribe
    public void handleEditorCloseState(CloseElementEditorEvent event) {
        eventBus.post(new StateChangeEvent(State.CLOSE));
    }
}
