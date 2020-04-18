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
package eu.europa.ec.leos.ui.component.milestones;

import com.google.common.eventbus.EventBus;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.components.grid.HeaderRow;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.ui.event.FetchMilestoneEvent;
import eu.europa.ec.leos.ui.model.MilestonesVO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Stream;

@SpringComponent
@ViewScope
public class MilestonesComponent extends CustomComponent {
    private static final long serialVersionUID = 8779532739907751262L;

    private Grid<MilestonesVO> milestonesGrid;
    private MessageHelper messageHelper;
    private EventBus eventBus;
    
    enum COLUMN {
        TITLE("title"),
        DATE("date"),
        STATUS("status"),
        ACTION("action");

        private String key;
        private static final String[] keys = Stream.of(values()).map(COLUMN::getKey).toArray(String[]::new);

        COLUMN(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public static String[] getKeys() {
            return keys;
        }
    }

    @Autowired
    public MilestonesComponent(MessageHelper messageHelper, EventBus eventBus) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        initGrid();
    }

    public void populateData(Set<MilestonesVO> milestones) {
        this.getUI().access(() -> {
            milestonesGrid.setItems(milestones);
            milestonesGrid.getDataProvider().refreshAll();
        });
    }

    private void initGrid() {
        milestonesGrid = new Grid<>();

        milestonesGrid.setSelectionMode(Grid.SelectionMode.NONE);
        
        Column<MilestonesVO, Button> titleColumn = milestonesGrid.addComponentColumn(vo -> {
            Button milestoneLink = new Button(vo.getTitle());
            milestoneLink.addStyleName("link");
            milestoneLink.addStyleName("milestone-exp-btn");
            milestoneLink.addClickListener(event -> {
                eventBus.post(new FetchMilestoneEvent(vo.getLegDocumentName(), vo.getTitle()));
            });
            return milestoneLink;
        }).setDescriptionGenerator(MilestonesVO::getTitle);
        Column<MilestonesVO, String> dateColumn = milestonesGrid.addColumn(MilestonesVO::getCreatedDate).setDescriptionGenerator(MilestonesVO::getCreatedDate);
        Column<MilestonesVO, String> statusColumn = milestonesGrid.addColumn(MilestonesVO::getStatus).setDescriptionGenerator(MilestonesVO::getStatus);
        titleColumn.setMaximumWidth(250);
        dateColumn.setMaximumWidth(160);
        statusColumn.setMaximumWidth(150);
        
        HeaderRow mainHeader = milestonesGrid.getDefaultHeaderRow();
        mainHeader.getCell(titleColumn).setHtml(messageHelper.getMessage("milestones.header.column.title"));
        mainHeader.getCell(dateColumn).setHtml(messageHelper.getMessage("milestones.header.column.date"));
        mainHeader.getCell(statusColumn).setHtml(messageHelper.getMessage("milestones.header.column.status"));
        
        milestonesGrid.setHeightMode(HeightMode.ROW);
        milestonesGrid.setWidth(100, Unit.PERCENTAGE);
        setCompositionRoot(milestonesGrid);
    }
}
