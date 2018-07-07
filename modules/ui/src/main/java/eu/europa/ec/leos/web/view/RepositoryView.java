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
package eu.europa.ec.leos.web.view;

import eu.europa.ec.leos.vo.UserVO;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.model.DocumentVO;

import java.util.List;

public interface RepositoryView extends LeosView {

    public static final String VIEW_ID = "repository";

    void setSampleDocuments(final List<DocumentVO> documentVOs);

    void showCreateDocumentWizard(List<CatalogItem> catalogItems);

    void updateLockInfo(DocumentVO documentVO);

    void showDisclaimer();

    void openContributorsWindow(List<UserVO> allUsers, DocumentVO docDetails);
}
