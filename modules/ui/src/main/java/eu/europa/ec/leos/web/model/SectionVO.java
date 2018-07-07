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

import eu.europa.ec.leos.vo.CommentVO;

import java.util.List;

/** This class is to be used for sending exising comments to sideComments plugin
 * Only those elements which have some comments, need to be populated*/
public class SectionVO {

    // SectionId name is used as sidecommentPlugin uses this terminology.. this does not correspond to section in akmantoso section in anyway
    // This section Id correcponds to the id of the Enclosing Element for the comment for eg.. aknp, heading etc.
    private String sectionId;
    private List<CommentVO> comments;

    public SectionVO(){
    }

    public SectionVO(String sectionId, List<CommentVO> comments) {
        this.sectionId =sectionId;
        this.comments = comments;
    }

        public List<CommentVO> getComments() {
            return comments;
    }

    public void setComments(List<CommentVO> comments) {
        this.comments = comments;
    }

    public String getSectionId() {
        return sectionId;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }
}
