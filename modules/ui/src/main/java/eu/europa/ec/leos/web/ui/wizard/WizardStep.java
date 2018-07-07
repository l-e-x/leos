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
package eu.europa.ec.leos.web.ui.wizard;

import com.vaadin.ui.Component;

public interface WizardStep {

    /**
     * @return the title being displayed in the wizard window
     */
    public String getStepTitle();

    /**
     * @return the description of the current step being displayed in the footer
     */
    public String getStepDescription();

    /**
     * @return the body of the wizard
     */
    public Component getComponent();

    /**
     * this method validates the current step. it is called before moving away from the current step
     *
     * @return true if the step is valid, false otherwise
     */
    public boolean validateState();

    /**
     * @return rue if the current step can trigger the finish action, false otherwise
     */
    boolean canFinish();
}
