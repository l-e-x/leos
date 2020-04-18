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
package eu.europa.ec.leos.web.ui.component.layout;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import javax.annotation.Nonnull;

@org.springframework.stereotype.Component
@Scope("vaadin-ui")
@Instance(InstanceType.COMMISSION)
public class ProposalHeader extends Header {

    @Autowired
    public ProposalHeader(LanguageHelper langHelper,
            MessageHelper msgHelper, EventBus eventBus,
            SecurityContext securityContext) {
        super(langHelper, msgHelper, eventBus, securityContext);
    }

    @Override
    @Nonnull public Component buildLogo() {
        return new Image(null, LeosTheme.LEOS_HEADER_LOGO_RESOURCE_PROPOSAL);
    }
}
