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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.eventbus.EventBus;

import eu.europa.ec.leos.web.support.i18n.MessageHelper;

public abstract class AbstractEditChangeMonitorWindow extends AbstractEditWindow {

    private static final long serialVersionUID = -1268547140627697834L;
    private final PropertyChangeSupport propertyChangeSupport;
    private final static String AFTER_SAVE_EVENT_NAME = "afterSave";

    protected Boolean dataChanged = false;

    protected AbstractEditChangeMonitorWindow(MessageHelper messageHelper, EventBus eventBus) {
        super(messageHelper, eventBus);
        saveButton.setEnabled(false);
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    protected void enableSave() {
        dataChanged = true;
        saveButton.setEnabled(true);
        saveButton.setDisableOnClick(true);
    }

    @Override
    public void handleCloseButton() {
        if (dataChanged) {
            ConfirmDialog.show(getUI(), messageHelper.getMessage("edit.close.not.saved.title"),
                    messageHelper.getMessage("edit.close.not.saved.message"),
                    messageHelper.getMessage("edit.close.not.saved.confirm"), messageHelper.getMessage("edit.close.not.saved.close"),
                    new ConfirmDialog.Listener() {
                        private static final long serialVersionUID = -1441968814274639475L;

                        public void onClose(ConfirmDialog dialog) {
                            if (dialog.isConfirmed()) {
                                propertyChangeSupport.addPropertyChangeListener(AFTER_SAVE_EVENT_NAME, new PropertyChangeListener() {
                                    @Override
                                    public void propertyChange(PropertyChangeEvent evt) {
                                        close();
                                    }
                                });

                                onSave();
                            } else {
                                close();
                            }
                        }
                    });
        } else {
            close();
        }
    }

    @Override
    protected void onSave() {
        dataChanged = false;
        propertyChangeSupport.firePropertyChange(AFTER_SAVE_EVENT_NAME, null, null);
    }
}
