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
package eu.europa.ec.leos.ui.view.annex;

import com.vaadin.annotations.JavaScript;
import com.vaadin.spring.annotation.SpringView;
import eu.europa.ec.leos.ui.view.AbstractLeosView;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.SessionAttribute;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

@SpringView(name = AnnexView.VIEW_ID)
@JavaScript({"vaadin://../js/ui/view/annexViewWrapper.js" + LeosCacheToken.TOKEN})
class AnnexViewImpl extends AbstractLeosView<AnnexScreenImpl> implements AnnexView {

    private static final long serialVersionUID = 1L;

    @Autowired
    AnnexViewImpl(AnnexScreenImpl screen, AnnexPresenter presenter) {
        super(screen, presenter);
        Validate.notNull(presenter, "Presenter must not be null!");
    }

    @Override
    protected String[] getParameterKeys() {
        String[] PARAM_KEYS = {SessionAttribute.ANNEX_ID.name()};
        return PARAM_KEYS;
    }
}
