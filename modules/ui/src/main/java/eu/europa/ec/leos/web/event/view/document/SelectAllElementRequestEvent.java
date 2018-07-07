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
package eu.europa.ec.leos.web.event.view.document;

public class SelectAllElementRequestEvent {
    private boolean value;
    private String elementName;

    public SelectAllElementRequestEvent(boolean value, String elementName) {
        super();
        this.value = value;
        this.elementName = elementName;
    }
    
    public boolean isValue() {
        return value;
    }

    public String getElementName() {
        return elementName;
    }
}
