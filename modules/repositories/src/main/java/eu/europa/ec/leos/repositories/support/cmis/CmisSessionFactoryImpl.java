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
package eu.europa.ec.leos.repositories.support.cmis;

import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.DocumentType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import eu.europa.ec.leos.model.content.LeosTypeId;
import eu.europa.ec.leos.model.security.SecurityContext;

public class CmisSessionFactoryImpl implements CmisSessionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CmisSessionFactoryImpl.class);

    @Autowired
    private SessionFactory sessionFactory;

    @Value("${leos.cmis.repository.id}")
    private String repositoryId;

    @Value("${leos.cmis.repository.binding}")
    private String repositoryBinding;

    @Value("${leos.cmis.repository.url}")
    private String repositoryUrl;

    @Value("${leos.cmis.ws.repository.url}")
    private String repositoryRepositoryServiceWsUrl;

    @Value("${leos.cmis.ws.discovery.url}")
    private String repositoryDiscoveryServiceWsUrl;

    @Value("${leos.cmis.ws.multifiling.url}")
    private String repositoryMultiFilingServiceWsUrl;

    @Value("${leos.cmis.ws.navigation.url}")
    private String repositoryNavigationServiceWsUrl;

    @Value("${leos.cmis.ws.object.url}")
    private String repositoryObjectServiceWsUrl;

    @Value("${leos.cmis.ws.policy.url}")
    private String repositoryPolicyServiceWsUrl;

    @Value("${leos.cmis.ws.relationship.url}")
    private String repositoryRelationshipServiceWsUrl;

    @Value("${leos.cmis.ws.versioning.url}")
    private String repositoryVersioningServiceWsUrl;

    @Value("${leos.cmis.ws.acl.url}")
    private String repositoryAclServiceWsUrl;

    @Value("${leos.cmis.httpInvoker.class:}")
    private String httpInvokerClass;

    @Autowired
    protected SecurityContext leosSecurityContext;
        
    public Session createSession() {
        LOG.trace("Creating a CMIS session...");

        Map<String, String> parameters = new HashMap<>();
        parameters.put(SessionParameter.REPOSITORY_ID, repositoryId);

        BindingType bindingType = BindingType.fromValue(repositoryBinding);
        parameters.put(SessionParameter.BINDING_TYPE, bindingType.value());

        if (StringUtils.isNotBlank(httpInvokerClass)) {
            LOG.debug("Overriding default HTTP invoker with class: {}", httpInvokerClass);
            parameters.put(SessionParameter.HTTP_INVOKER_CLASS, httpInvokerClass);
        }

        //link the session to user
        parameters.put(SessionParameter.USER, leosSecurityContext.getUser().getName());

        switch (bindingType) {
            case WEBSERVICES:
                parameters.put(SessionParameter.WEBSERVICES_JAXWS_IMPL, "sunri" );
                parameters.put(SessionParameter.WEBSERVICES_ACL_SERVICE,            repositoryRepositoryServiceWsUrl);
                parameters.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE,      repositoryDiscoveryServiceWsUrl);
                parameters.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE,    repositoryMultiFilingServiceWsUrl);
                parameters.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE,     repositoryNavigationServiceWsUrl);
                parameters.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE,         repositoryObjectServiceWsUrl);
                parameters.put(SessionParameter.WEBSERVICES_POLICY_SERVICE,         repositoryPolicyServiceWsUrl);
                parameters.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE,   repositoryRelationshipServiceWsUrl);
                parameters.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE,     repositoryRepositoryServiceWsUrl);
                parameters.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE,     repositoryVersioningServiceWsUrl);
                break;
            case ATOMPUB:
                parameters.put(SessionParameter.ATOMPUB_URL, repositoryUrl);
                break;
            case BROWSER:
                parameters.put(SessionParameter.BROWSER_URL, repositoryUrl);
                break;
            default:
                throw new IllegalArgumentException("Repository binding of type '" + repositoryBinding + "' is not supported!");
        }

        Session session = sessionFactory.createSession(parameters);

        // FIXME find a better place to log the repository info on demand
        logRepositoryInfo(session);
        checkMandatoryCapabilities(session);
        
        return session;
    }

    private void logRepositoryInfo(Session session) {
        if (LOG.isDebugEnabled()) {
            RepositoryInfo repositoryInfo = session.getRepositoryInfo();
            LOG.debug("Repository information...");
            LOG.debug("Repository vendor name: {}", repositoryInfo.getVendorName());
            LOG.debug("Repository product name: {}", repositoryInfo.getProductName()); 
            LOG.debug("Repository product version: {}", repositoryInfo.getProductVersion());
            LOG.debug("Repository id: {}", repositoryInfo.getId());
            LOG.debug("Repository name: {}", repositoryInfo.getName());
            LOG.debug("Repository description: {}", repositoryInfo.getDescription());
            LOG.debug("Repository CMIS version supported: {}", repositoryInfo.getCmisVersionSupported());
            
        }
    }
    
    
    //this method checks for mandatory capabilities. if not then throw exception.
    private void checkMandatoryCapabilities(Session session) {

    	DocumentType leosDocType =(DocumentType)session.getTypeDefinition(LeosTypeId.LEOS_DOCUMENT.value());
    	
    	//1. if leos:Document type is available
    	Validate.notNull(leosDocType, "leos:document type is not defined");
        LOG.debug("Leos Document Type is defined");

    	//2. check if the leos document type is versionable
        Validate.isTrue(
        		(leosDocType.isVersionable()==true),
                "leos:document is not a versionable! [isVersionalble=%s]",
                leosDocType.isVersionable());
        LOG.debug("Leos Document Type is versionable: {}", leosDocType.isVersionable());
    }

}
