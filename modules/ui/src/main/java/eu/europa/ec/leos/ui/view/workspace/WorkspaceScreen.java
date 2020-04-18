/*
 * Copyright 2019 European Commission
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
package eu.europa.ec.leos.ui.view.workspace;

import com.google.common.base.Function;
import eu.europa.ec.leos.domain.cmis.document.Proposal;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.domain.vo.ValidationVO;
import eu.europa.ec.leos.model.filter.QueryFilter;
import eu.europa.ec.leos.ui.view.TriFunction;
import eu.europa.ec.leos.vo.catalog.CatalogItem;

import java.util.List;
import java.util.stream.Stream;

interface WorkspaceScreen {

    void setDataFunctions(
            TriFunction<Integer, Integer, QueryFilter, Stream<Proposal>> dataFn,
            Function<QueryFilter, Integer> countFn);

    void showCreateDocumentWizard(List<CatalogItem> catalogItems);

    void showCreateMandateWizard();

    void showUploadDocumentWizard();

    void refreshData();

    void showValidationResult(ValidationVO result);

    void showPostProcessingResult(Result result);

    void intializeFiltersWithData(List<CatalogItem> catalogItems);
}
