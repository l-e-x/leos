/**
 * Copyright 2015 European Commission
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
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;

import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.event.view.document.SaveRecitalsRequestEvent;
import eu.europa.ec.leos.web.event.window.CloseRecitalsEditorEvent;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.CKEditorComponent;


public class EditRecitalsWindow extends AbstractEditChangeMonitorWindow {
    private static final long serialVersionUID = 2324679729171812974L;

    private CKEditorComponent ckEditor;
    private String recitalsId;
    private  final String EDITOR_NAME = "leosAknRecitalsEditor";
    private  final String PROFILE_ID = "aknRecitals";
    
    public EditRecitalsWindow(MessageHelper messageHelper, EventBus eventBus, String recitalsId, String recitalsContentData, ConfigurationHelper cfgHelper) {
        super(messageHelper, eventBus);

        setWidth(880, Unit.PIXELS);
        setHeight(685, Unit.PIXELS);
        setCaption(messageHelper.getMessage("edit.recitals.window.title"));
        addButtonOnLeft(buildDapButton(cfgHelper.getProperty("leos.dap.edit.recitals.url")));
        
        ckEditor = new CKEditorComponent(PROFILE_ID , EDITOR_NAME,recitalsContentData);
        setBodyComponent(ckEditor);
        
        addCKEditorListeners();
        this.recitalsId=recitalsId;
    }

    @Override
    protected void onSave() {
        ckEditor.actionDone(CKEditorComponent.SAVE);
    }

    @Override
    public void close() {
        ckEditor.actionDone(CKEditorComponent.CLOSE);
        
    }
    
    public void updateContent(String newContent) {
        ckEditor.setContent(newContent);
    }
    
    private void addCKEditorListeners(){
        
        ckEditor.addChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                enableSave();                
            }
        });
        
        ckEditor.addSaveListener(new CKEditorComponent.SaveListener() {
            @Override
            public void saveClick(String content) {
                eventBus.post(new SaveRecitalsRequestEvent(recitalsId, content));
                EditRecitalsWindow.super.onSave();
            }
        });                       
         
        ckEditor.addCloseListener(new CKEditorComponent.CloseListener() {
            @Override
            public void close() {
                eventBus.post(new CloseRecitalsEditorEvent(recitalsId));
                eventBus.post(new RefreshDocumentEvent());
                EditRecitalsWindow.super.close();
            }
        });
    }
}
