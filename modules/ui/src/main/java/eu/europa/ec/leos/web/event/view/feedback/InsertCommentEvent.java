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

public class InsertCommentEvent {
    private String elementId=null;
    private String commentId=null;
    private String noteContent=null;
    
    public InsertCommentEvent(String elementId, String commentId, String noteContent){
        this.elementId=elementId;
        this.commentId=commentId;
        this.noteContent=noteContent;
    }
    public String getElementId() {
        return elementId;
    }
    public String getCommentId() {
        return commentId;
    }
    public String getCommentContent() {
        return noteContent;
    }
}
