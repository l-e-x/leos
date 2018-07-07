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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.server.BootstrapFragmentResponse;
import com.vaadin.server.BootstrapListener;
import com.vaadin.server.BootstrapPageResponse;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;

import eu.europa.ec.leos.model.security.SecurityContext;

@Scope("singleton")
@Component("jsBootstrapSessionListener")
public class JsBootstrapSessionListener implements SessionInitListener {

	private static final long serialVersionUID = -2240619531224616575L;

	private static final Logger LOG = LoggerFactory
			.getLogger(JsBootstrapSessionListener.class);

	@Autowired
	private SecurityContext securityContext;

	@Override
	public void sessionInit(SessionInitEvent event) throws ServiceException {
		event.getSession().addBootstrapListener(new BootstrapListener() {

			private static final long serialVersionUID = 1748216946283264700L;

			@Override
			public void modifyBootstrapPage(BootstrapPageResponse response) {
				LOG.trace("Including require js in the head of html document...");
				/*
				 * ensures that require.js is included as first script in the
				 * html head to process
				 */
				response.getDocument()
						.head()
						.appendElement("script")
						.attr("type", "text/javascript")
						.attr("src",
								"VAADIN/js/editor/lib/requirejs_2.1.20/require.js")
						.attr("data-main", "VAADIN/js/editor/leosBootstrap.js");
			}

			@Override
			public void modifyBootstrapFragment(
					BootstrapFragmentResponse response) {
			}
		});
	}
}
