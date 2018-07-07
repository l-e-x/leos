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
package eu.europa.ec.leos.web.model;

import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.model.user.Permission;
import eu.europa.ec.leos.vo.UserVO;
import eu.europa.ec.leos.vo.lock.LockData;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DocumentVO {

    public static enum LockState {
        UNLOCKED, LOCKED
    };
    private String leosId;
    private String title;
    private String createdBy;
    private Date createdOn;
    private String updatedBy;
    private Date updatedOn;
    private String language;
    private String template;
    private List<UserVO> contributors;
    private LeosDocumentProperties.OwnerSystem ownerSystem;
    private LeosDocumentProperties.Stage stage;

    private LockState lockState;
    private String msgForUser;
    private List<LockData> arrLockInfo;
    private List<Permission> permissions;
    private UserVO author;

    public DocumentVO() {

    }

    public DocumentVO(LeosDocumentProperties leosDocumentProperties) {
        if(leosDocumentProperties!=null) {
            this.leosId = leosDocumentProperties.getLeosId();
            this.title = leosDocumentProperties.getTitle();
            this.createdBy = leosDocumentProperties.getCreatedBy();
            this.createdOn = leosDocumentProperties.getCreatedOn();
            this.updatedBy = leosDocumentProperties.getUpdatedBy();
            this.updatedOn = leosDocumentProperties.getUpdatedOn();
            this.language = leosDocumentProperties.getLanguage();
            this.template = leosDocumentProperties.getTemplate();
            this.stage = leosDocumentProperties.getStage();
            this.ownerSystem = leosDocumentProperties.getOwnerSystem();
            this.author = leosDocumentProperties.getAuthor();
            this.contributors = leosDocumentProperties.getContributors();
        }
    }

    public String getLeosId() {
        return leosId;
    }

    public void setLeosId(String leosId) {
        this.leosId = leosId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public LeosDocumentProperties.Stage getStage() {
        return stage;
    }

    public void setStage(LeosDocumentProperties.Stage stage) {
        this.stage = stage;
    }

    public List<LockData> getArrLockInfo() {
        return arrLockInfo;
    }

    public void setArrLockInfo(List<LockData> arrLockInfo) {
        this.arrLockInfo = arrLockInfo;
    }

    public LockState getLockState() {
        return lockState;
    }

    public void setLockState(LockState lockState) {
        this.lockState = lockState;
    }

    public List<LockData> getLockInfo() {
        return arrLockInfo;
    }

    public void setLockInfo(List<LockData> arrLockInfo) {
        this.arrLockInfo = arrLockInfo;
    }

    public String getMsgForUser() {
        return msgForUser;
    }

    public void setMsgForUser(String msgForUser) {
        this.msgForUser = msgForUser;
    }

    public LeosDocumentProperties.OwnerSystem getOwnerSystem() {
        return ownerSystem;
    }

    public void setOwnerSystem(LeosDocumentProperties.OwnerSystem ownerSystem) {
        this.ownerSystem = ownerSystem;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    public void setContributors(List<UserVO> contributors) {
        this.contributors = contributors;
    }

    public List<UserVO> getContributors() {
        return contributors;
    }

    public UserVO getAuthor() {
        return author;
    }

    public void setAuthor(UserVO author) {
        this.author = author;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DocumentVO that = (DocumentVO) o;

        if (!getLeosId().equals(that.getLeosId())) return false;
        if (getTitle() != null ? !getTitle().equals(that.getTitle()) : that.getTitle() != null)
            return false;
        if (getCreatedBy() != null ? !getCreatedBy().equals(that.getCreatedBy()) : that.getCreatedBy() != null)
            return false;
        if (getCreatedOn() != null ? !getCreatedOn().equals(that.getCreatedOn()) : that.getCreatedOn() != null)
            return false;
        if (getUpdatedBy() != null ? !getUpdatedBy().equals(that.getUpdatedBy()) : that.getUpdatedBy() != null)
            return false;
        if (getUpdatedOn() != null ? !getUpdatedOn().equals(that.getUpdatedOn()) : that.getUpdatedOn() != null)
            return false;
        if (getLanguage() != null ? !getLanguage().equals(that.getLanguage()) : that.getLanguage() != null)
            return false;
        if (!getTemplate().equals(that.getTemplate())) return false;
        if (getOwnerSystem() != that.getOwnerSystem()) return false;
        return getStage() == that.getStage();

    }

    @Override public int hashCode() {
        int result = getLeosId().hashCode();
        result = 31 * result +  (getTitle() != null ? getTitle() .hashCode() : 0);
        result = 31 * result + (getCreatedBy() != null ? getCreatedBy().hashCode() : 0);
        result = 31 * result + (getCreatedOn() != null ? getCreatedOn().hashCode() : 0);
        result = 31 * result + (getUpdatedBy() != null ? getUpdatedBy().hashCode() : 0);
        result = 31 * result + (getUpdatedOn() != null ? getUpdatedOn().hashCode() : 0);
        result = 31 * result + (getLanguage() != null ? getLanguage().hashCode() : 0);
        result = 31 * result + (getTemplate() != null ? getTemplate().hashCode() : 0);
        result = 31 * result + (getOwnerSystem() != null ? getOwnerSystem().hashCode() : 0);
        result = 31 * result + (getStage() != null ? getStage().hashCode() : 0);
        return result;
    }
}
