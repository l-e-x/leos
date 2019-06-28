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
package eu.europa.ec.leos.ui.window;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.Binder;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
import eu.europa.ec.leos.ui.event.CreateMilestoneEvent;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.window.AbstractWindow;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MilestoneWindow extends AbstractWindow {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(MilestoneWindow.class);
    private static final String COMBOBOX_PREFIX_KEY = "document.create.milestone.combobox.key.";
    private static final String COMBOBOX_PREFIX_VALUE = "document.create.milestone.combobox.value.";
    private static final int TEXT_AREA_LENGTH = 200;

    private ComboBox titleComboBox;
    private TextArea titleArea;
    Binder<TitleVO> titleBinder;

    public MilestoneWindow(MessageHelper messageHelper, EventBus eventBus) {
        super(messageHelper, eventBus);
        setCaption(messageHelper.getMessage("document.create.milestone.window.title"));
        prepareWindow();
    }

    public void clear() {
        titleArea.clear();
    }

    private void prepareWindow() {
        setWidth("420px");
        setHeight("187px");
        addStyleName("milestoneWindow");

        FormLayout windowLayout = new FormLayout();
        windowLayout.setSizeFull();
        windowLayout.setMargin(false);
        windowLayout.setSpacing(true);
        setBodyComponent(windowLayout);
        buildLayout(windowLayout);

        addButton(buildCreateButton());
    }

    private void buildLayout(FormLayout windowLayout) {
        titleComboBox = new ComboBox();
        titleComboBox.setWidth(100, Unit.PERCENTAGE);
        titleComboBox.setEmptySelectionAllowed(false);
        titleComboBox.setTextInputAllowed(false);
        final List<ComboBoxItemVO> items = getComboBoxItems();
        final ComboBoxItemVO otherItem = items.get(items.size()-1);
        final ComboBoxItemVO selectedItem = items.get(0);
        titleComboBox.setItems(items);
        titleComboBox.setValue(selectedItem);
        titleComboBox.addValueChangeListener(event -> {
            if (event.getSource().isEmpty()) {
                titleArea.setValue("");
            } else {
                populateTextArea((ComboBoxItemVO)event.getValue(), otherItem);
            }
        });
        windowLayout.addComponent(titleComboBox);

        titleArea = new TextArea();
        titleArea.setCaption(messageHelper.getMessage("document.create.milestone.title.caption"));
        titleArea.setWidth(100, Unit.PERCENTAGE);
        titleArea.setRows(3);
        titleArea.setMaxLength(TEXT_AREA_LENGTH);
        titleBinder = new Binder<>();
        windowLayout.addComponent(titleArea);

        populateTextArea(selectedItem, otherItem);
    }

    private void populateTextArea(ComboBoxItemVO selectedItem, ComboBoxItemVO otherItem) {
        if(selectedItem.equals(otherItem)){
            titleArea.setValue("");
            titleArea.setPlaceholder(otherItem.getDescription());
            titleArea.setEnabled(true);
            titleArea.setRequiredIndicatorVisible(true);

            titleBinder.forField(titleArea).asRequired(messageHelper.getMessage("document.create.milestone.validation.error"))
                    .withValidator(val -> !StringUtils.isBlank(val), messageHelper.getMessage("document.create.milestone.validation.empty.space.error"))
                    .bind(TitleVO::getTitle, TitleVO::setTitle);
            titleBinder.setBean(new TitleVO(""));
        } else {
            titleArea.setValue(selectedItem.getDescription());
            titleArea.setEnabled(false);
            titleArea.setRequiredIndicatorVisible(false);

            titleBinder.setBean(new TitleVO(selectedItem.getDescription()));
        }
    }

    private List<ComboBoxItemVO> getComboBoxItems() {
        List<ComboBoxItemVO> items = new ArrayList<>();
        String title_key = String.format("%s%d", COMBOBOX_PREFIX_KEY, 1);
        String title_value = messageHelper.getMessage(title_key);
        String desc_key = String.format("%s%d", COMBOBOX_PREFIX_VALUE, 1);
        String desc_value = getMessageWithMaxLength(messageHelper.getMessage(desc_key));

        for (int i = 2; isAnotherItemPresent(title_key, title_value); i++) {
            // if "value" is missing in properties file, we show the "key" value
            if(StringUtils.isBlank(desc_value) || desc_key.equals(desc_value)){
                desc_value = title_value;
            }
            items.add(new ComboBoxItemVO(title_value, desc_value));

            title_key = String.format("%s%d", COMBOBOX_PREFIX_KEY, i);
            title_value = messageHelper.getMessage(title_key);
            desc_key = String.format("%s%d", COMBOBOX_PREFIX_VALUE, i);
            desc_value = getMessageWithMaxLength(messageHelper.getMessage(desc_key));
        }

        // last item, "other" item
        title_value = messageHelper.getMessage("document.create.milestone.combobox.other");
        desc_value = getMessageWithMaxLength(messageHelper.getMessage("document.create.milestone.textarea.placeholder"));
        items.add(new ComboBoxItemVO(title_value, desc_value));

        return items;
    }

    private boolean isAnotherItemPresent(String title_key, String title_value) {
        return StringUtils.isNotBlank(title_value) && !title_key.equals(title_value);
    }

    private String getMessageWithMaxLength(String message) {
        if(message.length() > TEXT_AREA_LENGTH){
            message = message.substring(0, TEXT_AREA_LENGTH);
        }
        return message;
    }

    private Button buildCreateButton() {
        Button createButton = new Button(messageHelper.getMessage("document.create.milestone.button.create"));
        createButton.addStyleName("primary");
        createButton.addClickListener(event -> {
            if (titleBinder.validate().isOk()) {
                String savedTitle = titleBinder.getBean().getTitle();
                LOG.debug("Milestone creation titled '{}' has been requested.", savedTitle.trim());
                eventBus.post(new CreateMilestoneEvent(savedTitle.trim()));
                close();
            }
        });
        return createButton;
    }

    class ComboBoxItemVO {
        private String title;
        private String description;

        public ComboBoxItemVO(String title, String description){
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean equals(Object o){
            if (o == null || getClass() != o.getClass())
                return false;

            ComboBoxItemVO item = (ComboBoxItemVO) o;
            return Objects.equals(title, item.title) &&
                    Objects.equals(description, item.description);
        }

        @Override
        public int hashCode(){
            return Objects.hash(title, description);
        }

        @Override
        public String toString() {
            return title;
        }
    }

    class TitleVO {
        private String title;

        public TitleVO(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}