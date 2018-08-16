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
package eu.europa.ec.leos.annotate.model.web.user;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing the entire user profile that can be retrieved
 */
public class JsonUserProfile {

    private String authority;
    private String userid;
    private String status = "okay";

    // saved preferences and group memberships
    private JsonUserShowSideBarPreference preferences;
    private List<JsonGroup> groups;

    // user information, e.g. display name
    private JsonUserInfo user_info;

    // "black box" properties - represent fix objects currently
    private JsonClientFeatures features;
    private JsonFlash flash;

    // -----------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------
    public JsonUserProfile() {
        this.features = new JsonClientFeatures();
        this.flash = new JsonFlash();
        this.groups = new ArrayList<JsonGroup>();
        this.user_info = new JsonUserInfo();
    }

    // -----------------------------------------------------------
    // Comfort getters & setters
    // -----------------------------------------------------------

    public void addGroup(JsonGroup group) {
        if (this.groups == null) {
            this.groups = new ArrayList<JsonGroup>();
        }
        this.groups.add(group);
    }

    public void setDisplayName(String name) {
        if (this.user_info == null) {
            this.user_info = new JsonUserInfo();
        }
        this.user_info.setDisplay_name(name);
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JsonUserShowSideBarPreference getPreferences() {
        return preferences;
    }

    public void setPreferences(JsonUserShowSideBarPreference preferences) {
        this.preferences = preferences;
    }

    public List<JsonGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<JsonGroup> groups) {
        this.groups = groups;
    }

    public JsonClientFeatures getFeatures() {
        return features;
    }

    public void setFeatures(JsonClientFeatures features) {
        this.features = features;
    }

    public JsonFlash getFlash() {
        return flash;
    }

    public void setFlash(JsonFlash flash) {
        this.flash = flash;
    }

    public JsonUserInfo getUser_info() {
        return user_info;
    }

    public void setUser_info(JsonUserInfo user_info) {
        this.user_info = user_info;
    }

}
