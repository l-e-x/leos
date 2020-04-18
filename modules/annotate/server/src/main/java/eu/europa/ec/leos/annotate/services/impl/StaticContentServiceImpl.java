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
package eu.europa.ec.leos.annotate.services.impl;

import eu.europa.ec.leos.annotate.services.StaticContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Service for delivering the content of static resources
 */
@Service
public class StaticContentServiceImpl implements StaticContentService {
//This could be renamed or moved to controller
    private static final Logger LOG = LoggerFactory.getLogger(StaticContentServiceImpl.class);


    @Value("${annotate.server.url}")
    private String serverUrl;

    // -------------------------------------
    // Required components
    // -------------------------------------
    @Autowired
    private ResourceLoader resourceLoader;

    // -------------------------------------
    // Service functionality
    // -------------------------------------
    /**
     * return the API feature list (JSON)
     */
    @Override
    public String getApi() throws IOException {

        try {
            return getContent("responses/api.json")
                     .replaceAll("@annotate.server.url@", serverUrl);
        } catch (IOException e) {
            LOG.error("Cannot read static API resource!");
            throw e;
        }
    }

    /**
     * return the link list (JSON)
     */
    @Override
    public String getLinks() throws IOException {

        try {
            return getContent("responses/links.json")
                    .replaceAll("@annotate.server.url@", serverUrl);
        } catch (IOException e) {
            LOG.error("Cannot read static links resource!");
            throw e;
        }
    }

    /**
     * read the content of a given file below the classpath
     * 
     * @param filePath path of the file to be read
     * @return file content as string
     * @throws IOException exception is thrown when requested resource does not exist
     */
    private String getContent(final String filePath) throws IOException {

        final Resource resource = resourceLoader.getResource("classpath:" + filePath);
        final InputStream instream = resource.getInputStream();

        final StringBuilder strb = new StringBuilder();
        try(InputStreamReader isr = new InputStreamReader(instream, Charset.forName("UTF-8"))) {
            try(BufferedReader reader = new BufferedReader(isr)) {
                while (true) {
                    final String line = reader.readLine();
                    if (line == null) break;
                    strb.append(line);
                }
            }
        }
        
        return strb.toString();
    }
}
