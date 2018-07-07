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
package eu.europa.ec.leos.services.format;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;

/** implementation of how to load local resources by Itext renderer(flying saucer  
 */
public class LocalLoadingUserAgent extends ITextUserAgent {
	
	LocalLoadingUserAgent(ITextOutputDevice _outputDevice){
		super(_outputDevice);
	}
	
	private static final Logger LOG = LoggerFactory.getLogger(LocalLoadingUserAgent.class);

	/**
     * Gets a Reader for the resource identified
     *
     * @param uri PARAM
     * @return The stylesheet value
     */
    //TODO:implement this with nio.
    protected InputStream resolveAndOpenStream(String uri) {
        java.io.InputStream is = null;
        StringBuffer sbAbsolutePth=new StringBuffer("");
        ClassLoader cLoader = Thread.currentThread().getContextClassLoader();
        try {
            if(uri==null)
                return is;
        // input find path starting from
        int iLastIndex=uri.lastIndexOf("VAADIN");
        String fileName =null;
        if(iLastIndex >-1)
        	fileName =uri.substring(iLastIndex);
        else 
        	fileName=uri;
        
        
        	//create absolute path and
        	String pathWebApp= cLoader.getResource("").getPath();
        	sbAbsolutePth.append(pathWebApp).append("../../").append(fileName);
        	is = new FileInputStream(new File(sbAbsolutePth.toString()));
            //is = new URL(uri).openStream();
        } catch (Throwable e ) {// to cleanup later
        	e.printStackTrace();
        	LOG.debug("exception occured {}",e);
        	LOG.debug("resource name {} not found", uri);
        	
        }
        return is;
    }
    
    //unfinsihed
    /*private InputStream getProtectedResource(String uri){
        
    if (_isProtectedResource(uri))
    {
        java.io.InputStream is = null;
        uri = resolveURI(uri);
        try {
            URL url = new URL(uri);
            String encoding = new Base64Encoder().encode ("kaushvi:kaushvi".getBytes());
            URLConnection uc = url.openConnection();
            uc.setRequestProperty  ("Authorization", "Basic " + encoding);
            is = uc.getInputStream();
            LOG.debug("got input stream");
        }
        catch (java.net.MalformedURLException e) {
            LOG.error("bad URL given: " + uri, e);
        }
        catch (java.io.FileNotFoundException e) {
            LOG.error("item at URI " + uri + " not found");
        }
        catch (java.io.IOException e) {
            LOG.error("IO problem for " + uri, e);
        }
        return is;
    }
    else
    {
        return super.resolveAndOpenStream(uri);
    }
}
   */

    
}
