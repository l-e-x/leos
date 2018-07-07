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
package eu.europa.ec.leos.web.ui.component.documentCard;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.Property;
import com.vaadin.ui.*;
import eu.europa.ec.leos.model.content.LeosDocumentProperties.Stage;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.converter.StageValueConverter;

public class StagePopupView extends PopupView implements Property.Viewer {

    private Property newDataSource;
    private PopupContent content;

    private NativeSelect stages;//lazy
    private Label stageLabel = new Label();

    private MessageHelper messageHelper;
    private EventBus eventBus;

    //init is a stateless function which should not be tied to item/card data
    void init(MessageHelper messageHelper, final EventBus eventBus) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;

        stageLabel.setConverter(new StageValueConverter(messageHelper));
        content = new PopupContent();

        setHideOnMouseOut(true);
        setContent(content);
    }

    @Override
    public void setPropertyDataSource(Property newDataSource) {
        this.newDataSource = newDataSource;
        stageLabel.setPropertyDataSource(newDataSource);
    }

    @Override
    public Property getPropertyDataSource() {
        return newDataSource;
    }

    public Stage getSelectedStage() {
        return (stages!=null) ?(Stage) stages.getValue():null;
    }

    private NativeSelect createSelectGroup() {
        NativeSelect stages = new NativeSelect();
        stages.setNullSelectionAllowed(false);
        stages.addStyleName("stage-popup-view");
        StageValueConverter converter = new StageValueConverter(messageHelper);
        stages.setItemCaptionMode(AbstractSelect.ItemCaptionMode.EXPLICIT);
        for (Stage stage : Stage.values()) {
            stages.addItem(stage);
            stages.setItemCaption(stage, converter.convertToPresentation(stage, null, null));
        }
        if (newDataSource != null) {// lazy set
            stages.setValue(newDataSource.getValue());
        }
        return stages;
    }

    // content class for popup view
    class PopupContent implements PopupView.Content {

        @Override
        public String getMinimizedValueAsHTML() {
            return stageLabel.getValue();
        }

        @Override
        public Component getPopupComponent() {
            stages = createSelectGroup();
            return stages;
        }
    }
}
