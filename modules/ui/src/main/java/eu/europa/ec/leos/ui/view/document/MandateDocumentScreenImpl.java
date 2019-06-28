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
package eu.europa.ec.leos.ui.view.document;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.TreeData;
import com.vaadin.server.Resource;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.UI;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.security.LeosPermissionAuthorityMapHelper;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.ui.component.ComparisonComponent;
import eu.europa.ec.leos.ui.component.doubleCompare.DoubleComparisonComponent;
import eu.europa.ec.leos.ui.component.toc.TableOfContentItemConverter;
import eu.europa.ec.leos.ui.extension.SoftActionsExtension;
import eu.europa.ec.leos.ui.window.EditTocWindow;
import eu.europa.ec.leos.ui.window.TocEditor;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.LegalTextMandateTocItemType;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import eu.europa.ec.leos.web.event.view.document.InstanceTypeResolver;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

@ViewScope
@SpringComponent
@Instance(InstanceType.COUNCIL)
public class MandateDocumentScreenImpl extends DocumentScreenImpl {
    private static final long serialVersionUID = 6711728542602337765L;

    private DoubleComparisonComponent<Bill> doubleComparisonComponent;
    private ComparisonComponent<Bill> comparisonComponent;

    MandateDocumentScreenImpl(UserHelper userHelper, SecurityContext securityContext, EventBus eventBus, ConfigurationHelper cfgHelper,
            MessageHelper messageHelper, TocEditor tocEditor, InstanceTypeResolver instanceTypeResolver,
            WebApplicationContext webAppContext, LeosPermissionAuthorityMapHelper authorityMapHelper) {
        super(userHelper, securityContext, eventBus, cfgHelper, messageHelper, tocEditor, instanceTypeResolver, webAppContext, authorityMapHelper);
    }

    @Override
    public void init() {
        super.init();
        comparisonComponent = new ComparisonComponent<>();
        doubleComparisonComponent = new DoubleComparisonComponent<>(eventBus, messageHelper, userHelper);
        comparisonComponent.setContent(doubleComparisonComponent);
        legalTextPaneComponent.screenLayoutHelper.addPane(comparisonComponent, 2, false);
        legalTextPaneComponent.screenLayoutHelper.layoutComponents();
        new SoftActionsExtension<>(legalTextPaneComponent.getContent());
    }
    
    @Override
    public void showTocEditWindow(List<TableOfContentItemVO> tableOfContentItemVoList,
            Map<TocItemType, List<TocItemType>> tableOfContentRules) {

        EditTocWindow editTocWindow = new EditTocWindow(messageHelper, eventBus, cfgHelper, tableOfContentRules,
                LegalTextMandateTocItemType.values(), tocEditor);
        TreeData<TableOfContentItemVO> tocData = TableOfContentItemConverter.buildTocData(tableOfContentItemVoList);
        editTocWindow.setTableOfContent(tocData);
        UI.getCurrent().addWindow(editTocWindow);
        editTocWindow.center();
        editTocWindow.focus();
    }
    
    @Override
    public void populateDoubleComparisonContent(String doubleComparisonContent) {
        doubleComparisonComponent.populateDoubleComparisonContent(doubleComparisonContent, LeosCategory.BILL);
    }

    @Override
    public void setDownloadStreamResource(Resource downloadstreamResource) {
        doubleComparisonComponent.setDownloadStreamResource(downloadstreamResource);
    }
    
    @Override
    public void scrollToMarkedChange(String elementId) {
    }
}
