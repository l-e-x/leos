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
package eu.europa.ec.leos.web.support.vaadin;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.ServiceException;
import com.vaadin.spring.annotation.UIScope;
import eu.europa.ec.leos.web.support.event.DeadEventLogSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@UIScope
@Component
public class DeadEventHandler {

    private static final long serialVersionUID = -2298382818306984474L;

    private static final Logger LOG = LoggerFactory.getLogger(DeadEventHandler.class);

    private EventBus leosEventBus;

    @Autowired
    public DeadEventHandler(EventBus leosEventBus){
        this.leosEventBus = leosEventBus;
    }

    @PostConstruct
    public void init() throws ServiceException {
        LOG.trace("Event bus init...");

        // register dead event logger
        Object deadEventSubscriber = new DeadEventLogSubscriber();
        leosEventBus.register(deadEventSubscriber);
        LOG.trace("Registered dead event logger: {}", deadEventSubscriber.getClass().getSimpleName());
    }
}
