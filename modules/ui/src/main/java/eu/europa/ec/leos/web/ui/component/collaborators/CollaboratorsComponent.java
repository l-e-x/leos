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
import com.vaadin.v7.data.util.converter.Converter;
import com.vaadin.v7.shared.ui.grid.HeightMode;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.Grid;
import de.datenhahn.vaadin.componentrenderer.ComponentRenderer;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermissionAuthorityMapHelper;
import eu.europa.ec.leos.ui.event.view.collection.EditCollaboratorRequest;
import eu.europa.ec.leos.ui.event.view.collection.RemoveCollaboratorRequest;
import eu.europa.ec.leos.web.model.CollaboratorVO;
import eu.europa.ec.leos.web.support.UrlBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Locale;
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

    @Autowired
    LeosPermissionAuthorityMapHelper authorityMapHelper;

    enum COLUMN {
        NAME("user"),
        ENTITY("user.entity"),
        AUTHORITY("role"),
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
    private UrlBuilder urlBuilder;
    private boolean hasPermission;

    @Autowired
    public CollaboratorsComponent(MessageHelper messageHelper, EventBus eventBus, UrlBuilder urlBuilder) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.urlBuilder= urlBuilder;
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
        collaboratorGrid.getColumn(COLUMN.ENTITY.getKey()).setExpandRatio(1);
        collaboratorGrid.getColumn(COLUMN.AUTHORITY.getKey()).setExpandRatio(1).setRenderer(new ComponentRenderer());
        collaboratorGrid.getColumn(COLUMN.ACTION.getKey()).setExpandRatio(0).setRenderer(new ComponentRenderer());

        Grid.HeaderRow mainHeader = collaboratorGrid.getDefaultHeaderRow();
        for (Grid.Column col : collaboratorGrid.getColumns()) {
            mainHeader.getCell(col.getPropertyId()).setHtml(messageHelper.getMessage("collaborator.header.column." + col.getPropertyId()));
        }

        collaboratorGrid.setHeightMode(HeightMode.ROW);
        collaboratorGrid.setWidth(100, Unit.PERCENTAGE);
        setCompositionRoot(collaboratorGrid);
    }

    private Container.Indexed createDataContainer() {
        // Initialize Containers
        BeanItemContainer dataContainer = new BeanItemContainer<>(CollaboratorVO.class);
        dataContainer.addNestedContainerProperty(COLUMN.ENTITY.getKey());

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
        deleteButton.setDisableOnClick(true);        
        deleteButton.addClickListener(event -> {
                eventBus.post(new RemoveCollaboratorRequest(collaborator, urlBuilder.getDocumentUrl(this.getUI().getPage())));
            });
        return deleteButton;
    }

    // Combo box used for the Editing LeosAuthority field
    private ComboBox createInplaceAuthorityEditor(Item item, CollaboratorVO collaborator, Object propertyId) {
        ComboBox comboBox = new ComboBox();

        authorityMapHelper.getCollaboratorRoles().forEach(authority -> {
            comboBox.addItem(authority);
            comboBox.setItemCaption(authority, messageHelper.getMessage(authority.getMessageKey()));
        });

        comboBox.addStyleName("role-editor");
        comboBox.setPropertyDataSource(item.getItemProperty(propertyId));
        comboBox.setNullSelectionAllowed(false);
        comboBox.addValueChangeListener(event -> {
            // this will be called if value is changed from editor before save is pressed
            // so need to ignore values if request is originated from editor. This should be handled via commit event(save).
            if (!collaboratorGrid.isEditorActive()) {
                eventBus.post(new EditCollaboratorRequest(collaborator, urlBuilder.getDocumentUrl(this.getUI().getPage())));
            }
        });
        comboBox.setEnabled(hasPermission);
        return comboBox;
    }

    public void addCollaborator() {
        if (editor == null) {
            editor = new CollaboratorEditor(collaboratorGrid, messageHelper, eventBus, urlBuilder.getDocumentUrl(this.getUI().getPage()), authorityMapHelper);
        }
        editor.addCollaborator();
    }

    @Override
    public void setEnabled(boolean enabled){
        collaboratorGrid.getColumn(CollaboratorsComponent.COLUMN.ACTION.getKey()).setHidden(!enabled);//hide if disabled
        this.hasPermission = enabled;
    }
    public class UserDisplayConverter implements Converter<String, User> {

        @Override
        public User convertToModel(String value, Class<? extends User> targetType, Locale locale) throws ConversionException {
            throw new ConversionException("Not Implemented Method");
        }

        @Override
        public String convertToPresentation(User value, Class<? extends String> targetType, Locale locale) throws ConversionException {
            return (value != null) ? value.getName() : null;
        }

        @Override
        public Class<User> getModelType() {
            return User.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }
    }

}
