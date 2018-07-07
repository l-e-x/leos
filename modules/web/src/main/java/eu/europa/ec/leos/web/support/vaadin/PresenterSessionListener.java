/**
 * Copyright 2015 European Commission
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
package eu.europa.ec.leos.web.support.vaadin;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import eu.europa.ec.leos.web.presenter.AbstractPresenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PresenterSessionListener implements SessionInitListener {
    private static final long serialVersionUID = -8419607849891564820L;

    @Autowired
    private EventBus leosEventBus;

    @Autowired
    private List<AbstractPresenter> presenters;

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        // register all presenters so they can listen to the view events
        for (AbstractPresenter presenter : presenters) {
            leosEventBus.register(presenter);
        }
    }
}
