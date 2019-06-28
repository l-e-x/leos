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
package eu.europa.ec.leos.ui.view.repository;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.v7.data.Container;
import com.vaadin.v7.data.util.BeanContainer;
import com.vaadin.v7.data.util.converter.Converter;
import com.vaadin.v7.data.util.filter.SimpleStringFilter;
import com.vaadin.v7.ui.*;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.card.DynamicFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

public class RepositoryFilters<CARD_DATA> {
    private MessageHelper messageHelper;
    private LanguageHelper langHelper;
    private EventBus eventBus;

    private static final Logger LOG = LoggerFactory.getLogger(RepositoryFilters.class);

    RepositoryFilters(MessageHelper messageHelper, LanguageHelper langHelper, EventBus eventBus,
            VerticalLayout filterBox, TextField searchBox,
            BeanContainer<String, CARD_DATA> container, Map<String, Converter> dynamicFiltersConverterMap, User user) {
        this.messageHelper = messageHelper;
        this.langHelper = langHelper;
        this.eventBus = eventBus;

        FilterController controller = constructController(container, dynamicFiltersConverterMap, user);
        initFilterBox(filterBox, controller);
        initSearchBox(searchBox, controller);
    }

    private FilterController constructController(BeanContainer<String, CARD_DATA> container, Map<String, Converter> propertyConverterMap, User user) {
        return new FilterController(container, propertyConverterMap, user);
    }

    private VerticalLayout initFilterBox(VerticalLayout filterBox, final FilterController controller) {
        filterBox.removeAllComponents();
        // 1. create Top title
        filterBox.addComponent(constructTitleBarForFilterBox(controller));

        ArrayList<OptionGroup> allGroups = new ArrayList<>();
        allGroups.addAll(controller.dynamicFilters.getOptionGroups());

        // 3.Add Option groups to layout and on change of value of checkbox, trigger filtering
        for (OptionGroup optionGroup : allGroups) {
            filterBox.addComponents(optionGroup);
            optionGroup.addValueChangeListener(event -> controller.applyDynamicFilters());
        } // end for
          // take up remaining space in end
        CustomComponent space = new CustomComponent();
        filterBox.addComponent(space);
        filterBox.setExpandRatio(space, 1f);

        return filterBox;
    }

    private Component constructTitleBarForFilterBox(final FilterController controller) {
        HorizontalLayout titleBar = new HorizontalLayout();
        titleBar.setStyleName("leos-filter-header");
        Label filterCaption = new Label(messageHelper.getMessage("repository.caption.filters"));
        titleBar.addComponent(filterCaption);
        titleBar.setExpandRatio(filterCaption, .75f);

        Button resetButton = new Button(messageHelper.getMessage("repository.caption.filters.reset"));
        resetButton.setPrimaryStyleName("icon-only");
        resetButton.addStyleName("link reset-button");
        resetButton.setDescription(messageHelper.getMessage("repository.caption.filters.reset"));
        titleBar.addComponent(resetButton);
        titleBar.setComponentAlignment(resetButton,Alignment.TOP_RIGHT);
        titleBar.setExpandRatio(resetButton,.25f);
        resetButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                controller.resetFilter();
            }
        });
        titleBar.setWidth("100%");
        return titleBar;
    }

    private Component initSearchBox(TextField searchBox, final FilterController controller) {
        searchBox.setInputPrompt(messageHelper.getMessage("repository.filter.search.prompt"));
        controller.applyTextFilter(searchBox.getValue());
        searchBox.addTextChangeListener(event -> controller.applyTextFilter(event.getText()));
        return searchBox;
    }

    /** behaviour part**********************************************************/
    class FilterController {
        private final String STRING_SEARCH_PROPERTY = "title";
        private DynamicFilters dynamicFilters;
        private BeanContainer<String, CARD_DATA> container;

        private FilterController(BeanContainer<String, CARD_DATA> container, Map<String, Converter> propertyConverterMap, User user) {
            this.container = container;
            dynamicFilters = new DynamicFilters(messageHelper, propertyConverterMap, container);
        }

        void resetFilter() {
            // reset All Filters by selecting All checkBoxes
            for (OptionGroup optionGroup : dynamicFilters.getOptionGroups()) {
                for (Object itemId : optionGroup.getItemIds()) {
                    optionGroup.select(itemId);
                }
            }
        }

        // Trigger filtering
        void applyDynamicFilters() {
            if (!dynamicFilters.isUpdating()) {// to avoid cascade of events when dynamic filters are updating
                // removing same filter if already applied and apply again
                if (container.getContainerFilters().contains(dynamicFilters)) {
                    container.removeContainerFilter(dynamicFilters);
                }
                container.addContainerFilter(dynamicFilters);
            }
        }

        void applyTextFilter(String searchString) {
            for (Container.Filter filter : container.getContainerFilters()) {
                if (filter.appliesToProperty(STRING_SEARCH_PROPERTY)) {
                    container.removeContainerFilter(filter);
                    break;
                }
            }
            if (searchString != null && searchString.length() > 0) {
                container.addContainerFilter(new SimpleStringFilter(STRING_SEARCH_PROPERTY, searchString, true, false));
            }
        }

    }

}
