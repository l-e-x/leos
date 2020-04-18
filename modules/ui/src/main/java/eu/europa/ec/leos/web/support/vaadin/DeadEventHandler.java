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
package eu.europa.ec.leos.web.support.vaadin;

import com.google.common.eventbus.EventBus;
import com.vaadin.spring.annotation.UIScope;
import eu.europa.ec.leos.web.support.event.DeadEventLogSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@UIScope
@Component
public class DeadEventHandler {

    private static final long serialVersionUID = -2298382818306984474L;

    private static final Logger LOG = LoggerFactory.getLogger(DeadEventHandler.class);

    private EventBus eventBus;
    private EventBus leosApplicationEventBus;
    private DeadEventLogSubscriber deadEventSubscriber;

    @Autowired
    public DeadEventHandler(EventBus eventBus, EventBus leosApplicationEventBus){
        this.eventBus = eventBus;
        this.leosApplicationEventBus = leosApplicationEventBus;
    }

    @PostConstruct
    public void init() {
        LOG.trace("Event bus init...");

        // register dead event logger
        deadEventSubscriber = new DeadEventLogSubscriber();
        if(eventBus != null){
            eventBus.register(deadEventSubscriber);
        }
        if(leosApplicationEventBus != null){
            leosApplicationEventBus.register(deadEventSubscriber);
        }
        LOG.trace("Registered dead event logger: {}", deadEventSubscriber.getClass().getSimpleName());
    }

    @PreDestroy
    void destroy(){
        if(deadEventSubscriber != null){
            if(eventBus != null ){
                eventBus.unregister(deadEventSubscriber);
            }
            if(leosApplicationEventBus != null){
                leosApplicationEventBus.unregister(deadEventSubscriber);
            }
        }
    }
}
