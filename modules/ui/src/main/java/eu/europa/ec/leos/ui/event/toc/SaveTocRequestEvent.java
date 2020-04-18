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
package eu.europa.ec.leos.ui.event.toc;


import eu.europa.ec.leos.model.action.CheckinElement;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;

import java.util.List;
import java.util.Set;

public class SaveTocRequestEvent {
    private List<TableOfContentItemVO> tableOfContentItemVOs;
    
    private Set<CheckinElement> saveElements;
    
    public SaveTocRequestEvent(List<TableOfContentItemVO> tocVOs, Set<CheckinElement> saveElements) {
        this.tableOfContentItemVOs = tocVOs;
        this.saveElements = saveElements;
    }
    
    public List<TableOfContentItemVO> getTableOfContentItemVOs() {
        return tableOfContentItemVOs;
    }
    
    public void setTableOfContentItemVOs(List<TableOfContentItemVO> tableOfContentItemVOs) {
        this.tableOfContentItemVOs = tableOfContentItemVOs;
    }
    
    public Set<CheckinElement> getSaveElements() {
        return saveElements;
    }
    
    public void setSaveElements(Set<CheckinElement> saveElements) {
        this.saveElements = saveElements;
    }
}
