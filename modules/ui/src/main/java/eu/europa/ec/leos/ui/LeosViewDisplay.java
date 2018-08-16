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
package eu.europa.ec.leos.ui;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.spring.annotation.SpringViewDisplay;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

@SpringViewDisplay
class LeosViewDisplay extends VerticalLayout implements ViewDisplay {

    LeosViewDisplay(){
        addStyleName("leos-body");
        setMargin(false);
        setSpacing(false);
        setSizeFull();
    }

    @Override
    public void showView(View view) {
        // Assuming View's are components, which is often the case
        removeAllComponents();
        addComponent((Component) view);
    }
}
