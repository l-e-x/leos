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
package eu.europa.ec.leos.ui.component.markedText;

import com.google.common.eventbus.EventBus;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.instance.Instance;

import java.util.HashMap;
import java.util.Map;

@ViewScope
@SpringComponent
@Instance(InstanceType.OS)
public class LeosMarkedTextComponent extends MarkedTextComponent {

    private static final long serialVersionUID = 2444295781989179408L;

    public LeosMarkedTextComponent(final EventBus eventBus, final MessageHelper messageHelper) {
        super(eventBus, messageHelper);
    }

    @Override
    protected Map<String, String> getSelectorStyleMap() {
        Map<String, String> selectorStyleMap = new HashMap<>();
        selectorStyleMap.put(".leos-marker-content-removed", "pin-leos-marker-content-removed");
        selectorStyleMap.put(".leos-marker-content-added", "pin-leos-marker-content-added");
        selectorStyleMap.put(".leos-content-removed", "pin-leos-content-removed");
        selectorStyleMap.put(".leos-content-new", "pin-leos-content-new");
        return selectorStyleMap;
    }
}
