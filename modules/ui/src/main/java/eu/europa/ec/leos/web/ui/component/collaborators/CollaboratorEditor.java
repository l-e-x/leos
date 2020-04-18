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
import com.vaadin.v7.data.fieldgroup.FieldGroup;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.Grid;
import com.vaadin.v7.ui.TextField;
import de.datenhahn.vaadin.componentrenderer.grid.editor.ComponentCustomField;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.permissions.Role;
import eu.europa.ec.leos.security.LeosPermissionAuthorityMapHelper;
import eu.europa.ec.leos.ui.event.view.collection.AddCollaboratorRequest;
import eu.europa.ec.leos.web.model.CollaboratorVO;
import eu.europa.ec.leos.web.model.UserVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.collaborators.CollaboratorsComponent.COLUMN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/* This class is controlling the editing function for the collaborator editor */
class CollaboratorEditor {
    private static final Logger LOG = LoggerFactory.getLogger(CollaboratorEditor.class);

    private GridWithEditorListener collaboratorGrid;
    private UserSearchComponent userSearchComponent;
    private TextField entityEditor;
    private MessageHelper messageHelper;
    private EventBus eventBus;
    private String documentURL;
    private LeosPermissionAuthorityMapHelper authorityMapHelper;

    CollaboratorEditor(GridWithEditorListener collaboratorGrid, MessageHelper messageHelper, EventBus eventBus, String documentURL, LeosPermissionAuthorityMapHelper authorityMapHelper) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.collaboratorGrid = collaboratorGrid;
        this.documentURL = documentURL;
        this.authorityMapHelper = authorityMapHelper;
        initGridEditor();
    }

    private void initGridEditor() {
        collaboratorGrid.getColumn(COLUMN.NAME.getKey()).setEditorField(getUserEditor()).setEditable(true);
        // CustomField is Combobox. which contains role value
        collaboratorGrid.getColumn(COLUMN.AUTHORITY.getKey()).setEditorField(new ComponentCustomField()).setEditable(true);
        collaboratorGrid.getColumn(COLUMN.ENTITY.getKey()).setEditorField(getEntityEditor()).setEditable(true);
        collaboratorGrid.getColumn(COLUMN.ACTION.getKey()).setEditable(false);

        // setting min space text as Vaadin implementation doesnt allow setting html here. So will generate icons in css
        collaboratorGrid.setEditorSaveCaption(".");
        collaboratorGrid.setEditorCancelCaption(".");

        collaboratorGrid.addEditorListener(new CollaboratorEditorListener());
        collaboratorGrid.getEditorFieldGroup().addCommitHandler(new CollaboratorCommitHandler());
    }

    void addCollaborator() {
        //preEditOperations();
        // a random temp collaborator is generated which will be added and then edited in grid editor
        CollaboratorVO tempObject = new CollaboratorVO(new UserVO(new User(0l, UUID.randomUUID().toString(), null, null, null,null)), authorityMapHelper.getCollaboratorRoles().get(0));
        collaboratorGrid.getContainerDataSource().addItemAt(0, tempObject); // Add at first position
        collaboratorGrid.editItem(tempObject);
    }

    //This field is used only for displaying value. This should be not editable.
    private TextField getEntityEditor() {
        entityEditor = new TextField();
        entityEditor.setNullRepresentation("-");
        entityEditor.setEnabled(false);
        entityEditor.setValidationVisible(false);
        return entityEditor;
    }

    private UserSearchComponent getUserEditor() {
        userSearchComponent = new UserSearchComponent(messageHelper, eventBus);
        userSearchComponent.addValueChangeListener(event -> updateEntityEditor((UserVO) event.getProperty().getValue()));
        return userSearchComponent;
    }

    private void updateEntityEditor(UserVO selectedUser) {
        entityEditor.setValue((selectedUser == null) ? null : selectedUser.getEntity());
    }

    private List<UserVO> getExistingUsers() {
        return collaboratorGrid.getContainerDataSource().getItemIds()
                .stream()
                .map(item -> ((CollaboratorVO) item).getUser())
                .collect(Collectors.toList());
    }

    private class CollaboratorCommitHandler implements FieldGroup.CommitHandler {
        @Override
        public void preCommit(FieldGroup.CommitEvent commitEvent) throws FieldGroup.CommitException {
            // Editor show messages in proper format only when they are generated by individual field validator.
            // if we throw exception from here. it causes a generic message
            userSearchComponent.setValidationVisible(true);
        }

        @Override
        public void postCommit(FieldGroup.CommitEvent commitEvent) throws FieldGroup.CommitException {
            try {
                UserVO user = (UserVO) commitEvent.getFieldBinder().getField(COLUMN.NAME.getKey()).getValue();
                ComboBox customEditorField = (ComboBox) commitEvent.getFieldBinder().getField(COLUMN.AUTHORITY.getKey()).getValue();
                Role role = (Role) customEditorField.getValue();
                CollaboratorVO collaborator = new CollaboratorVO(user, role);// temp object is not right object to use as it contains dummy values
                eventBus.post(new AddCollaboratorRequest(collaborator, documentURL));
                collaboratorGrid.getContainerDataSource().addItemAt(0, collaborator);// FIXME: this should be done when save is done
            } finally {
                userSearchComponent.setValidationVisible(false);
            }
        }
    }
    
    private class CollaboratorEditorListener implements Grid.EditorListener {
        @Override
        public void editorOpened(Grid.EditorOpenEvent e) {
            LOG.trace("Collaborator editor opened");
            userSearchComponent.setExistingUsers(getExistingUsers());
            entityEditor.setEnabled(false);
            userSearchComponent.setValidationVisible(false); // validation is done only at commit time. Else validate fire every time a value change is detected.
            userSearchComponent.focus();
        }

        @Override
        public void editorMoved(Grid.EditorMoveEvent e) {
            // Not possible and not implemented in
        }

        @Override
        public void editorClosed(Grid.EditorCloseEvent e) {
            LOG.trace("Collaborator editor closed");
            collaboratorGrid.getContainerDataSource().removeItem(e.getItem()); // removing tempObject
        }
    }
}
