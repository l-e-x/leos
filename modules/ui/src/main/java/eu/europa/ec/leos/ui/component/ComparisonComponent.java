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
package eu.europa.ec.leos.ui.component;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.Panel;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.web.ui.component.ContentPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

@SpringComponent
@Scope("prototype")
public class ComparisonComponent<T extends XmlDocument> extends Panel implements ContentPane {
    private static final long serialVersionUID = 4303756450873933579L;

    @Autowired
    public ComparisonComponent() {
        setSizeFull();
    }

    @Override
    public float getDefaultPaneWidth(int numberOfFeatures, boolean tocPresent) {
        final float featureWidth;
        switch(numberOfFeatures){
            case 1:
                featureWidth=100f;
                break;
            default:
                if(tocPresent) {
                    featureWidth = 42.5f;
                } else {
                    featureWidth = 50f;
                }
                break;
        }//end switch
        return featureWidth;
    }
}
