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
package eu.europa.ec.leos.web.ui.component;

import java.io.Serializable;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.annotations.JavaScript;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;

import eu.europa.ec.leos.web.support.LeosCacheToken;

@JavaScript({"vaadin://js/web/connector/ckEditorConnector.js" + LeosCacheToken.TOKEN })
public class CKEditorComponent extends AbstractJavaScriptComponent {

    public static final String SAVE= "save";
    public static final String CLOSE= "close";

    private static final Logger LOG = LoggerFactory.getLogger(CKEditorComponent.class);
    private static final long serialVersionUID = 3751840217577800033L;
    private LinkedList<ValueChangeListener> changeListeners = new LinkedList<ValueChangeListener>();
    private LinkedList<SaveListener> saveListeners = new LinkedList<SaveListener>();
    private LinkedList<CloseListener> closeListeners = new LinkedList<CloseListener>();

    private CKEditorState cachedState=null;

    public CKEditorComponent(final String profileId, final String editorName, String content){
        setProfileId(profileId);
        setEditorName(editorName);
        setContent(content);
        addClientToServerListeners();
    }

    /** these functions will be present in the AbstractJavaScriptComponent at client side 
     * and any call to them will be propagated to below functions
     */
    private void addClientToServerListeners(){

        addFunction("onAction", new JavaScriptFunction() {
            private static final long serialVersionUID = -2199710366817990634L;
            @Override
            public void call(JSONArray arguments) throws JSONException {
                String actionName = arguments.getString(0);
                LOG.debug("Action :{} received from CKEDitor" , actionName);

                switch(actionName){
                    case "onChange":{
                        synchronized(changeListeners) {
                            for( ValueChangeListener listener : changeListeners ) {
                                listener.valueChange(null);
                            }
                        }
                    }
                    break;

                    case "onSave":{
                        String newContent = arguments.getString(1);
                        setContent(newContent);
                        synchronized(saveListeners) {
                            for( SaveListener listener : saveListeners ) {
                                listener.saveClick(newContent);
                            }
                        }
                    }
                    break;

                    case "onClose":{
                        synchronized(closeListeners) {
                            for( CloseListener listener : closeListeners ) {
                                listener.close();
                            }
                        }
                    }
                    break;
                }//end switch
            }

        });
    }

    /**these actions will be sent to CKeditor after current vaadin action is finished (async)
     * after the action is done, CKEditorConnector should invoke action listener with action name to complete the processing
     * Action possible :close, save */    

    public void actionDone(String action){
        callFunction("ckConnector_doAction", action, getEditorName());
    }

    /** any update to State is sent to connector onStateChanged
     * @see com.vaadin.ui.AbstractJavaScriptComponent#getState() */
    @Override
    protected CKEditorState getState() {
        cachedState=(CKEditorState) super.getState();
        return cachedState;
    }

    public void setContent(String content) {
        getState().setContent(content);
    }

    public void setEditorName(String editorName) {
        getState().setEditorName(editorName);
    }

    public void  setProfileId(String profileId) {
        getState().setProfileId(profileId);
    }

    public String getProfileId() {
        return cachedState.getProfileId();
    }

    public String getEditorName() {
        return cachedState.getEditorName();
    }

    public String getContent() {
        return cachedState.getContent();
    }

    public void addChangeListener(ValueChangeListener listener) {
        synchronized(changeListeners) {
            changeListeners.add(listener);
        }
    }
    public void removeChangeListener(ValueChangeListener listener) {
        synchronized(changeListeners) {
            changeListeners.remove(listener);
        }
    }

    public interface SaveListener extends Serializable {
        public void saveClick(String content);
    }

    public void addSaveListener(SaveListener listener) {
        synchronized(saveListeners) {
            saveListeners.add(listener);
        }
    }

    public void removeSaveListener(SaveListener listener) {
        synchronized(saveListeners) {
            saveListeners.remove(listener);
        }
    }

    public interface CloseListener extends Serializable {
        public void close();
    }

    public void addCloseListener(CloseListener closeListener) {
        synchronized(closeListeners) {
            closeListeners.add(closeListener);
        }
    }

    public void removeCloaseListener(CloseListener listener) {
        synchronized(closeListeners) {
            closeListeners.remove(listener);
        }
    }
}
