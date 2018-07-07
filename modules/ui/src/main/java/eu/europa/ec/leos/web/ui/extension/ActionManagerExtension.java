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
package eu.europa.ec.leos.web.ui.extension;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.ClientConnector;
import com.vaadin.ui.Label;
import eu.europa.ec.leos.web.support.LeosCacheToken;

@JavaScript({"vaadin://../js/web/actionManagerConnector.js" + LeosCacheToken.TOKEN })
public class ActionManagerExtension extends LeosJavaScriptExtension {

    public void extend(Label target) {
        super.extend(target);
    }

    @Override
    protected Class<? extends ClientConnector> getSupportedParentType() {
        return Label.class;
    }
}