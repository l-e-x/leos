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

import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import eu.europa.ec.leos.web.event.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeosSubscriberExceptionHandler implements SubscriberExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LeosSubscriberExceptionHandler.class);

    @Override
    public void handleException(Throwable exception, SubscriberExceptionContext context) {
        LOG.error("An exception occurred in a subscriber of the event bus!" +
                  "\nEvent class: " + context.getEvent().getClass().getSimpleName() +
                  "\nSubscriber class: " + context.getSubscriber().getClass().getSimpleName() +
                  "\nSubscriber method: " + context.getSubscriberMethod().getName() +
                  "\nException message: " + exception.getMessage(),
                exception);

        // KLUGE LEOS-2391 display a notification if any kind of unhandled exception occurs, not only for some selected cases
        // NOTE If the exception originates from a notification event then do not post another one to avoid an endless loop
        if (!NotificationEvent.class.isAssignableFrom(context.getEvent().getClass())) {
            context.getEventBus().post(
                    new NotificationEvent(NotificationEvent.Type.ERROR, "error.message", exception.getMessage()));
        }
    }
}
