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

import eu.europa.ec.leos.vo.UserVO;

import java.util.Date;
import java.util.List;

public interface LeosObjectProperties {

    /**
     * Get the LEOS type id of this object.
     *
     * @return the LEOS type id.
     */
    LeosTypeId getLeosTypeId();

    /**
     * Get the invariable id of this object to be used in Leos Application.
     *
     * @return the id.
     */
    String getLeosId();
    /**
     * Get the name of this object.
     *
     * @return the name.
     */
    String getName();

    /**
     * Get the description of this object.
     *
     * @return the description.
     */
    String getDescription();

    /**
     * Get the user who created this object.
     *
     * @return the username.
     */
    String getCreatedBy();

    /**
     * Get the date when this object was created.
     *
     * @return the date.
     */
    Date getCreatedOn();

    /**
     * Get the user who updated this object.
     *
     * @return the username.
     */
    String getUpdatedBy();

    /**
     * Get the date when this object was updated.
     *
     * @return the date.
     */
    Date getUpdatedOn();

    /**
     * returns Contributors name to display for leos document
     * @return List
     */
    List<UserVO> getContributors();
    /**
     * returns Author for leos document
     * @return String
     */
    UserVO getAuthor();
}
