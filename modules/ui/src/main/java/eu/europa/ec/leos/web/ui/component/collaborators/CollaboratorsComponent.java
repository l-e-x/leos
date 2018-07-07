/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.web.ui.component.collaborators;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.data.Container;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.data.util.BeanItemContainer;
import com.vaadin.v7.data.util.GeneratedPropertyContainer;
import com.vaadin.v7.data.util.PropertyValueGenerator;
import com.vaadin.v7.shared.ui.grid.HeightMode;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.Grid;
import de.datenhahn.vaadin.componentrenderer.ComponentRenderer;
import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.web.event.view.proposal.EditCollaboratorRequest;
import eu.europa.ec.leos.web.event.view.proposal.RemoveCollaboratorRequest;
import eu.europa.ec.leos.web.model.CollaboratorVO;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.converter.UserDisplayConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

/* This class contain collaborators to be shown as a grid
    1) In place editing of role. This is done using componentRenderer add-on which allows value of grid column to be represented as components
        This is unbuffered
    2) Allows a new Collaborator to be added using Grid Editor(Buffered to allow use of save or cancel).
       We allow editing of two columns user and role.
       A) For role editing, we use same componentRenderer field. This is unbuffered so we need to handle the buffering.
       B) For user editing and search, we use a custom combobox implementation which is buffered.

       To control that only a new item can be edited, we use GridWithEditorListener.
 */
@SpringComponent
@ViewScope
public class CollaboratorsComponent extends CustomComponent {
    private static final Logger LOG = LoggerFactory.getLogger(CollaboratorsComponent.class);
    private static final EnumSet<LeosAuthority> selectableAuthorities = EnumSet.of(LeosAuthority.CONTRIBUTOR, LeosAuthority.OWNER);

    enum COLUMN {
        NAME("user"),
        DG("user.dg"),
        AUTHORITY("leosAuthority"),
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

    private GridWithEditorListener collaboratorGrid;
    private CollaboratorEditor editor;
    private MessageHelper messageHelper;
    private EventBus eventBus;

    @Autowired
    public CollaboratorsComponent(MessageHelper messageHelper, EventBus eventBus) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        initGrid();
    }

    public void populateData(List<CollaboratorVO> collaborators) {
        Container container = collaboratorGrid.getContainerDataSource();
        container.removeAllItems(); // Refresh all
        if (collaborators != null) {
            collaborators.forEach(container::addItem);
        }
    }

    private void initGrid() {
        collaboratorGrid = new GridWithEditorListener();

        Container.Indexed container = createDataContainer();
        collaboratorGrid.setContainerDataSource(container);
        collaboratorGrid.setSelectionMode(Grid.SelectionMode.NONE);

        collaboratorGrid.setColumns(COLUMN.getKeys());// Explicitly restricting the visible columns

        collaboratorGrid.getColumn(COLUMN.NAME.getKey()).setExpandRatio(2).setConverter(new UserDisplayConverter());
        collaboratorGrid.getColumn(COLUMN.DG.getKey()).setExpandRatio(1);
        collaboratorGrid.getColumn(COLUMN.AUTHORITY.getKey()).setExpandRatio(1).setRenderer(new ComponentRenderer());
        collaboratorGrid.getColumn(COLUMN.ACTION.getKey()).setExpandRatio(0).setRenderer(new ComponentRenderer());

        Grid.HeaderRow mainHeader = collaboratorGrid.getDefaultHeaderRow();
        for (Grid.Column col : collaboratorGrid.getColumns()) {
            mainHeader.getCell(col.getPropertyId()).setHtml(messageHelper.getMessage("collaborator.header.column." + col.getPropertyId()));
        }

        collaboratorGrid.setHeightMode(HeightMode.CSS);
        collaboratorGrid.setSizeFull();
        setCompositionRoot(collaboratorGrid);
    }

    private Container.Indexed createDataContainer() {
        // Initialize Containers
        BeanItemContainer dataContainer = new BeanItemContainer<>(CollaboratorVO.class);
        dataContainer.addNestedContainerProperty(COLUMN.DG.getKey());

        GeneratedPropertyContainer generatedPropertyContainer = new GeneratedPropertyContainer(dataContainer);
        generatedPropertyContainer.addGeneratedProperty(COLUMN.ACTION.getKey(), new PropertyValueGenerator<Component>() {
            @Override
            public Component getValue(Item item, Object itemId, Object propertyId) {
                return createDeleteButton((CollaboratorVO) itemId);
            }

            @Override
            public Class<Component> getType() {
                return Component.class;
            }
        });

        // Override authority column with a generated property for editing requires a component
        generatedPropertyContainer.addGeneratedProperty(COLUMN.AUTHORITY.getKey(), new PropertyValueGenerator<Component>() {
            @Override
            public Component getValue(Item item, Object itemId, Object propertyId) {
                return createInplaceAuthorityEditor(item, (CollaboratorVO) itemId, propertyId);
            }

            @Override
            public Class<Component> getType() {
                return Component.class;
            }
        });

        return generatedPropertyContainer;
    }

    private Button createDeleteButton(CollaboratorVO collaborator) {
        Button deleteButton = new Button();
        deleteButton.setPrimaryStyleName(ValoTheme.BUTTON_ICON_ONLY);
        deleteButton.setIcon(FontAwesome.MINUS_CIRCLE);
        deleteButton.addStyleName("delete-button");
        deleteButton.addClickListener(event -> {
            collaboratorGrid.getContainerDataSource().removeItem(collaborator);// FIXME: remove first and fire or fire and remove in another update
                eventBus.post(new RemoveCollaboratorRequest(collaborator));
            });
        return deleteButton;
    }

    // Combo box used for the Editing LeosAuthority field
    private ComboBox createInplaceAuthorityEditor(Item item, CollaboratorVO collaborator, Object propertyId) {
        ComboBox comboBox = new ComboBox();
        for (LeosAuthority authority : selectableAuthorities) {
            comboBox.addItem(authority);
        }
        comboBox.addStyleName("role-editor");
        comboBox.setPropertyDataSource(item.getItemProperty(propertyId));
        comboBox.setNullSelectionAllowed(false);
        comboBox.addValueChangeListener(event -> {
            // this will be called if value is changed from editor before save is pressed
            // so need to ignore values if request is originated from editor. This should be handled via commit event(save).
            if (!collaboratorGrid.isEditorActive()) {
                eventBus.post(new EditCollaboratorRequest(collaborator));
            }
        });
        return comboBox;
    }

    public void addCollaborator() {
        if (editor == null) {
            editor = new CollaboratorEditor(collaboratorGrid, messageHelper, eventBus);
        }
        editor.addCollaborator();
    }

    @Override
    public void setEnabled(boolean enabled){
        collaboratorGrid.getColumn(CollaboratorsComponent.COLUMN.ACTION.getKey()).setHidden(!enabled);//hide if disabled
        collaboratorGrid.setEnabled(enabled);
    }
}
