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
package eu.europa.ec.leos.web.ui.navigation;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.spring.navigator.SpringViewProvider;

@UIScope
@SpringComponent
public class LeosViewProvider implements ViewProvider {

    private static final long serialVersionUID = 1L;

    private final SpringViewProvider springViewProvider;

    LeosViewProvider(SpringViewProvider springViewProvider) {
        this.springViewProvider = springViewProvider;
    }

    @Override
    public String getViewName(String viewAndParameters) {
        if ((viewAndParameters != null) && viewAndParameters.isEmpty()) {
            return Target.HOME.getViewId();
        }
        return null;
    }

    @Override
    public View getView(String viewName) {
        return springViewProvider.getView(viewName);
    }
}
