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
package eu.europa.ec.leos.web.event.view.feedback;

import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.vo.CommentVO;

import java.util.List;

public class CommentsAvailableEvent {

    private List<CommentVO> comments;
    private User user;

    public CommentsAvailableEvent(List<CommentVO> comments, User user) {
        this.comments = comments;
        this.user = user;
    }

    public List<CommentVO> getComments() {
        return comments;
    }

    public User getUser() {
        return user;
    }
}