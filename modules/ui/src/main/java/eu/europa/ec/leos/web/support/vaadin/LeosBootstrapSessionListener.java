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

import eu.europa.ec.leos.web.support.LeosBuildInfo;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.server.BootstrapFragmentResponse;
import com.vaadin.server.BootstrapListener;
import com.vaadin.server.BootstrapPageResponse;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;

@Scope("singleton")
@Component("leosBootstrapSessionListener")
public class LeosBootstrapSessionListener implements SessionInitListener {

    private static final long serialVersionUID = 3353508071786551417L;

	@Override
	public void sessionInit(SessionInitEvent event) throws ServiceException {
		event.getSession().addBootstrapListener(new LeosBootstrapListener());
	}

    private static class LeosBootstrapListener implements BootstrapListener {

        private static final long serialVersionUID = -6992365769132147362L;
        private static final Logger LOG = LoggerFactory.getLogger(LeosBootstrapListener.class);

        private static final String LEOS_BOOTSTRAP = "js/leosBootstrap.js";
        private static final String LEOS_BOOTSTRAP_ID = "leosBootstrap";
        private static final String REQUIRE_JS = "lib/requirejs_2.1.22/require.js";
        private static final String WEB_BOOTSTRAP = "js/web/leosWebBootstrap.js";

        @Override
        public void modifyBootstrapFragment(BootstrapFragmentResponse response) {
            // do nothing
        }

        @Override
        public void modifyBootstrapPage(BootstrapPageResponse response) {
            LOG.debug("Injecting LEOS JavaScript bootstrap into application's host HTML page...");

            Element htmlHead = response.getDocument().head();

            // set minimal LEOS bootstrap configuration
            htmlHead.appendElement("script")
                    .appendText("var LEOS_BOOTSTRAP_CONFIG = {")
                    .appendText("urlArgs: 'ts=" + LeosBuildInfo.BUILD_TIMESTAMP + "'};");

            // inject LEOS bootstrap script that handles LEOS configuration
            // and sets RequireJS configuration using "require" global var
            htmlHead.appendElement("script")
                    .attr("type", "application/javascript")
                    .attr("src", LEOS_BOOTSTRAP)
                    .attr("id", LEOS_BOOTSTRAP_ID);

            // inject RequireJS with the specified main entry point
            // views and components should load wrappers with @JS
            htmlHead.appendElement("script")
                    .attr("type", "application/javascript")
                    .attr("src", REQUIRE_JS)
                    .attr("data-main", WEB_BOOTSTRAP);
        }
    }
}