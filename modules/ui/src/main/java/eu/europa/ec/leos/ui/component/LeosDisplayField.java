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
package eu.europa.ec.leos.ui.component;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Label;

public class LeosDisplayField extends CustomField<String> {

    private static final long serialVersionUID = 1L;

    private final Label content = new Label();

    @Override
    protected Component initContent() {
        content.setContentMode(ContentMode.HTML);
        content.setSizeFull();
        return content;
    }

    @Override
    protected void doSetValue(String text) {
        content.setValue(text);
    }

    @Override
    public String getValue() {
        return content.getValue();
    }
}
