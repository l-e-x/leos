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
package eu.europa.ec.leos.web.support.vaadin;

import com.vaadin.server.SessionInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.context.ApplicationContext;
import ru.xpoft.vaadin.SpringApplicationContext;
import ru.xpoft.vaadin.SpringVaadinServlet;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class LeosSpringVaadinServlet extends SpringVaadinServlet {

    private static final long serialVersionUID = -7386049372625216493L;

    private static final Logger LOG = LoggerFactory.getLogger(LeosSpringVaadinServlet.class);

    private static final String SERVLET_PARAMETER_SESSION_INIT_LISTENER_BEANS = "sessionInitListenerBeans";
    private static final String SERVLET_PARAMETER_SESSION_INIT_LISTENER_CLASSES = "sessionInitListenerClasses";

    private static final String STRING_VALUE_SEPARATOR_REGEX = "\\s*,\\s*";

    @Override
    protected void servletInitialized() throws ServletException {
        LOG.trace("Initializing LEOS servlet...");
        super.servletInitialized();
        processSessionInitListenerBeans();
        processSessionInitListenerClasses();
        LOG.info("LEOS servlet is initialized!");
    }

    private void processSessionInitListenerBeans() throws ServletException {
        if (isParameterConfigured(SERVLET_PARAMETER_SESSION_INIT_LISTENER_BEANS)) {
            LOG.trace("Processing session init listener beans...");
            ApplicationContext applicationContext = SpringApplicationContext.getApplicationContext();
            for (String paramBeanName : getParameterValues(SERVLET_PARAMETER_SESSION_INIT_LISTENER_BEANS)) {
                LOG.trace("Loading bean: '{}'", paramBeanName);
                try {
                    SessionInitListener listenerBean = applicationContext.getBean(paramBeanName, SessionInitListener.class);
                    registerListener(listenerBean);
                } catch (BeanNotOfRequiredTypeException ex) {
                    LOG.error("Bean '{}' is not a subtype of '{}'", paramBeanName, SessionInitListener.class.getCanonicalName());
                    throw new ServletException(ex);
                } catch (Exception ex) {
                    LOG.error("Unable to load bean: '{}'", paramBeanName);
                    throw new ServletException(ex);
                }
            }
        } else {
            LOG.trace("Servlet init parameter '{}' is not configured!", SERVLET_PARAMETER_SESSION_INIT_LISTENER_BEANS);
        }
    }

    private void processSessionInitListenerClasses() throws ServletException {
        if (isParameterConfigured(SERVLET_PARAMETER_SESSION_INIT_LISTENER_CLASSES)) {
            LOG.trace("Processing session init listener classes...");
            for (String paramClassName : getParameterValues(SERVLET_PARAMETER_SESSION_INIT_LISTENER_CLASSES)) {
                try {
                    LOG.trace("Loading class: '{}'", paramClassName);
                    Class<?> paramClass = Class.forName(paramClassName);
                    try {
                        Class<? extends SessionInitListener> listenerClass = paramClass.asSubclass(SessionInitListener.class);
                        try {
                            SessionInitListener listenerObj = listenerClass.newInstance();
                            registerListener(listenerObj);
                        } catch (InstantiationException | IllegalAccessException ex) {
                            LOG.error("Unable to instantiate object from class: '{}'", listenerClass.getCanonicalName());
                            throw new ServletException(ex);
                        }
                    } catch (ClassCastException ex) {
                        LOG.error("Class '{}' is not a subtype of '{}'", paramClassName, SessionInitListener.class.getCanonicalName());
                        throw new ServletException(ex);
                    }
                } catch (ClassNotFoundException ex) {
                    LOG.error("Unable to load class: '{}'", paramClassName);
                    throw new ServletException(ex);
                }
            }
        } else {
            LOG.trace("Servlet init parameter '{}' is not configured!", SERVLET_PARAMETER_SESSION_INIT_LISTENER_CLASSES);
        }
    }

    private boolean isParameterConfigured(String paramName) {
        Enumeration<?> paramNames = getInitParameterNames();
        boolean found = false;
        while (!found && paramNames.hasMoreElements()) {
            found = paramName.equals(paramNames.nextElement());
        }
        return found;
    }

    private List<String> getParameterValues(String paramName) throws ServletException {
        List<String> splitValues;
        String paramValue = getInitParameter(paramName);
        if ((paramValue == null) || paramValue.isEmpty()) {
            splitValues = Collections.emptyList();
            LOG.trace("Servlet init parameter '{}' is null or empty!", paramName);
        } else {
            splitValues = new ArrayList<>();
            for (String value : paramValue.trim().split(STRING_VALUE_SEPARATOR_REGEX)) {
                if (!value.isEmpty()) {
                    splitValues.add(value);
                    LOG.trace("Servlet init parameter '{}' split value: '{}'", paramName, value);
                }
            }
        }
        return splitValues;
    }

    private void registerListener(SessionInitListener sessionInitListener) {
        LOG.trace("Registering session init listener: {}", sessionInitListener.getClass().getCanonicalName());
        getService().addSessionInitListener(sessionInitListener);
    }
}
