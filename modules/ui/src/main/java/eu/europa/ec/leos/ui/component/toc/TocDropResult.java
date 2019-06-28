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
package eu.europa.ec.leos.ui.component.toc;

import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;

public class TocDropResult {

    private boolean success;
    private String messageKey;
    private TableOfContentItemVO sourceItem;
    private TableOfContentItemVO targetItem;

    public TocDropResult(boolean success, String messageKey, TableOfContentItemVO sourceItem, TableOfContentItemVO targetItem) {
        this.success = success;
        this.messageKey = messageKey;
        this.sourceItem = sourceItem;
        this.targetItem = targetItem;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public TableOfContentItemVO getSourceItem() {
        return sourceItem;
    }

    public void setSourceItem(TableOfContentItemVO sourceItem) {
        this.sourceItem = sourceItem;
    }

    public TableOfContentItemVO getTargetItem() {
        return targetItem;
    }

    public void setTargetItem(TableOfContentItemVO targetItem) {
        this.targetItem = targetItem;
    }
}
