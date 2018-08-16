/*
 * Copyright 2018 European Commission
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

import com.vaadin.v7.data.fieldgroup.FieldGroup;
import com.vaadin.v7.ui.Grid;

import java.util.LinkedHashSet;

/*  This class controls that a temp row being edited is inserted at position 0 and on cancellation cleanup is performed
    it also ensures that editor becomes editable only for a single row and user is not able to edit any other row via editor.
*/
public class GridWithEditorListener extends Grid {

    private LinkedHashSet<EditorListener> listenerList = new LinkedHashSet<>();
    private Object elementBeingEdited;

    @Override
    public void doCancelEditor() {
        //this method is also called after save :(. So we need to handle both conditions(save and cancel)
        super.doCancelEditor();
        setEditorEnabled(false);
        listenerList.forEach(listener -> listener.editorClosed(new EditorCloseEvent(this, elementBeingEdited)));
        markAsDirty();
    }

    @Override
    public void saveEditor() throws FieldGroup.CommitException {
        super.saveEditor();
        //do anything else required
    }

    @Override
    public void editItem(Object object) {
        if (getContainerDataSource().indexOfId(object) >= 0 && !isEditorActive()) { // editor should not be already active
            setEditorEnabled(true);
            getEditorFieldGroup().clear();
            super.editItem(object);
            elementBeingEdited = getEditedItemId();
        }
    }

    //Editor actually opened
    @Override
    protected void doEditItem() {
        super.doEditItem();
        listenerList.forEach(listener -> listener.editorOpened(new EditorOpenEvent(this, elementBeingEdited)));
    }

    public void addEditorListener(EditorListener listener) {
        listenerList.add(listener);
    }

    public void removeEditorListener(EditorListener listener) {
        listenerList.remove(listener);
    }
}
