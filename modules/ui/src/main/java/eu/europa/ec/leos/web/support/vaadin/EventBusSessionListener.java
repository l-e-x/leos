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
package eu.europa.ec.leos.web.support.vaadin;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import eu.europa.ec.leos.web.support.event.DeadEventLogSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope("singleton")
@Component("eventBusSessionListener")
public class EventBusSessionListener implements SessionInitListener {

    private static final long serialVersionUID = -2298382818306984444L;

    private static final Logger LOG = LoggerFactory.getLogger(EventBusSessionListener.class);

    @Autowired
    private EventBus leosEventBus;

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        LOG.trace("Event bus init...");
        LOG.trace("Session id: {}", event.getSession().getSession().getId());

        // register dead event logger
        Object deadEventSubscriber = new DeadEventLogSubscriber();
        leosEventBus.register(deadEventSubscriber);
        LOG.trace("Registered dead event logger: {}", deadEventSubscriber.getClass().getSimpleName());
    }
}
