/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.ui.view;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all LEOS views.
 */
public abstract class AbstractLeosView<T extends Component> extends CustomComponent implements LeosView {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractLeosView.class);

    protected final T screen;
    protected final LeosPresenter presenter;

    protected AbstractLeosView(T screen) {
        this(screen, null);
    }

    protected AbstractLeosView(T screen, LeosPresenter presenter) {
        super();
        LOG.trace("Initializing {} view...", getViewId());
        Validate.notNull(screen, "Screen must not be null!");
        this.screen = screen;
        this.presenter = presenter;
        init();
    }

    private void init() {
        setCompositionRoot(screen);
        addStyleName("leos-" + getViewId() + "-view");
        setSizeFull();
    }

    @Override
    public void attach() {
        LOG.trace("Attaching {} view...", getViewId());
        super.attach();
        if (presenter != null) {
            presenter.attach();
        }
    }

    @Override
    public void detach() {
        LOG.trace("Detaching {} view...", getViewId());
        if (presenter != null) {
            presenter.detach();
        }
        removeSessionParameters();
        super.detach();
    }

    @Override
    public void enter(ViewChangeEvent event) {
        LOG.trace("Entering {} view with parameters :{}", getViewId(), event.getParameters());
        setSessionParameter(event.getParameters());
        if (presenter != null) {
            presenter.enter();
        }
    }

    private WrappedSession getWrappedSession() {
        VaadinRequest request = VaadinService.getCurrentRequest();
        return (request != null) ? request.getWrappedSession() : null;
    }

    private void setSessionParameter(String parameterString){
        String[] parametersKeys = getParameterKeys();
        String[] parameters = parameterString.isEmpty()
                                ? new String[]{}
                                : parameterString.split("/",-1);

        if (parametersKeys.length != parameters.length) {
            LOG.debug("Incorrect number of parameters for {} view:{}", getViewId(), parameters);
        }

        WrappedSession session = getWrappedSession();
        for (int index =0; index < parameters.length && index < parametersKeys.length; index++) {
            session.setAttribute(parametersKeys[index], parameters[index]);
        }
    }

    private void removeSessionParameters(){
        WrappedSession session = getWrappedSession();
        if (session != null) {
            for (String key : getParameterKeys()) {
                session.removeAttribute(key);
            }
        }
    }

    /**
     * get the keys of parameters required for the view,
     *
     * @return the parameters keys
     */
    abstract protected String[] getParameterKeys();
}
