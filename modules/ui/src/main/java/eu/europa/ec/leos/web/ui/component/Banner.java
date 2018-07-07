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
package eu.europa.ec.leos.web.ui.component;

import com.vaadin.server.Page;
import com.vaadin.server.WebBrowser;
import com.vaadin.ui.Component;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.VerticalLayout;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Banner extends VerticalLayout {
    private static final long serialVersionUID = 3924630817693865467L;

    private static final Logger LOG = LoggerFactory.getLogger(Banner.class);
    
    private MessageHelper messageHelper;
    private final String FIREFOX = "Firefox";
    private final String INTERNETEXPLORER = "Internet Explorer";
    private final String UNKNOWN = "Unknown Browser";

    public Banner(final MessageHelper msgHelper) {
        this.messageHelper = msgHelper;
        LOG.trace("Initializing Banner...");
        initLayout();
    }

    private void initLayout() {
        addStyleName("leos-banner-layout");
        WebBrowser browser = Page.getCurrent().getWebBrowser();
        if (!browser.isChrome()) {
            Component label = getBrowserWarning(browser);
            addComponent(label);
        }
    }
    
    private Component getBrowserWarning(WebBrowser browser) {
        String browserName = "";
        Label warningLabel = new Label();
        if(browser.isFirefox()) {
            browserName = FIREFOX;
        } else if(browser.isIE()) {
            browserName = INTERNETEXPLORER;
        } else {
            browserName = UNKNOWN;
        }
        warningLabel.setValue(messageHelper.getMessage("leos.ui.header.browser.warning", browserName));
        warningLabel.addStyleName("leos-banner-message");
        return warningLabel;
    }
}
