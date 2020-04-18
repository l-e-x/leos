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
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.model.action.VersionVO;
import eu.europa.ec.leos.ui.event.doubleCompare.DoubleCompareRequestEvent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;

@SpringComponent
@ViewScope
@Instance(InstanceType.COUNCIL)
public class VersionComparatorMandate extends VersionComparator {
    
    @Autowired
    public VersionComparatorMandate(EventBus eventBus) {
        super(eventBus);
    }
    
    @Override
    int getNumberVersionsForComparing() {
        return 3;
    }
    
    @Override
    public boolean isCompareModeAvailable() {
        return false;
    }
    
    @Override
    void doubleCompare(Collection<VersionVO> selectedCheckBoxes) {
        List<VersionVO> orderedCheckboxes = getOrderedCheckboxes(selectedCheckBoxes);
        final String originalProposal = orderedCheckboxes.get(0).getDocumentId();
        final String intermediateMajor = orderedCheckboxes.get(1).getDocumentId();
        final String current = orderedCheckboxes.get(2).getDocumentId();
        eventBus.post(new DoubleCompareRequestEvent(originalProposal, intermediateMajor, current, true));
    }
}
