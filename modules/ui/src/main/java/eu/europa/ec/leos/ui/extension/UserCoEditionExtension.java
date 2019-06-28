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
package eu.europa.ec.leos.ui.extension;

import com.vaadin.annotations.JavaScript;
import com.vaadin.ui.AbstractField;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.coedition.InfoType;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;

@JavaScript({"vaadin://../js/ui/extension/userCoEditionConnector.js" + LeosCacheToken.TOKEN})
public class UserCoEditionExtension<T extends AbstractField<V>, V> extends LeosJavaScriptExtension {

    private static final long serialVersionUID = 1L;

    public static SimpleDateFormat dataFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    private MessageHelper messageHelper;
    private User user;
    private boolean coEditionSipEnabled;
    private String coEditionSipDomain;

    public UserCoEditionExtension(T target, MessageHelper messageHelper, SecurityContext securityContext, ConfigurationHelper cfgHelper) {
        super();
        this.messageHelper = messageHelper;
        this.user = securityContext.getUser();
        this.coEditionSipEnabled = Boolean.valueOf(cfgHelper.getProperty("leos.coedition.sip.enabled"));
        this.coEditionSipDomain = cfgHelper.getProperty("leos.coedition.sip.domain");
        extend(target);
    }

    @Override
    protected UserCoEditionState getState() {
        return (UserCoEditionState) super.getState();
    }

    @Override
    protected UserCoEditionState getState(boolean markAsDirty) {
        return (UserCoEditionState) super.getState(markAsDirty);
    }

    protected void extend(T target) {
        super.extend(target);
        // handle target's value change
        target.addValueChangeListener(event -> {
            LOG.trace("Target's value changed...");
            // Mark that this connector's state might have changed.
            // There is no need to send new data to the client-side,
            // since we just want to trigger a state change event...
            forceDirty();
        });
    }

    public void updateUserCoEditionInfo(List<CoEditionVO> coEditionVOS, String presenterId) {
        Map<String, String> coEditionElements = new HashMap<>();
        coEditionVOS.stream()
                .filter((x) -> InfoType.ELEMENT_INFO.equals(x.getInfoType()) && !x.getPresenterId().equals(presenterId))
                .sorted(Comparator.comparing(CoEditionVO::getUserName).thenComparingLong(CoEditionVO::getEditionTime))
                .forEach(x -> {
                    StringBuilder userDescription = new StringBuilder();
                    if (!x.getUserLoginName().equals(user.getLogin())) {
                        userDescription.append("<a href=\"")
                                .append(StringUtils.isEmpty(x.getUserEmail()) ? "" : (coEditionSipEnabled ? new StringBuilder("sip:").append(x.getUserEmail().replaceFirst("@.*", "@" + coEditionSipDomain)).toString()
                                        : new StringBuilder("mailto:").append(x.getUserEmail()).toString()))
                                .append("\">").append(x.getUserName()).append(" (").append(StringUtils.isEmpty(x.getEntity()) ? "-" : x.getEntity())
                                .append(")</a>");
                    } else {
                        userDescription.append(x.getUserName()).append(" (").append(StringUtils.isEmpty(x.getEntity()) ? "-" : x.getEntity()).append(")");
                    }
                    coEditionElements.merge(x.getElementId(),
                            messageHelper.getMessage("coedition.tooltip.message", userDescription, dataFormat.format(new Date(x.getEditionTime()))) + "<br>",
                            String::concat);
                });
        getState(true).coEditionElements = coEditionElements;
    }

}
