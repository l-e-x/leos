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
package eu.europa.ec.leos.web.view.subView;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.filter.And;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.event.FieldEvents;
import com.vaadin.ui.*;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.vo.UserVO;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.card.DynamicFilters;
import eu.europa.ec.leos.web.ui.component.card.RoleFilter;
import eu.europa.ec.leos.web.ui.component.documentCard.LangCodeToDescriptionConverter;
import eu.europa.ec.leos.web.ui.converter.StageValueConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.resetbuttonfortextfield.ResetButtonForTextField;
import org.vaadin.teemu.VaadinIcons;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class FilterSubView<CARD_DATA> implements LeosSubView {
    private MessageHelper messageHelper;
    private LanguageHelper langHelper;
    private EventBus eventBus;

    private VerticalLayout filterBox;
    private Component searchBox;
    private static final Logger LOG = LoggerFactory.getLogger(FilterSubView.class);

    public FilterSubView(MessageHelper messageHelper, LanguageHelper langHelper, EventBus eventBus, BeanContainer<String, CARD_DATA> container, User user) {
        this.messageHelper = messageHelper;
        this.langHelper = langHelper;
        this.eventBus = eventBus;

        FilterSubViewController controller = constructController(container, user);
        filterBox = contructFilterBox(controller);
        searchBox = contructSearchBox(controller);
    }

    private FilterSubViewController constructController(BeanContainer<String, CARD_DATA> container, User user) {
        return new FilterSubViewController(container, user);
    }

    private VerticalLayout contructFilterBox(final FilterSubViewController controller) {
        VerticalLayout filterLayout = new VerticalLayout();
        filterLayout.setSizeFull();
        filterLayout.setStyleName("leos-card-filters");
        // 1. create Top title
        filterLayout.addComponent(constructTitleBarForFilterBox(controller));

        ArrayList<OptionGroup> allGroups = new ArrayList<>();
        allGroups.addAll(controller.dynamicFilters.getOptionGroups());
        allGroups.add(controller.roleFilter.getOptionGroup());

        // 3.Add Option groups to layout and on change of value of checkbox, trigger filtering
        for (OptionGroup optionGroup : allGroups) {
            filterLayout.addComponents(optionGroup);
            optionGroup.addValueChangeListener(new Property.ValueChangeListener() {
                @Override
                public void valueChange(Property.ValueChangeEvent event) {
                    controller.triggerFiltering();
                }
            });
        } // end for
        //take up remaining space in end
        CustomComponent space=new CustomComponent();
        filterLayout.addComponent(space);
        filterLayout.setExpandRatio(space,1f);

        return filterLayout;
    }

    private Component constructTitleBarForFilterBox(final FilterSubViewController controller) {
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

    private Component contructSearchBox(final FilterSubViewController controller) {
        TextField searchBox = new TextField();
        ResetButtonForTextField.extend(searchBox);// add a reset button inside search box
        searchBox.setIcon(VaadinIcons.SEARCH);
        searchBox.addStyleName("inline-icon");
        //searchBox.addStyleName("leos-search-field"); //TODO : fix as it was commented cause reset button to mis align
        searchBox.setInputPrompt(messageHelper.getMessage("repository.filter.search.prompt"));
        searchBox.setImmediate(true);
        searchBox.setWidth("90%");
        searchBox.addTextChangeListener(new FieldEvents.TextChangeListener() {
            @Override
            public void textChange(FieldEvents.TextChangeEvent event) {
                String filterString = event.getText();
                controller.applyTextFilter(filterString);
            }
        });
        return searchBox;
    }

    @Override
    public Component getUIComponent() {
        return filterBox;
    }

    public Component getSearchBox() {
        return searchBox;
    }

    /** behaviour part**********************************************************/
    class FilterSubViewController {
        private final String STRING_SEARCH_PROPERTY = "title";
        private Container.Filter combinedFilter;
        private DynamicFilters dynamicFilters;
        private RoleFilter roleFilter;
        private BeanContainer<String, CARD_DATA> container;

        private FilterSubViewController(BeanContainer<String, CARD_DATA> container, User user) {
            this.container = container;

            Map<String, Converter> propertyConverterMap = new LinkedHashMap<>();
            propertyConverterMap.put("stage", new StageValueConverter(messageHelper));
            propertyConverterMap.put("template", null);
            propertyConverterMap.put("language", new LangCodeToDescriptionConverter(langHelper));

            dynamicFilters = new DynamicFilters(messageHelper, propertyConverterMap, container);
            roleFilter = new RoleFilter(messageHelper, container, new UserVO(user));
            combinedFilter = new And(dynamicFilters, roleFilter);
        }

        public void resetFilter() {
            // reset All Filters by selecting All checkBoxes
            for (OptionGroup optionGroup : dynamicFilters.getOptionGroups()) {
                for (Object itemId : optionGroup.getItemIds()) {
                    optionGroup.select(itemId);
                }
            }
            for (Object itemId : roleFilter.getOptionGroup().getItemIds()) {
                roleFilter.getOptionGroup().select(itemId);
            }
        }

        // Trigger filtering
        public void triggerFiltering() {
            if (!dynamicFilters.isUpdating()) {// to avoid cascade of events when dynamic filters are updating
                // removing same filter if already applied and apply again
                if (container.getContainerFilters().contains(combinedFilter)) {
                    container.removeContainerFilter(combinedFilter);
                }
                container.addContainerFilter(combinedFilter);
            }
        }

        public void applyTextFilter(String searchString) {
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
