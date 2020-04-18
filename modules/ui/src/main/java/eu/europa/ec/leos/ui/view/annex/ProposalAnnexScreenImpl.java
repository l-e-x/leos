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
package eu.europa.ec.leos.ui.view.annex;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.model.annex.AnnexStructureType;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.ui.component.ComparisonComponent;
import eu.europa.ec.leos.ui.component.markedText.MarkedTextComponent;
import eu.europa.ec.leos.ui.component.toc.TableOfContentItemConverter;
import eu.europa.ec.leos.ui.component.versions.VersionsTab;
import eu.europa.ec.leos.ui.event.view.AddStructureChangeMenuEvent;
import eu.europa.ec.leos.ui.window.toc.TocEditor;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.component.LayoutChangeRequestEvent;
import eu.europa.ec.leos.web.event.view.document.InstanceTypeResolver;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.screen.document.ColumnPosition;

import javax.inject.Provider;
import java.util.List;

@ViewScope
@SpringComponent
@Instance(instances = {InstanceType.COMMISSION, InstanceType.OS})
public class ProposalAnnexScreenImpl extends AnnexScreenImpl {
    private static final long serialVersionUID = -6719257516608653344L;

    private MarkedTextComponent markedTextComponent;
    
    ProposalAnnexScreenImpl(MessageHelper messageHelper, EventBus eventBus, SecurityContext securityContext, UserHelper userHelper,
            ConfigurationHelper cfgHelper, TocEditor numberEditor, InstanceTypeResolver instanceTypeResolver,
            VersionsTab<Annex> versionsTab, Provider<StructureContext> structureContextProvider, PackageService packageService,
            MarkedTextComponent markedTextComponent) {
        super(messageHelper, eventBus, securityContext, userHelper, cfgHelper, numberEditor, instanceTypeResolver, versionsTab, structureContextProvider, packageService);
        this.markedTextComponent = markedTextComponent;
    }

    @Override
    public void init() {
        super.init();
        actionsMenuBar.setChildComponentClass(MarkedTextComponent.class);
        screenLayoutHelper.addPane(comparisonComponent, 2, false);
        screenLayoutHelper.layoutComponents();
    }
    
    @Subscribe
    void changePosition(LayoutChangeRequestEvent event) {
        changeLayout(event, markedTextComponent);
    }

    @Override
    public void cleanComparedContent() {
        final String versionInfo = messageHelper.getMessage("document.compare.version.caption.simple");
        markedTextComponent.populateMarkedContent("", LeosCategory.BILL, versionInfo);
    }

    @Override
    public void showVersion(String content, String versionInfo) {
        changePosition(new LayoutChangeRequestEvent(ColumnPosition.DEFAULT, ComparisonComponent.class));

        markedTextComponent.populateMarkedContent(content.replaceAll("(?i) id=\"", " id=\"marked-"), LeosCategory.BILL, versionInfo);
        markedTextComponent.hideCompareButtons();
    }

    @Override
    public void populateComparisonContent(String comparedContent, String comparedInfo) {
        markedTextComponent.populateMarkedContent(comparedContent, LeosCategory.BILL, comparedInfo);
        markedTextComponent.showCompareButtons();
    }

    @Override
    public void populateDoubleComparisonContent(String comparedContent, String versionInfo) {
        throw new IllegalArgumentException("Operation not valid");
    }

    @Override
    public void scrollToMarkedChange(String elementId) {
        markedTextComponent.scrollToMarkedChange(elementId);
    }
    
    @Override
    public void enableTocEdition(List<TableOfContentItemVO> tableOfContent) {
        tableOfContentComponent.handleEditTocRequest(tocEditor);
        tableOfContentComponent.setTableOfContent(TableOfContentItemConverter.buildTocData(tableOfContent));
    }
    
    @Override
    public void setStructureChangeMenuItem() {
        AnnexStructureType structureType = getStructureType();
        eventBus.post(new AddStructureChangeMenuEvent(structureType));
    }

    @Override
    public boolean isComparisonComponentVisible() {
        return comparisonComponent != null && comparisonComponent.getParent() != null;
    }

}
