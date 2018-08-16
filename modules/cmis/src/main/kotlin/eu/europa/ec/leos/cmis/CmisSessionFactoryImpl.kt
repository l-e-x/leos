/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.cmis

import java.util.HashMap
import org.apache.chemistry.opencmis.client.api.DocumentType
import org.apache.chemistry.opencmis.client.api.Session
import org.apache.chemistry.opencmis.client.api.SessionFactory
import org.apache.chemistry.opencmis.commons.SessionParameter
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo
import org.apache.chemistry.opencmis.commons.enums.BindingType
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import eu.europa.ec.leos.security.SecurityContext

class CmisSessionFactoryImpl : CmisSessionFactory {
    @Autowired
    private val sessionFactory: SessionFactory? = null
    @Value("\${leos.cmis.repository.id}")
    private val repositoryId: String? = null
    @Value("\${leos.cmis.repository.binding}")
    private val repositoryBinding: String? = null
    @Value("\${leos.cmis.ws.repository.url}")
    private val repositoryRepositoryServiceWsUrl: String? = null
    @Value("\${leos.cmis.atom.repository.url}")
    private val repositoryRepositoryAtomUrl: String? = null
    @Value("\${leos.cmis.browser.repository.url}")
    private val repositoryRepositoryBrowserUrl: String? = null
    @Value("\${leos.cmis.ws.discovery.url}")
    private val repositoryDiscoveryServiceWsUrl: String? = null
    @Value("\${leos.cmis.ws.multifiling.url}")
    private val repositoryMultiFilingServiceWsUrl: String? = null
    @Value("\${leos.cmis.ws.navigation.url}")
    private val repositoryNavigationServiceWsUrl: String? = null
    @Value("\${leos.cmis.ws.object.url}")
    private val repositoryObjectServiceWsUrl: String? = null
    @Value("\${leos.cmis.ws.policy.url}")
    private val repositoryPolicyServiceWsUrl: String? = null
    @Value("\${leos.cmis.ws.relationship.url}")
    private val repositoryRelationshipServiceWsUrl: String? = null
    @Value("\${leos.cmis.ws.versioning.url}")
    private val repositoryVersioningServiceWsUrl: String? = null
    @Value("\${leos.cmis.ws.acl.url}")
    private val repositoryAclServiceWsUrl: String? = null
    @Value("\${leos.cmis.ws.authentication.provider.class}")
    private val repositoryAuthProviderClass: String? = null
    @Value("\${leos.cmis.httpInvoker.class:}")
    private val httpInvokerClass: String? = null
    @Autowired
    protected var leosSecurityContext: SecurityContext? = null

    override fun createSession(): Session? {
        LOG!!.trace("Creating a CMIS session...")
        val parameters = HashMap<String, String>()
        parameters!!.put(SessionParameter.REPOSITORY_ID, repositoryId!!)
        val bindingType = BindingType.fromValue(repositoryBinding)
        parameters!!.put(SessionParameter.BINDING_TYPE, bindingType!!.value())
        if (StringUtils.isNotBlank(httpInvokerClass)) {
            LOG!!.debug("Overriding default HTTP invoker with class: {}", httpInvokerClass)
            parameters!!.put(SessionParameter.HTTP_INVOKER_CLASS, httpInvokerClass!!)
        }
        parameters!!.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS, repositoryAuthProviderClass!!)

        when (bindingType) {
            BindingType.WEBSERVICES -> {
                parameters!!.put(SessionParameter.WEBSERVICES_JAXWS_IMPL, "cxf")
                parameters!!.put(SessionParameter.WEBSERVICES_ACL_SERVICE, repositoryAclServiceWsUrl!!)
                parameters!!.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE, repositoryDiscoveryServiceWsUrl!!)
                parameters!!.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE, repositoryMultiFilingServiceWsUrl!!)
                parameters!!.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE, repositoryNavigationServiceWsUrl!!)
                parameters!!.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, repositoryObjectServiceWsUrl!!)
                parameters!!.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, repositoryPolicyServiceWsUrl!!)
                parameters!!.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE, repositoryRelationshipServiceWsUrl!!)
                parameters!!.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE, repositoryRepositoryServiceWsUrl!!)
                parameters!!.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE, repositoryVersioningServiceWsUrl!!)
            }
            BindingType.ATOMPUB -> parameters!!.put(SessionParameter.ATOMPUB_URL, repositoryRepositoryAtomUrl!!)
            BindingType.BROWSER -> parameters!!.put(SessionParameter.BROWSER_URL, repositoryRepositoryBrowserUrl!!)
            else -> throw IllegalArgumentException("Repository binding of type '" + repositoryBinding + "' is not supported!")
        }
        val session = sessionFactory!!.createSession(parameters)
        // KLUGE LEOS-2398 completely disable client-side session cache by default
        session!!.getDefaultContext().setCacheEnabled(false)
        // KLUGE LEOS-2369 load all properties by default
        session.defaultContext.filterString = "*"

        // FIXME find a better place to log the repository info on demand
        logRepositoryInfo(session)
        checkMandatoryCapabilities(session)
        return session
    }

    private fun logRepositoryInfo(session: Session?) {
        if (LOG!!.isDebugEnabled()) {
            val repositoryInfo = session!!.getRepositoryInfo()
            LOG!!.debug("Repository information...")
            LOG!!.debug("Repository vendor name: {}", repositoryInfo!!.getVendorName())
            LOG!!.debug("Repository product name: {}", repositoryInfo!!.getProductName())
            LOG!!.debug("Repository product version: {}", repositoryInfo!!.getProductVersion())
            LOG!!.debug("Repository id: {}", repositoryInfo!!.getId())
            LOG!!.debug("Repository name: {}", repositoryInfo!!.getName())
            LOG!!.debug("Repository description: {}", repositoryInfo!!.getDescription())
            LOG!!.debug("Repository CMIS version supported: {}", repositoryInfo!!.getCmisVersionSupported())
        }
    }

    //this method checks for mandatory capabilities. if not then throw exception.
    private fun checkMandatoryCapabilities(session: Session?) {
        val leosDocType = session!!.getTypeDefinition("leos:xml") as DocumentType
//1. if leos:Document type is available
        Validate.notNull(leosDocType, "leos:document type is not defined")
        LOG!!.debug("Leos Document Type is defined")
//2. check if the leos document type is versionable
        Validate.isTrue(
                (leosDocType!!.isVersionable() === true),
                "leos:document is not a versionable! [isVersionalble=%s]",
                leosDocType!!.isVersionable())
        LOG!!.debug("Leos Document Type is versionable: {}", leosDocType!!.isVersionable())
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CmisSessionFactoryImpl::class.java)
    }
}
