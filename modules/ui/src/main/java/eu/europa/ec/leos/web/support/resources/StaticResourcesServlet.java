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
package eu.europa.ec.leos.web.support.resources;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * this servlet serves the static resources placed in base path.. css/images etc
 * It should not be used to serve dynamic or user specific content
 */

public class StaticResourcesServlet extends HttpServlet {

    private static final long serialVersionUID = -3650695759517339652L;
    private static final long ONE_SECOND_IN_MILLIS = TimeUnit.SECONDS.toMillis(1);
    public static final long DEFAULT_EXPIRE_TIME_IN_MILLIS = TimeUnit.DAYS.toMillis(30);
    public static final int DEFAULT_STREAM_BUFFER_SIZE = 102400;

    private String basePath;

    @Override
    public void init() throws ServletException {
        String baseFolder = getInitParameter("basePath");
        this.basePath = (baseFolder == null)
                ? ""
                : baseFolder; // Get base path as init parameter. (path inside the web app )
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(request, response, true);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(request, response, false);
    }

    private void doRequest(HttpServletRequest request, HttpServletResponse response, boolean head) throws IOException {
        InputStream resource;

        try {
            resource = getStaticResource(request);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        if (resource == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // String eTag = String.format("W/\"%s-%s\"", URLEncoder.encode(resource.getName(), "UTF-8"), resource.lastModified());

        response.reset();
        // response.setHeader("ETag", eTag);
        // response.setDateHeader("Last-Modified", resource.lastModified());
        response.setDateHeader("Expires", System.currentTimeMillis() + DEFAULT_EXPIRE_TIME_IN_MILLIS);

        // if (notModified(request, eTag, resource.lastModified())) {
        // response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        // return;
        // }

        response.setContentType(getServletContext().getMimeType(getName(request)));

        // if (resource.available() != -1) {
        // response.setHeader("Content-Length", String.valueOf(resource.length()));
        // }

        // if (head) {
        // return;
        // }

        try (ReadableByteChannel inputChannel = Channels.newChannel(resource);
                WritableByteChannel outputChannel = Channels.newChannel(response.getOutputStream());) {

            ByteBuffer buffer = ByteBuffer.allocateDirect(DEFAULT_STREAM_BUFFER_SIZE);
            long size = 0;

            while (inputChannel.read(buffer) != -1) {
                buffer.flip();
                size += outputChannel.write(buffer);
                buffer.clear();
            }

            if (resource.available() == -1 && !response.isCommitted()) {
                response.setHeader("Content-Length", String.valueOf(size));
            }
        } catch (Exception ioe) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    /**
     * Returns the resource associated with the given HTTP servlet request. This returns <code>null</code> when
     * the resource does actually not exist. The servlet will then return a HTTP 404 error.
     * @param request The involved HTTP servlet request.
     * @return The resource associated with the given HTTP servlet request.
     * @throws 
     */

    protected InputStream getStaticResource(HttpServletRequest request) throws Exception {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.isEmpty() || "/".equals(pathInfo)) {
            throw new IllegalArgumentException();
        }

		// dont be Mr Smart:, we have tried that ..:)
		//URL fileURL = this.getServletContext().getResource(this.basePath + pathInfo);
        //File file = new File(fileURL.toURI());
        //return !file.exists() || !file.isFile() || !file.canRead()? null : file;
		
        InputStream inputStream = this.getClass().getResourceAsStream(this.basePath + pathInfo);
        return inputStream;
    }

    private static boolean notModified(HttpServletRequest request, String eTag, long lastModified) {
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && matches(ifNoneMatch, eTag)) {
            return true;
        }
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + ONE_SECOND_IN_MILLIS > lastModified) { // That second is because the header is in
            // seconds, not millis.
            return true;
        }
        return false;
    }

    private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1 || Arrays.binarySearch(matchValues, "*") > -1;
    }

    private String getName(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo.substring(pathInfo.lastIndexOf("/"));
    }
}
