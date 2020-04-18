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
package eu.europa.ec.leos.ui.view.workspace;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.*;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.filter.QueryFilter;
import eu.europa.ec.leos.permissions.Role;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.LeosPermissionAuthorityMap;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.event.view.repository.RefreshDisplayedListEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static eu.europa.ec.leos.model.filter.QueryFilter.FilterType;

@ViewScope
@SpringComponent
class WorkspaceOptions extends VerticalLayout {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceOptions.class);
    private static final List<Integer> LEVELS_TO_DISPLAY = Arrays.asList(2, 3, 4);
    private static final Map<Integer, FilterType> levelKind = new HashMap<>();

    static {
        //from catalog
        levelKind.put(0, FilterType.Root);             //unused
        levelKind.put(1, FilterType.actType);          //ex- legal acts
        levelKind.put(2, FilterType.procedureType);    //ex- olp
        levelKind.put(3, FilterType.docType);          //ex- reg,dir,
        levelKind.put(4, FilterType.template);         //ex- SJ-023
        levelKind.put(5, FilterType.docTemplate);      //Unused but reserved

        //non catalog
        levelKind.put(6, FilterType.category);         //ex- PROPOSAL/BILL/ANEX/MEMO
        levelKind.put(7, FilterType.language);         //ex- EN/FR
        levelKind.put(8, FilterType.role);             //John::OWNER
    }

    private final MessageHelper messageHelper;
    private final EventBus eventBus;
    private final SecurityContext securityContext;
    private Map<FilterType, List> optionsData = new HashMap<>();
    private QueryFilter workspaceFilter = new QueryFilter();
    private List<CheckBoxGroup> optionGroups = new ArrayList<>();
    private LeosPermissionAuthorityMap authorityMap;

    WorkspaceOptions(MessageHelper messageHelper,
                     EventBus eventBus,
                     LeosPermissionAuthorityMap authorityMap,
                     SecurityContext securityContext) {
        //this.searchBox = searchBox;
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.authorityMap = authorityMap;
        this.securityContext = securityContext;
    }

    @Override
    public void attach() {
        super.attach();
        LOG.trace("Attaching filters ...");
    }

    @Override
    public void detach() {
        super.detach();
        LOG.trace("Detaching filters ...");
    }

    void intializeOptions(List<CatalogItem> catalogItems) {
        if (optionsData.size() == 0) {
            preProcessCatalog(catalogItems, 0, optionsData);
            createCheckBoxFromCatalog(optionsData);
            initSortOrder();
        }
    }

    //This method BFS the tree to gather nodes of same depth in lists per depth level
    private void preProcessCatalog(List<CatalogItem> catalogItems, int level, Map<FilterType, List> optionsData) {
        List<CatalogItem> list = optionsData.size() <= level ? new ArrayList<>() : optionsData.get(levelKind.get(level));
        for (CatalogItem item : catalogItems) {
            //if(item.isEnabled())
            if(optionsData.get(levelKind.get(level))== null || !optionsData.get(levelKind.get(level)).contains(item)) {
                list.add(item);
            }
        }
        if (optionsData.size() <= level) {
            optionsData.put(levelKind.get(level), list);
        }
        for (CatalogItem item : list) {
            preProcessCatalog(item.getItems(), level + 1, optionsData);
        }
    }

    void createCheckBoxFromCatalog(Map<FilterType, List> optionsData) {

        for (int i = 0; i < optionsData.size(); i++) {
            if (!LEVELS_TO_DISPLAY.contains(i)) {
                continue;
            }

            List<CatalogItem> options = optionsData.get(levelKind.get(i));
            CheckBoxGroup<CatalogItem> optionGroup = new CheckBoxGroup<>();
            optionGroup.setWidth("100%");
            optionGroup.addStyleName("left"); //to move checkbox to right

            optionGroup.setCaption(messageHelper.getMessage("repository.caption.filters." + i));
            optionGroup.setItemDescriptionGenerator(catalogItem -> catalogItem.getDescription("EN"));//we can fetch language of the user
            optionGroup.setItemCaptionGenerator(catalogItem -> catalogItem.getName("EN"));
            optionGroup.setDataProvider(new ListDataProvider<>(options));
            optionGroup.setId(levelKind.get(i).name());

            optionGroup.addSelectionListener(event -> {
                        boolean nullCheck = false;
                        String id = event.getComponent().getId();
                        // in order to make this new fields compatible with the existing proposals
                        if (id.equalsIgnoreCase(FilterType.procedureType.name()) || id.equalsIgnoreCase(FilterType.actType.name())){
                            nullCheck = true;
                        }

                List<String> values = event.getAllSelectedItems().stream()
                        .map(CatalogItem::getKey)
                        .collect(Collectors.toList());
                        workspaceFilter.removeFilter(id);
                        workspaceFilter.addFilter(new QueryFilter.Filter(id, "IN", nullCheck,
                               values.toArray(new String[]{})) );
                        if (event.isUserOriginated()) {
                            eventBus.post(new RefreshDisplayedListEvent());
                        }
                    }
            );

            optionGroup.select(options.toArray(new CatalogItem[]{}));
            optionGroups.add(optionGroup);
            this.addComponent(optionGroup);
        }

        CheckBoxGroup<Role> rolesOptions = createRoleOptions();
        this.addComponent(rolesOptions);
        optionGroups.add(rolesOptions);

        // take up remaining space in end
        CustomComponent space = new CustomComponent();
        this.addComponentsAndExpand(space);
        this.setExpandRatio(space, 1f);
    }

    private CheckBoxGroup<Role> createRoleOptions() {
        CheckBoxGroup<Role> optionGroup = new CheckBoxGroup<>();
        optionGroup.setWidth("100%");
        optionGroup.addStyleName("left"); //to move checkbox to right

        optionGroup.setCaption(messageHelper.getMessage("repository.caption.filters.roles"));

        optionGroup.setItemDescriptionGenerator(role -> messageHelper.getMessage("repository.caption.filters.roles"));//we can fetch language of the user
        optionGroup.setItemCaptionGenerator(role -> messageHelper.getMessage(role.getMessageKey()));//Can use message helper but not needed
        List<Role> appRoles = authorityMap.getAllRoles().stream()
                .filter(Role::isApplicationRole)
                .collect(Collectors.toList());
        optionGroup.setId(FilterType.role.name());
        optionGroup.addSelectionListener(event -> {
                    String id = event.getComponent().getId();
                    workspaceFilter.removeFilter(id);
                    boolean appRoleSelected = event.getAllSelectedItems().stream()
                            .anyMatch(role ->  appRoles.contains(role));

                    if (!appRoleSelected) {
                        workspaceFilter.addFilter(new QueryFilter.Filter(id, "IN",false,
                                event.getAllSelectedItems().stream()
                                        .map(role -> securityContext.getUser().getLogin() + "::" + role.getName())
                                        .collect(Collectors.toList()).toArray(new String[]{})));
                    }

                    if (event.isUserOriginated()) {
                        eventBus.post(new RefreshDisplayedListEvent());
                    }
                }
        );
        List<Role> roles = authorityMap.getAllRoles().stream()
                    .filter(Role::isCollaborator)
                    .collect(Collectors.toList());
        if (securityContext.hasPermission(null, LeosPermission.CAN_SEE_ALL_DOCUMENTS)) {
            roles.addAll(appRoles);
        }
        optionGroup.setDataProvider(new ListDataProvider<>(roles));
        optionGroup.select(roles.toArray(new Role[]{}));
        optionsData.put(FilterType.role, roles);

        return optionGroup;
    }

    //unchecked
    void resetFilters(Button.ClickEvent event) {
        //select all options
        workspaceFilter.removeAllFilters();
        optionGroups.forEach(optionGroup -> {
                    optionGroup.deselectAll();
                    optionGroup.select(((ListDataProvider) optionGroup.getDataProvider()).getItems().toArray());
                }
        );
        eventBus.post(new RefreshDisplayedListEvent());
    }

    /* this will intialize seach box and attach the value listener */
    Component initializeSearchBox(TextField searchBox) {
        searchBox.setPlaceholder(messageHelper.getMessage("repository.filter.search.prompt"));
        searchBox.setValueChangeMode(ValueChangeMode.LAZY);

        searchBox.addValueChangeListener(event -> {
            workspaceFilter.removeFilter(FilterType.title.name());
            if (!searchBox.getValue().isEmpty()) {
                workspaceFilter.addFilter(new QueryFilter.Filter(FilterType.title.name(),
                        "LIKE",false,
                        "%" + searchBox.getValue() + "%"));// Not escaping %/. are taken as matching chars
            }
            eventBus.post(new RefreshDisplayedListEvent());
        });
        return searchBox;
    }

    QueryFilter getQueryFilter() {
        return workspaceFilter;
    }

    private void initSortOrder(){
            workspaceFilter.removeSortOrder(QueryFilter.FilterType.lastModificationDate.name());
            workspaceFilter.addSortOrder(
                    new QueryFilter.SortOrder(QueryFilter.FilterType.lastModificationDate.name(), QueryFilter.SORT_DESCENDING));
    }

    void setTitleSortOrder(boolean sortOrder) {
        workspaceFilter.removeSortOrder(QueryFilter.FilterType.lastModificationDate.name());
        workspaceFilter.addSortOrder(
                new QueryFilter.SortOrder(QueryFilter.FilterType.lastModificationDate.name(),
                        sortOrder ? QueryFilter.SORT_ASCENDING : QueryFilter.SORT_DESCENDING));
        eventBus.post(new RefreshDisplayedListEvent());
    }
}