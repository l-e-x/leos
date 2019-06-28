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
package eu.europa.ec.leos.web.ui.component.collaborators;

import com.vaadin.v7.event.FieldEvents;
import com.vaadin.v7.ui.ComboBox;

import java.util.LinkedHashSet;
import java.util.Map;
/*  This class is a specific and possibly not reusable extension of combo box.
    Its sole purpose is to trap the user input and pass this input text to interested class via registered listener */
class InputListeningComboBox extends ComboBox {

    private LinkedHashSet<FieldEvents.TextChangeListener> listenerList = new LinkedHashSet<>();
    private String inputString;

    void addInputChangeListener(FieldEvents.TextChangeListener listener) {
        listenerList.add(listener);
    }

    @Override
    public void changeVariables(Object source, Map<String, Object> variables) {
        super.changeVariables(source, variables);

        String clientValue = (String) variables.get("filter");
        if (clientValue != null && !clientValue.equals(inputString)) {
            inputString = clientValue;
            listenerList.forEach(listener -> listener.textChange(new FieldEvents.TextChangeEvent(this) {
                @Override
                public String getText() {
                    return inputString;
                }

                @Override
                public int getCursorPosition() {
                    return inputString.length();
                }
            }));
        }
    }
}
