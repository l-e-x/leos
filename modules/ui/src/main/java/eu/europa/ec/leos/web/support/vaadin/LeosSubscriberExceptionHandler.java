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

import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeosSubscriberExceptionHandler implements SubscriberExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LeosSubscriberExceptionHandler.class);

    @Override
    public void handleException(Throwable exception, SubscriberExceptionContext context) {
        LOG.error("An exception occurred in a subscriber of the event bus: " + context.getSubscriber().getClass().getSimpleName() + " " +
                context.getSubscriberMethod().getName() + " for event: " + context.getEvent().getClass().getSimpleName(), exception);
    }
}