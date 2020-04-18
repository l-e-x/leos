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
package eu.europa.ec.leos.integration.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import eu.europa.ec.leos.integration.ExternalDocumentProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;

import java.io.ByteArrayOutputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.concurrent.TimeUnit;

@Component
class OJDocumentProviderImpl implements ExternalDocumentProvider {
    private static Logger LOG = LoggerFactory.getLogger(OJDocumentProviderImpl.class);

    @Value("#{integrationProperties['leos.import.oj.url']}")
    private String ojUrl;

    @Value("#{integrationProperties['leos.import.oj.sparql.uri']}")
    private String sparqlUri;
    
    @Value("#{integrationProperties['leos.proxy.username']}")
    private String proxyUsername;
    @Value("#{integrationProperties['leos.proxy.password']}")
    private String proxyPassword;
    @Value("#{integrationProperties['leos.proxy.host']}")
    private String proxyHost;
    @Value("#{integrationProperties['leos.proxy.port']}")
    private String proxyPort;
    
    private static final String DOC_TYPE = "/DOC_2";
    private static final String PARAM_DEBUG_VALUE = "on";
    private static final long PARAM_TIMEOUT_VALUE = 60000;
    private static final String PARAM_FORMAT_VALUE = "application/sparql-results+json";

    @Autowired
    private RestOperations restTemplate;

    @Override
    public String getFormexDocument(String type, int year, int number) {
        final String uriDocument = getOJFormexDocumentUrl(type, year, number);
        final String formexDocument = uriDocument != null ? getOJFormexDocumentByUrl(uriDocument) : null;
        return formexDocument;
    }

    String getOJFormexDocumentUrl(String type, int year, int number) {
        setProxy();
        final String uri = ojUrl + sparqlUri;
        try {
            Stopwatch stopwatch=Stopwatch.createStarted();
            ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
            queryStr.setNsPrefix("cdm", "http://publications.europa.eu/ontology/cdm#");
            queryStr.append("SELECT DISTINCT ?manifestation ");
            queryStr.append("where {");
            queryStr.append(" ?work cdm:resource_legal_eli ?eli . filter(?eli = 'http://data.europa.eu/eli/");
            queryStr.append(type);
            queryStr.append("/");
            queryStr.append(year);
            queryStr.append("/");
            queryStr.append(number);
            queryStr.append("/oj'");
            queryStr.append("^^");
            queryStr.appendIri("http://www.w3.org/2001/XMLSchema#anyURI");
            queryStr.append(") ");
            queryStr.append(" ?work ^cdm:expression_belongs_to_work ?expression . ");
            queryStr.append("?expression cdm:expression_uses_language ?lng. ");
            queryStr.append("filter(?lng=");
            queryStr.appendIri("http://publications.europa.eu/resource/authority/language/ENG");
            queryStr.append(") ");
            queryStr.append("?manifestation cdm:manifestation_manifests_expression ?expression ; ");
            queryStr.append("cdm:manifestation_type ?type filter(regex(str(?type),'fmx4'))");
            queryStr.append("}");
            Query query = queryStr.asQuery();
            QueryEngineHTTP qexec = QueryExecutionFactory.createServiceRequest(uri, query);
            try {
                LOG.debug("OJ Sparql URL: "+ uri);
                qexec.addDefaultGraph("");
                qexec.addParam("debug", PARAM_DEBUG_VALUE);
                qexec.addParam("timeout", String.valueOf(PARAM_TIMEOUT_VALUE));
                qexec.addParam("format", PARAM_FORMAT_VALUE);
                qexec.setTimeout(PARAM_TIMEOUT_VALUE, PARAM_TIMEOUT_VALUE);
                LOG.debug("OJ Sparql Query: {}", qexec.getQuery().toString(qexec.getQuery().getSyntax()));
                ResultSet results = qexec.execSelect();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ResultSetFormatter.outputAsJSON(outputStream, results);
                String json = new String(outputStream.toByteArray());
                LOG.debug("OJ Sparql Response: {}", json);                
                LOG.trace("OJ Sparql query executed in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
                unsetProxy();
                
                return getDocumentUrl(json);
            } finally {
                qexec.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the getOJFormexDocumentUrl operation. Failed calling: " + uri, e);
        }
    }
    
    String getDocumentUrl(String json) {
        String uriDocument = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json);
            JsonNode bindings = rootNode.get("results").get("bindings");
            if(bindings.size() > 0 && bindings.get(0) != null) {
                JsonNode manifestation = bindings.get(0).get("manifestation");
                uriDocument = manifestation.size() > 0 ? manifestation.get("value").textValue() : null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred during retreival of document url", e);
        }
        return uriDocument;
    }

    String getOJFormexDocumentByUrl(String uriDocument) {
        String formexDocument = null;
        try {
            setProxy();
            uriDocument = uriDocument + DOC_TYPE;
            LOG.debug("OJ Formex document url: " + uriDocument);
            formexDocument = restTemplate.getForObject(uriDocument, String.class);
            unsetProxy();
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the getOJFormexDocumentByUrl operation. Failed calling: " + uriDocument, e);
        }
        return formexDocument;
    }
    
    /**
     * The proxy is added only if the parameters leos.proxy.* are set in the property file
     */
    private void setProxy() {
        if (!StringUtils.isEmpty(proxyHost)) {
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(proxyUsername, proxyPassword.toCharArray());
                }
            });
        }
    }
    
    private void unsetProxy(){
        System.setProperty("http.proxyHost", "");
        System.setProperty("http.proxyPort", "");
        Authenticator.setDefault(null);
    }
}
