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

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import javax.annotation.PostConstruct;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.spring.navigator.SpringNavigator;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import eu.europa.ec.leos.ui.view.LeosView;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NavigationUpdateEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@UIScope
@Component
public class NavigationManager implements Serializable {

    private static final long serialVersionUID = 4402645174494054571L;

    private static final Logger LOG = LoggerFactory.getLogger(NavigationManager.class);

    private EventBus eventBus;
    private SpringNavigator navigator;
    private final LeosViewProvider viewProvider;

    private final FiniteStack<NavigationRequestEvent> navigationStack = new FiniteStack<>(5);

    @Autowired
    public NavigationManager(EventBus eventBus, SpringNavigator navigator, LeosViewProvider viewProvider) {
        this.eventBus = eventBus;
        this.navigator = navigator;
        this.viewProvider = viewProvider;
    }

    @PostConstruct
    private void init() {
        LOG.trace("Configuring navigation manager...");
        navigator.addProvider(viewProvider);

        // create view change listener to handle automatic-closing of the opened windows
        navigator.addViewChangeListener(new OpenWindowHandler());

        // create view change listener to handle navigation updates
        navigator.addViewChangeListener(new NavigationBroadcastHandler());

        eventBus.register(this);
    }

    @Subscribe
    public void navigationRequest(NavigationRequestEvent event) {
        LOG.debug("Navigation request received... [event={}]", event);
        event = resolveDestination(event);
        String urlFragment = createURLFragment(event.getTarget(), event.getParameters());
        navigator.navigateTo(urlFragment);
    }

    private String createURLFragment(Target target, String[] params) {
        return (params == null || params.length == 0)
                ? target.getViewId()
                : target.getViewId() + "/" + StringUtils.joinWith("/", params);//String join doesnt handle null
    }

    private NavigationRequestEvent resolveDestination(NavigationRequestEvent destination) {
        String viewId = destination.getTarget().getViewId();
        NavigationRequestEvent current = navigationStack.peek();
        if(current != null && current.getTarget().getViewId().equalsIgnoreCase(viewId)) {
            navigationStack.pop();
        }
        if (Target.PREVIOUS.equals(destination.getTarget())) {
            navigationStack.pop();              // Discard the current view, as this is current displayed view
            destination = navigationStack.pop();// Previous view from current
            if(destination == null){
                destination = new NavigationRequestEvent(Target.HOME);
            }
        }

        navigationStack.push(destination);// always maintain the view in stack
        LOG.debug("Destination resolved to [target={}]", destination.getTarget());

        return destination;
    }

    private class OpenWindowHandler implements ViewChangeListener {

        private static final long serialVersionUID = -3156526235453685822L;

        @Override
        public boolean beforeViewChange(ViewChangeEvent viewChangeEvent) {
            ArrayList<Window> arrWindows = new ArrayList<>();
            arrWindows.addAll(UI.getCurrent().getWindows());
            for (Window window : arrWindows) {
                LOG.info("Automatic closing of the window {}", window.getCaption());
                window.close();
            }
            return true;
        }

        @Override
        public void afterViewChange(ViewChangeEvent viewChangeEvent) {
            // nothing
        }
    }

    private class NavigationBroadcastHandler implements ViewChangeListener {
        private static final long serialVersionUID = -806089256160487161L;

        @Override
        public boolean beforeViewChange(ViewChangeEvent event) {
            return true;
        }

        @Override
        public void afterViewChange(ViewChangeEvent event) {
            LeosView newView = (LeosView) event.getNewView();
            LOG.debug("Firing navigation update event... [viewId={}]", newView.getViewId());
            eventBus.post(new NavigationUpdateEvent(newView.getViewId(), newView.getViewKey()));
        }
    }

    /* this stack have a finite size. if you try to insert elements beyond size limit, it discards elements from bottom.*/
    private class FiniteStack<T> {

        private Deque<T> stack;
        private int maxCapacity;

        FiniteStack(int maxCapacity) {
            this.stack = new ArrayDeque<>();
            this.maxCapacity = maxCapacity;
        }

        void push(T event) {
            if (size() == maxCapacity) {
                stack.removeLast();// discard the Last in object
            }
            stack.addFirst(event);
        }

        T pop() {
            return (peek() == null) ? null : stack.pop();
        }

        T peek() {
            return stack.peek();
        }

        int size() {
            return stack.size();
        }
    }
}
