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

public class RangeSliderStepVO {
    
    private String stepValue;
    private String mileStoneComments;
    private boolean milestoneVersion;
    
    
    public RangeSliderStepVO(String stepValue, String mileStoneComments, boolean milestoneVersion) {
        this.stepValue = stepValue;
        this.mileStoneComments = mileStoneComments;
        this.milestoneVersion = milestoneVersion;
    }
    
    public String getStepValue() {
        return stepValue;
    }
    public void setStepValue(String stepValue) {
        this.stepValue = stepValue;
    }
    public boolean isMilestoneVersion() {
        return milestoneVersion;
    }
    public void setMilestoneVersion(boolean milestoneVersion) {
        this.milestoneVersion = milestoneVersion;
    }

    public String getMileStoneComments() {
        return mileStoneComments;
    }

    public void setMileStoneComments(String mileStoneComments) {
        this.mileStoneComments = mileStoneComments;
    }
}
