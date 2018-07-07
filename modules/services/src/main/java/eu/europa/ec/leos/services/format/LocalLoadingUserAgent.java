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
package eu.europa.ec.leos.services.format;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;

/** implementation of how to load local resources by Itext renderer(flying saucer  
 */
public class LocalLoadingUserAgent extends ITextUserAgent {
    private static final Logger LOG = LoggerFactory.getLogger(LocalLoadingUserAgent.class);

    LocalLoadingUserAgent(ITextOutputDevice _outputDevice){
        super(_outputDevice);
    }

    /**Gets a Reader for the resource identified
     * @param uri PARAM
     * @return open input stream of resource
     */
    protected InputStream resolveAndOpenStream(String uri) {
        LOG.trace("Requested resource uri: {}", uri);
        InputStream is = null;
        try {
            if(uri==null)
                return is;

            if(uri.contains("/VAADIN")){
                is = readfromFileSystem(uri);
            }
            else {
                is = readfromURL(uri);
            }

        } catch (Exception e ) {// to cleanup later
            LOG.error("Resource :{} not found", uri);
        }
        return is;
    }

    private InputStream readfromFileSystem(String uri) throws FileNotFoundException{
        //create absolute path and try to find the file
        StringBuffer sbAbsolutePth=new StringBuffer("");
        String fileName =uri.substring(uri.lastIndexOf("VAADIN"));
        ClassLoader cLoader = Thread.currentThread().getContextClassLoader();
        String pathWebApp= cLoader.getResource("").getPath();
        
        sbAbsolutePth.append(pathWebApp).append("../../").append(fileName);
        
        return new FileInputStream(new File(sbAbsolutePth.toString()));
    }

    private InputStream readfromURL(String uri) throws IOException{
        URL address= new URL(uri);
        return address.openStream();
    }
}
