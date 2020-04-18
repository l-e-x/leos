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
package eu.europa.ec.leos.ui.component.versions;

import com.google.common.eventbus.EventBus;
import eu.europa.ec.leos.model.action.VersionVO;
import eu.europa.ec.leos.web.event.component.CompareRequestEvent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public abstract class VersionComparator {
    
    protected EventBus eventBus;
    
    @Autowired
    public VersionComparator(EventBus eventBus){
        this.eventBus = eventBus;
    }
    
    abstract int getNumberVersionsForComparing();
    
    abstract void doubleCompare(Collection<VersionVO> selectedCheckBoxes);
    
    public boolean isCompareModeAvailable() {
        return true;
    }

    List<VersionVO> getOrderedCheckboxes(Collection<VersionVO> checkboxes) {
        //TODO create new comparator ordering by cmis:versionNumber
        List<VersionVO> orderedCheckboxes = new ArrayList<>(checkboxes);
        orderedCheckboxes.sort(Comparator.comparing(VersionVO::getVersionNumber));
        return orderedCheckboxes;
    }

    void compare(Collection<VersionVO> selectedCheckBoxes) {
        List<VersionVO> orderedCheckboxes = getOrderedCheckboxes(selectedCheckBoxes);
        final String oldVersion = orderedCheckboxes.get(0).getDocumentId();
        final String newVersion = orderedCheckboxes.get(1).getDocumentId();
        eventBus.post(new CompareRequestEvent(oldVersion, newVersion));
    }
    
}
