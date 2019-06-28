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
package eu.europa.ec.leos.web.ui.navigation;


import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.ui.view.annex.AnnexView;
import eu.europa.ec.leos.ui.view.collection.CollectionView;
import eu.europa.ec.leos.ui.view.document.DocumentView;
import eu.europa.ec.leos.ui.view.logout.LogoutView;
import eu.europa.ec.leos.ui.view.memorandum.MemorandumView;
import eu.europa.ec.leos.ui.view.repository.RepositoryView;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Target {
    REPOSITORY(RepositoryView.VIEW_ID),
    PROPOSAL(CollectionView.VIEW_ID),
    LEGALTEXT(DocumentView.VIEW_ID),
    MEMORANDUM(MemorandumView.VIEW_ID),
    ANNEX(AnnexView.VIEW_ID),
    LOGOUT(LogoutView.VIEW_ID),
    HOME(RepositoryView.VIEW_ID),       //Default view
    PREVIOUS("PreviousView");           //Special Target to go to previous view

    private static final Logger LOG = LoggerFactory.getLogger(Target.class);
    private String viewId;


    Target(String viewId) {
        this.viewId = viewId;
    }

    public String getViewId() {
        return viewId;
    }

    public static Target getTarget(String viewId) {
        if (StringUtils.isNotEmpty(viewId)) {
            for (Target target : values()) {
                if (target.getViewId().equals(viewId)) {
                    return target;
                }
            }
        }
        //In case of invalid view id passed, go to HOME.
        LOG.debug("No view is mapped to view id:{}, navigating to HOME", viewId);
        return HOME;
    }

    public static Target getTarget(LeosCategory documentType){
        Target target;
        switch(documentType) {
            case BILL:
                target = Target.LEGALTEXT;
                break;
            case MEMORANDUM:
                target = Target.MEMORANDUM;
                break;
            case ANNEX:
                target = Target.ANNEX;
                break;
            case PROPOSAL:
                target = Target.PROPOSAL;
                break;
            default:
                LOG.debug("No view is mapped to document type:{}, navigating to HOME" , documentType);
                target = Target.HOME;
                break;
        }
        return target;
    }
}
