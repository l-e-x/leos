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

import com.vaadin.annotations.JavaScript;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;

import elemental.json.JsonArray;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.web.model.TocItemsVO;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@JavaScript({"vaadin://js/web/connector/ckEditorConnector.js" + LeosCacheToken.TOKEN})
public class CKEditorComponent extends AbstractJavaScriptComponent {

    public static final String SAVE = "save";
    public static final String CLOSE = "close";

    private static final Logger LOG = LoggerFactory.getLogger(CKEditorComponent.class);
    private static final long serialVersionUID = 3751840217577800033L;
    private LinkedList<ValueChangeListener> changeListeners = new LinkedList<ValueChangeListener>();
    private LinkedList<SaveListener> saveListeners = new LinkedList<SaveListener>();
    private LinkedList<CloseListener> closeListeners = new LinkedList<CloseListener>();
    private LinkedList<CrossReferenceTocListener> crossReferenceTocListeners = new LinkedList<CrossReferenceTocListener>();
    private List<LoadElementContentListener> loadElementContentListeners = new LinkedList<LoadElementContentListener>();
    private MessageHelper messageHelper;
    private CKEditorState cachedState = null;

    public enum DATA_TYPE {
        EDITOR_CONTENT,
        TOC,
        ELEMENT_CONTENT
    }

    public CKEditorComponent(final String profileId, final String editorName, String content, User user, MessageHelper messageHelper) {
        this.messageHelper = messageHelper;
    	setProfileId(profileId);
        setEditorName(editorName);
        setContent(content);
        setUserInfo(user);
        addClientToServerListeners();
    }

    /** these functions will be present in the AbstractJavaScriptComponent at client side
     * and any call to them will be propagated to below functions
     */
    private void addClientToServerListeners() {

        addFunction("onAction", new JavaScriptFunction() {
            private static final long serialVersionUID = -2199710366817990634L;

            @Override
            public void call(JsonArray arguments) throws JsonException {
                String actionName = arguments.getString(0);
                LOG.debug("Action :{} received from CKEDitor", actionName);

                switch (actionName) {
                    case "onChange": {
                        synchronized (changeListeners) {
                            for (ValueChangeListener listener : changeListeners) {
                                listener.valueChange(null);
                            }
                        }
                    }
                    break;

                    case "onSave": {
                        String newContent = arguments.getString(1);
                        synchronized (saveListeners) {
                            for (SaveListener listener : saveListeners) {
                                listener.saveClick(newContent);
                            }
                        }
                    }
                    break;

                    case "onClose": {
                        synchronized (closeListeners) {
                            for (CloseListener listener : closeListeners) {
                                listener.close();
                            }
                        }
                    }
                    break;

                    case "onLoadToc": {
                    	JsonObject tocEvent = arguments.getObject(1);
                    	String selectedNodeId=(tocEvent.hasKey("selectedNodeId"))?tocEvent.getString("selectedNodeId"):null;
                        synchronized (crossReferenceTocListeners) {
                            for (CrossReferenceTocListener listener : crossReferenceTocListeners) {
                                listener.loadCrossReferenceToc(selectedNodeId);
                            }
                        }
                    }
                    break;

                    case "onLoadElementContent": {
                        JsonObject elementDesc = arguments.getObject(1);
                        synchronized (loadElementContentListeners) {
                            for (LoadElementContentListener listener : loadElementContentListeners) {
                                listener.loadElementContent(elementDesc.getString("elementId"), elementDesc.getString("elementType"));
                            }
                        }
                    }
                    break;

                }// end switch
            }

        });
    }

    /**these actions will be sent to CKeditor after current vaadin action is finished (async)
     * after the action is done, CKEditorConnector should invoke action listener with action name to complete the processing
     * Action possible :close, save */

    public void actionDone(String action) {
        getState().ready = false;
        callFunction("ckConnector_doAction", action, getEditorName());
    }

    /** any update to State is sent to connector onStateChanged
     * @see com.vaadin.ui.AbstractJavaScriptComponent#getState() */
    @Override
    protected CKEditorState getState() {
        cachedState = (CKEditorState) super.getState();
        return cachedState;
    }

    public void setContent(String content) {
        getState().ready = false;
        getState().setContent(content);
        getState().setType(DATA_TYPE.EDITOR_CONTENT.toString());
        getState().ready = true;
    }

    /** this method sends the toc xml to client*/
    public void setCrossReferenceTableOfContent(List<TableOfContentItemVO> tocItemList, List<String> ancestorsIds) {
        LOG.debug("Setting the table of contents...");
        getState().ready = false;
        TocItemsVO tocItemsVO = new TocItemsVO(tocItemList, ancestorsIds, messageHelper);
        getState().setTocItems(tocItemsVO);
        getState().setType(DATA_TYPE.TOC.toString());
        getState().ready = true;
    }

    public void setElementContent(String elementContent) {
    	LOG.debug("Setting the element content...");
        getState().ready = false;
        getState().setElementContent(elementContent);
        getState().setType(DATA_TYPE.ELEMENT_CONTENT.toString());
        getState().ready = true;
    }


    public void setEditorName(String editorName) {
        getState().setEditorName(editorName);
    }

    public void setProfileId(String profileId) {
        getState().setProfileId(profileId);
    }

    public void setUserInfo(User user) {
        getState().setUserLogin(user.getLogin());
        getState().setUserName(user.getName());
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
        synchronized (changeListeners) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(ValueChangeListener listener) {
        synchronized (changeListeners) {
            changeListeners.remove(listener);
        }
    }

    public interface SaveListener extends Serializable {
        public void saveClick(String content);
    }

    public void addSaveListener(SaveListener listener) {
        synchronized (saveListeners) {
            saveListeners.add(listener);
        }
    }

    public void removeSaveListener(SaveListener listener) {
        synchronized (saveListeners) {
            saveListeners.remove(listener);
        }
    }

    public interface CloseListener extends Serializable {
        public void close();
    }

    public void addCloseListener(CloseListener closeListener) {
        synchronized (closeListeners) {
            closeListeners.add(closeListener);
        }
    }

    public void removeCloseListener(CloseListener listener) {
        synchronized (closeListeners) {
            closeListeners.remove(listener);
        }
    }

    public interface CrossReferenceTocListener extends Serializable {
        public void loadCrossReferenceToc(String selectedNodeId);
    }

    public void addCrossReferenceTocListener(CrossReferenceTocListener crossReferenceTocListener) {
        synchronized (crossReferenceTocListener) {
            crossReferenceTocListeners.add(crossReferenceTocListener);
        }
    }

    public void removeCrossReferenceTocListener(CrossReferenceTocListener listener) {
        synchronized (crossReferenceTocListeners) {
            crossReferenceTocListeners.remove(listener);
        }
    }

    public interface LoadElementContentListener {
    	public void loadElementContent(String elementId, String elementType);
  	}

    public void addLoadElementContentListener(LoadElementContentListener listener) {
        synchronized (loadElementContentListeners) {
        	loadElementContentListeners.add(listener);
        }
    }

    public void removeLoadElementContentListener(LoadElementContentListener listener) {
        synchronized (loadElementContentListeners) {
        	loadElementContentListeners.remove(listener);
        }
    }
}
