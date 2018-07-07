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
package eu.europa.ec.leos.web.ui.window;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.Container;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.SelectionEvent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.*;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;
import eu.europa.ec.leos.vo.UserVO;
import eu.europa.ec.leos.web.event.view.repository.EditContributorEvent;
import eu.europa.ec.leos.web.model.DocumentVO;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import java.util.*;

public class EditContributorWindow extends AbstractEditWindow {
    final static String COL_NAME = "name";
    final static String COL_LOGIN = "id";
    final static String COL_DG = "dg";

    private BeanItemContainer container;
    private Grid userGrid;
    private DocumentVO docDetails;

    //TODO use correct object inplcase of User VO
    public EditContributorWindow(MessageHelper messageHelper, EventBus eventBus, List<UserVO> allUsers, DocumentVO docDetails) {
        super(messageHelper, eventBus);
        setWidth(650, Unit.PIXELS);//same as create doc wiz size
        setHeight(450, Unit.PIXELS);
        setCaption(messageHelper.getMessage("edit.contributor.window.title"));
        this.docDetails = docDetails;
        container = new BeanItemContainer<>(UserVO.class, allUsers);

        initGrid();
    }

    private void initGrid() {
        userGrid = new Grid(container);
        userGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        customizeColumns();
        setColumnFiltering();
        setSummaryFooter();
        setExistingContributors();
        userGrid.setSizeFull();
        setBodyComponent(userGrid);
    }

    private void customizeColumns() {

        userGrid.removeColumn(COL_DG);

        String[] visibleColumns = {COL_NAME, COL_LOGIN};
        for (String col : visibleColumns) {
            Column column = userGrid.getColumn(col);
            column.setHeaderCaption(messageHelper.getMessage("edit.contributor.window.header." + col));
            column.setHidable(true);
        }
        userGrid.setColumnOrder(visibleColumns);
        userGrid.setColumnReorderingAllowed(true);
    }

    private void setColumnFiltering() {
        userGrid.setFrozenColumnCount(1);
        HeaderRow filteringHeader = userGrid.appendHeaderRow();
        // Add new TextFields to each column which filters the data from
        // that column
        String[] filterColumns = {COL_NAME, COL_LOGIN};
        for (String columnId : filterColumns) {
            TextField filter = getColumnFilter(columnId);
            filteringHeader.getCell(columnId).setComponent(filter);
            filteringHeader.getCell(columnId).setStyleName("filter-header");
        }
    }

    private TextField getColumnFilter(final Object columnId) {
        TextField filter = new TextField();
        filter.setWidth("100%");
        filter.addStyleName(ValoTheme.TEXTFIELD_TINY);
        filter.setInputPrompt("Filter by" + messageHelper.getMessage("edit.contributor.window.header." + columnId));
        filter.addTextChangeListener(new FieldEvents.TextChangeListener() {
            SimpleStringFilter filter = null;

            @Override
            public void textChange(FieldEvents.TextChangeEvent event) {
                Container.Filterable f = (Container.Filterable) userGrid.getContainerDataSource();
                // Remove old filter
                if (filter != null) {
                    f.removeContainerFilter(filter);
                }
                // Set new filter for the column
                filter = new SimpleStringFilter(columnId, event.getText(), true, false);
                f.addContainerFilter(filter);
            }
        });
        return filter;
    }

    private void setSummaryFooter() {
        // Add a summary footer row to the Grid
        final FooterRow footer = userGrid.addFooterRowAt(0);
        footer.join(footer.getCell(COL_NAME), footer.getCell(COL_LOGIN));
        setFooterMessage(footer);
        userGrid.addSelectionListener(new SelectionEvent.SelectionListener() {
            @Override
            public void select(SelectionEvent event) {
                setFooterMessage(footer);
            }
        });
    }

    private void setFooterMessage(FooterRow footer) {
        Collection<Object> selectedObjects = userGrid.getSelectedRows();
        footer.getCell(COL_LOGIN).setHtml(messageHelper.getMessage("edit.contributor.window.footer.message", selectedObjects.size()));
    }

    private void setExistingContributors() {
        MultiSelectionModel selection = (MultiSelectionModel) userGrid.getSelectionModel();
        selection.setSelected(docDetails.getContributors());
    }

    @Override
    protected void onSave() {
        // save the contributors
        Collection<Object> selectedObjects = userGrid.getSelectedRows();
        List<UserVO> updatedList = new ArrayList<>();
        for (Object obj : selectedObjects) {
            updatedList.add((UserVO) obj);
        }
        eventBus.post(new EditContributorEvent(docDetails.getLeosId(), updatedList));
        handleCloseButton();
    }
}
