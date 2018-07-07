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
package eu.europa.ec.leos.model.content;

 import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

 import eu.europa.ec.leos.vo.UserVO;
 import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.commons.lang3.Validate;

public class LeosObject implements LeosObjectProperties {

    public static final String AUTHOR_ID          ="leos:authorId";
    public static final String AUTHOR_NAME        ="leos:authorName";
    public static final String CONTRIBUTOR_IDS    ="leos:contributorIds";
    public static final String CONTRIBUTOR_NAMES  ="leos:contributorNames";

    private final CmisObject cmisObject;
    private List<UserVO> contributors;
    private UserVO author;

    public LeosObject(@Nonnull final CmisObject object) {
        cmisObject = object;

        Validate.notNull(cmisObject, "CMIS object must not be null!");

        Validate.notNull(
                LeosTypeId.fromValue(cmisObject.getType().getId()),
                "CMIS object is not a supported LEOS type! [id=%s, typeId=%s]",
                cmisObject.getId(),
                cmisObject.getType().getId());
    }

    @Override
    public LeosTypeId getLeosTypeId() {
        return LeosTypeId.fromValue(cmisObject.getType().getId());
    }

    @Override
    public String getLeosId() {
        return cmisObject.getId();
    }

    @Override
    public String getName() {
        return cmisObject.getName();
    }

    @Override
    public String getDescription() {
        return cmisObject.getDescription();
    }

    @Override
    public String getCreatedBy() {
        return cmisObject.getCreatedBy();
    }

    @Override
    public Date getCreatedOn() {
        return (cmisObject.getCreationDate() != null) ?
                cmisObject.getCreationDate().getTime() :
                null;
    }

    @Override
    public String getUpdatedBy() {
        return cmisObject.getLastModifiedBy();
    }

    @Override
    public Date getUpdatedOn() {
        return (cmisObject.getLastModificationDate() != null) ?
                cmisObject.getLastModificationDate().getTime() :
                null;
    }

    @Override
    public List<UserVO> getContributors(){
        return contributors;
    }

    @Override
    public UserVO getAuthor() {
        return author;
    }

    public void setContributors(List<UserVO> contributors) {
        this.contributors = contributors;
    }

    public void setAuthor(UserVO author) {
        this.author = author;
    }

}
