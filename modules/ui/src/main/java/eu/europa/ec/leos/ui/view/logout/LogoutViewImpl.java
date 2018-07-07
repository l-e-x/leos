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
package eu.europa.ec.leos.ui.view.logout;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.VerticalLayout;
import eu.europa.ec.leos.ui.view.AbstractLeosView;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@SpringView(name = LogoutView.VIEW_ID)
class LogoutViewImpl extends AbstractLeosView<VerticalLayout> implements LogoutView {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LogoutViewImpl.class);

    @Autowired
    LogoutViewImpl(MessageHelper messageHelper) {
        super(new VerticalLayout());
        init(messageHelper);
    }

    private void init(MessageHelper messageHelper) {
        screen.addStyleName("leos-ui-layout");
        screen.setMargin(true);
        screen.setSizeFull();
        screen.addComponent(new Label(messageHelper.getMessage("leos.logout.message")));
    }

    @Override
    public void enter(ViewChangeEvent event) {
        super.enter(event);
        LOG.info("Logging out from Leos... by invalidating the HTTP session!");
        UI.getCurrent().getSession().getSession().invalidate();
    }

    @Override
    protected String[] getParameterKeys() {
        String[] PARAM_KEYS = {};
        return PARAM_KEYS;
    }
}
