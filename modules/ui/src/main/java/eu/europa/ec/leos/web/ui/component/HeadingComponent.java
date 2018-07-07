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

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.*;
import org.springframework.context.annotation.Scope;

@SpringComponent
@Scope("prototype")
public class HeadingComponent extends HorizontalLayout {

    private static final long serialVersionUID = 1942600603461274213L;

    private Label name = new Label();
    private Button rightButton;

    public HeadingComponent() {
        setPrimaryStyleName("ui-block-heading");
        setWidth("100%");
        name.setPrimaryStyleName("ui-block-caption");
        addComponent(name);
        setComponentAlignment(name, Alignment.TOP_LEFT);
    }

    @Override
    public void setCaption(String caption) {
        name.setValue(caption);
    }

    @Override
    public String getCaption() {
        return name.getValue();
    }

    public void addRightButton(Button rightButton) {
        this.rightButton = rightButton;
        rightButton.setPrimaryStyleName("icon-only");
        rightButton.addStyleName("borderless end-button");
        addComponent(rightButton);
        setComponentAlignment(rightButton, Alignment.TOP_RIGHT);
    }

    public Button getRightButton() {
        return rightButton;
    }
}
