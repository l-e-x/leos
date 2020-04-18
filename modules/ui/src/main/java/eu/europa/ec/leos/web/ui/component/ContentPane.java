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
package eu.europa.ec.leos.web.ui.component;

import com.vaadin.ui.Component;

public interface ContentPane extends Component {

    /**
     * Called when defining horizontal splitter position.
     *
     * NOTE: implementing classes: MemorandumComponent; LegalTextComponent; AnnexComponent include space for ToC
     * and Doc presentation. meaning when numberOfFeatures is 2 and ToC is present, more space needs to be assigned
     * for the components, when ToC is not present, by default, width can be split evenly.
     *
     * @param numberOfFeatures
     * @param tocPresent
     * @return
     */
    default float getDefaultPaneWidth(int numberOfFeatures, boolean tocPresent) {
        final float featureWidth;
        switch(numberOfFeatures){
            case 1:
                featureWidth=100f;
                break;
            default:
                if(tocPresent) {
                    featureWidth = 57.5f;
                } else {
                    featureWidth = 50f;
                }
                break;
        }
        return featureWidth;
    }
}
