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
package eu.europa.ec.leos.ui.window;

import com.vaadin.spring.annotation.SpringComponent;
import eu.europa.ec.leos.domain.common.InstanceContext;
import eu.europa.ec.leos.services.support.flow.Workflow;
import eu.europa.ec.leos.ui.component.toc.TableOfContentItemConverter;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;

@SpringComponent
@Workflow(InstanceContext.Type.COUNCIL)
public class MandateTocEditor implements TocEditor {
    
    @Override
    public boolean isNumFieldEditable(TableOfContentItemVO item) {
        return !TableOfContentItemConverter.EC.equals(item.getOriginNumAttr());
    }

    @Override    
    public TableOfContentItemVO moveOriginAttribute(TableOfContentItemVO droppedElement, TableOfContentItemVO targetElement) {
        //TODO:May need to re-implement once numbering inside CK is implemented
        if(isElementAndTargetOriginDifferent(droppedElement, targetElement)) {
            droppedElement.setOriginAttr(TableOfContentItemConverter.CN);
        }
        droppedElement.setOriginNumAttr(TableOfContentItemConverter.CN);
        droppedElement.setNumber("#");
        return droppedElement;
    }
    
    private boolean isElementAndTargetOriginDifferent(TableOfContentItemVO element, TableOfContentItemVO parent) {
        boolean isDifferent = false;
        if(element.getOriginAttr() == null) {
            isDifferent = true;
        }
        else if (!element.getOriginAttr().equals(parent.getOriginAttr())) {
            isDifferent = true;
        }
        return isDifferent;
    }

}
