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

import com.vaadin.shared.ui.JavaScriptComponentState;

import eu.europa.ec.leos.web.model.TocItemsVO;

public class CKEditorState extends JavaScriptComponentState {

    private static final long serialVersionUID = -6274212084055868949L;
    private String content;
    private String profileId; 
    private String editorName;
    private String userLogin;
    private String userName;
    private TocItemsVO tocItems;
    private String type;
    private String elementContent;
    public boolean ready;
    
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getProfileId() {
        return profileId;
    }
    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }
    public String getEditorName() {
        return editorName;
    }
    public void setEditorName(String editorName) {
        this.editorName = editorName;
    }
    public String getUserLogin() {
        return userLogin;
    }
    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public TocItemsVO getTocItems() {
        return tocItems;
    }

    public void setTocItems(TocItemsVO tocItems) {
        this.tocItems = tocItems;
    }
    
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    
	public String getElementContent() {
		return elementContent;
	}
	
	public void setElementContent(String elementContent) {
		this.elementContent = elementContent;
	}
}
