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
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.spring.annotation.UIScope;
import eu.europa.ec.leos.web.event.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@UIScope
@Component
public class LeosVaadinErrorHandler extends DefaultErrorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LeosVaadinErrorHandler.class);
    private static final long serialVersionUID = 85924165400863840L;

    private EventBus eventBus;

    public LeosVaadinErrorHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void error(com.vaadin.server.ErrorEvent event) {
        // Find the final cause
        Throwable throwable = event.getThrowable();
        String errorReference = "#" + String.format("%04d", ThreadLocalRandom.current().nextInt(1, 10000));
        LOG.error("An error occurred! " + errorReference, throwable);
        eventBus.post(
                new NotificationEvent(NotificationEvent.Type.ERROR, "generic.error.message", errorReference));
    }
}
