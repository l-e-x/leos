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
package eu.europa.ec.leos.web.ui.component.layout;

import eu.europa.ec.leos.web.support.LeosBuildInfo;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import com.vaadin.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class Footer extends CustomComponent {

    private static final long serialVersionUID = -4244410110038968920L;

    private static final Logger LOG = LoggerFactory.getLogger(Footer.class);

    private MessageHelper msgHelper;

    public Footer(final MessageHelper msgHelper) {
        this.msgHelper = msgHelper;

        LOG.trace("Initializing footer...");
        initLayout();

    }

    // initialize footer layout
    private void initLayout() {
        // create footer layout
        final VerticalLayout footerLayout = new VerticalLayout();
        footerLayout.addStyleName("leos-footer-layout");
        footerLayout.setHeight("20px");

        // set footer layout as composition root
        setCompositionRoot(footerLayout);
        addStyleName("leos-footer");

        // info
        final Component info = buildFooterInfo();
        footerLayout.addComponent(info);
        footerLayout.setComponentAlignment(info, Alignment.MIDDLE_CENTER);
    }

    private @Nonnull
    Component buildFooterInfo() {
        final String infoMsg = msgHelper.getMessage(
                "leos.ui.footer.info",
                LeosBuildInfo.BUILD_VERSION,
                LeosBuildInfo.BUILD_ENVIRONMENT,
                LeosBuildInfo.SOURCE_REVISION,
                LeosBuildInfo.SOURCE_STATUS,
                LeosBuildInfo.BUILD_DATE);

        final Label infoLabel = new Label(infoMsg);
        infoLabel.setSizeUndefined();
        return infoLabel;
    }
}
