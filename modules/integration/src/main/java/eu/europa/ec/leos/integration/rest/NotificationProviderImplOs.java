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
package eu.europa.ec.leos.integration.rest;

import eu.europa.ec.leos.integration.NotificationProvider;
import eu.europa.ec.leos.model.notification.EmailNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationProviderImplOs implements NotificationProvider {
    private static Logger LOG = LoggerFactory.getLogger(NotificationProviderImplOs.class);
    
    public void sendNotification(EmailNotification emailNotification) {
        LOG.info("This feature is not available for this distribution");
    }
}
