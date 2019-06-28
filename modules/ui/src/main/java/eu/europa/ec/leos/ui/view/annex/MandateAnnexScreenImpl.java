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
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.ui.component.ComparisonComponent;
import eu.europa.ec.leos.ui.component.doubleCompare.DoubleComparisonComponent;
import eu.europa.ec.leos.ui.extension.SoftActionsExtension;
import eu.europa.ec.leos.ui.window.TocEditor;
import eu.europa.ec.leos.web.event.view.document.InstanceTypeResolver;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.actions.AnnexActionsMenuBar;

@SpringComponent
@ViewScope
@Instance(InstanceType.COUNCIL)
public class MandateAnnexScreenImpl extends AnnexScreenImpl {
    private static final long serialVersionUID = 934198605326069948L;

    private DoubleComparisonComponent<Annex> doubleComparisonComponent;
    
    MandateAnnexScreenImpl(MessageHelper messageHelper, EventBus eventBus, SecurityContext securityContext, UserHelper userHelper,
            ConfigurationHelper cfgHelper, TocEditor numberEditor, InstanceTypeResolver instanceTypeResolver,
            AnnexActionsMenuBar actionsMenu) {
        super(messageHelper, eventBus, securityContext, userHelper, cfgHelper, numberEditor, instanceTypeResolver);
    }

    @Override
    public void init() {
        super.init();
        doubleComparisonComponent = new DoubleComparisonComponent<>(eventBus, messageHelper, userHelper);
        comparisonComponent.setContent(doubleComparisonComponent);
        screenLayoutHelper.addPane(comparisonComponent, 2, false);
        screenLayoutHelper.layoutComponents();
        new SoftActionsExtension<>(annexContent);
    }

    @Override
    public void populateDoubleComparisonContent(String doubleComparisonContent) {
        if (componentEnabled(ComparisonComponent.class)) {
            doubleComparisonComponent.populateDoubleComparisonContent(doubleComparisonContent, LeosCategory.ANNEX);
        }
    }

    @Override
    public void populateMarkedContent(String markedContent) {
    }
}
