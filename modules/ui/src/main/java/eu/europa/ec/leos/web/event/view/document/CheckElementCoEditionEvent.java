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
package eu.europa.ec.leos.web.event.view.document;

public class CheckElementCoEditionEvent {

    public static enum Action {
        EDIT("edit"), DELETE("delete"), MERGE("merge");

        private final String value;

        Action(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final String elementId;
    private final String elementTagName;
    private final String elementContent;
    private final Action action;
    private final Object actionEvent;

    public CheckElementCoEditionEvent(String elementId, String elementTagName, Action action, Object actionEvent) {
        this(elementId, elementTagName, null, action, actionEvent);
    }

    public CheckElementCoEditionEvent(String elementId, String elementTagName, String elementContent, Action action, Object actionEvent) {
        this.elementId = elementId;
        this.elementTagName = elementTagName;
        this.elementContent = elementContent;
        this.action = action;
        this.actionEvent = actionEvent;
    }

    public String getElementId() {
        return elementId;
    }

    public String getElementTagName() {
        return elementTagName;
    }

    public String getElementContent() {
        return elementContent;
    }

    public Action getAction() {
        return action;
    }

    public Object getActionEvent() {
        return actionEvent;
    }

}
