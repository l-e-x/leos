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
package eu.europa.ec.leos.web.ui.component.card;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.OptionGroup;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DynamicFilters implements Container.Filter {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicFilters.class);

    private MessageHelper messageHelper;
    private ArrayList<OptionGroup> dynamicCheckBoxGroups = new ArrayList<>();
    private BeanContainer container;
    private boolean updateInProgress = false;
    private  Map<String,Converter> keyConverterMap;

    public DynamicFilters(MessageHelper messageHelper, Map<String,Converter> keyConverterMap, BeanContainer container) {
        this.messageHelper = messageHelper;
        this.container = container;
        this.keyConverterMap=keyConverterMap;

        init();
    }

    private void init() {
        // 1. create option groups
        createOptionGroups();
        // 2. create mechanism to update data in Option Groups
        attachDataChangeListener();
    }

    public List<OptionGroup> getOptionGroups() {
        return dynamicCheckBoxGroups;
    }

    private void createOptionGroups() {
        for (String filterPropertyKey:keyConverterMap.keySet()) {
            OptionGroup newGroupForProperty = createNewOptionGroup(filterPropertyKey);
            dynamicCheckBoxGroups.add(newGroupForProperty);
        }
    }

    private OptionGroup createNewOptionGroup(String propertyId) {
        OptionGroup newGroupForProperty = new OptionGroup(messageHelper.getMessage("repository.caption.filters." + propertyId));
        newGroupForProperty.setData(propertyId); // will serve as ID of the Option Group
        newGroupForProperty.addStyleName("right"); //to move checkbox to right
        newGroupForProperty.addStyleName(propertyId);
        newGroupForProperty.setImmediate(true);
        newGroupForProperty.setMultiSelect(true);
        newGroupForProperty.setWidth("100%");
        newGroupForProperty.setItemCaptionMode(AbstractSelect.ItemCaptionMode.EXPLICIT);

        return newGroupForProperty;
    }

    @Override
    public boolean passesFilter(Object itemId, Item item) throws UnsupportedOperationException {
        for (OptionGroup optionGroup : dynamicCheckBoxGroups) {
            Property property = item.getItemProperty(optionGroup.getData());
            if (property != null && !optionGroup.isSelected(property.getValue())) {// check if the item is selected or not
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean appliesToProperty(Object propertyId) {
        return (keyConverterMap.keySet().contains(propertyId));
    }

    public boolean isUpdating() {
        return updateInProgress;
    }

    private void attachDataChangeListener() {
        // this listener which will populate checkbox in optionGroup
        container.addItemSetChangeListener(new Container.ItemSetChangeListener() {
            @Override
            public void containerItemSetChange(BeanItemContainer.ItemSetChangeEvent event) {
                //if filters are applied, items after filter application will be returned
                updateInProgress = true;// blocking changeValue events to avoid loop of triggers
                for (Object itemId : event.getContainer().getItemIds()) {
                    Item containerItem = event.getContainer().getItem(itemId);
                    updateAllCheckBoxes(containerItem);
                }
                updateInProgress = false;
            }
        });
    }

    private void updateAllCheckBoxes(Item containerItem) {
        // create a entry in all checkBoxesGroups for this
        for (OptionGroup checkBoxGroup: dynamicCheckBoxGroups) { // one to one ...filterkey <-->checkBoxGroups
            Object propertyValueForItem = containerItem.getItemProperty(checkBoxGroup.getData()).getValue();
            // property value is not present in checkBox Group then  add it
            if (propertyValueForItem != null && !checkBoxGroup.getItemIds().contains(propertyValueForItem)) {
                Collection<Container.Filter> appliedFilters= new ArrayList<>();
                appliedFilters.addAll(container.getContainerFilters());
                container.removeAllContainerFilters();

                Item checkBox = checkBoxGroup.addItem(propertyValueForItem);
                Converter converter =  keyConverterMap.get(checkBoxGroup.getData());
                String caption=(converter!=null && String.class.isAssignableFrom(converter.getPresentationType()))
                        ?(String) converter.convertToPresentation(propertyValueForItem, null, null)
                        :propertyValueForItem.toString();
                checkBoxGroup.setItemCaption(propertyValueForItem, caption);
                checkBoxGroup.select(propertyValueForItem);
                checkBoxGroup.markAsDirty();

                for(Container.Filter filter:appliedFilters) {
                    container.addContainerFilter(filter);
                }
            }
        }
    }
}
