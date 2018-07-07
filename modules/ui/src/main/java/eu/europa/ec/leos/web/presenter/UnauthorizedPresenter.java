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
package eu.europa.ec.leos.web.presenter;

import com.google.common.eventbus.Subscribe;

import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.web.event.view.unauthorized.EnterUnauthorizedViewEvent;
import eu.europa.ec.leos.web.view.UnauthorizedView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
public class UnauthorizedPresenter extends AbstractPresenter<UnauthorizedView> {

    @Autowired
    private SecurityContext securityContext;

    @Autowired
    private UnauthorizedView unauthorizedView;

    @Subscribe
    public void enterUnauthorizedView(EnterUnauthorizedViewEvent event) {
        unauthorizedView.buildUnauthorizedInfo(securityContext.getPrincipalName());
    }

    @Override
    public UnauthorizedView getView() {
        return unauthorizedView;
    }

}
