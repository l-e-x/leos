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
package eu.europa.ec.leos.web.event.view.repository;

import eu.europa.ec.leos.model.content.LeosDocumentProperties.Stage;


public class SelectDocumentEvent {

    private String documentId;
    private Stage documentStage =Stage.DRAFT;

    public SelectDocumentEvent(String documentId, Stage docStage) {
        this.documentId = documentId;
        if(docStage!=null){
            this.documentStage= docStage;
        }
    }

    public String getDocumentId() {
        return documentId;
    }

    public Stage getDocumentStage() {
        return documentStage;
    }

    public void setDocumentStage(Stage documentStage) {
        this.documentStage = documentStage;
    }
    

}
