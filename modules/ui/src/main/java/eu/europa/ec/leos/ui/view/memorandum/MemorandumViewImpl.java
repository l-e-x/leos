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
package eu.europa.ec.leos.ui.view.memorandum;

import com.vaadin.annotations.JavaScript;
import com.vaadin.spring.annotation.SpringView;
import eu.europa.ec.leos.ui.view.AbstractLeosView;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.SessionAttribute;
import org.springframework.beans.factory.annotation.Autowired;

@SpringView(name = MemorandumView.VIEW_ID)
@JavaScript({"vaadin://../js/ui/view/memorandumViewWrapper.js" + LeosCacheToken.TOKEN})
class MemorandumViewImpl extends AbstractLeosView<MemorandumScreenImpl> implements MemorandumView {

    private static final long serialVersionUID = 1L;

    @Autowired
    MemorandumViewImpl(MemorandumScreen memorandumScreen, MemorandumPresenter presenter) {
        super((MemorandumScreenImpl) memorandumScreen, presenter);
    }

    @Override
    protected String[] getParameterKeys() {
        String[] PARAM_KEYS = {SessionAttribute.MEMORANDUM_ID.name()};
        return PARAM_KEYS;
    }
}
