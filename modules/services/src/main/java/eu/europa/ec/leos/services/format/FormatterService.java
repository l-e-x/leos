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

import java.io.InputStream;
import java.io.OutputStream;

public interface FormatterService {
    /**
     * This methods gets the document from repository, converts in HTML format 
     *
     * @param inputStream inputStream of Akmantoso to be returned in html format
     * @param contextPath the base path to be used while creating HTML for resources
     * @return document in html format in outputStream
     */
    public void formatToHtml(InputStream inputStream, OutputStream outputStream, String contextPath);
    /**
     * This methods gets the document from repository, converts in pdf format 
     *
     * @param inputStream inputStream of Akmantoso file to be converted to pdf
     * @param contextPath the base path to be used while creating HTML for resources
	 * @return document in pdf format in outputStream 
     */
    public void formatToPdf(InputStream inputStream, OutputStream outputStream, String contextPath);
    /**
     * This methods gets the document from repository, converts in html format 
     *
    * @param id version id to be returned in html format( NOT document ID)
     * @param contextPath the base path to be used while creating HTML for resources
     * @return document String in html format
     */
    public String formatToHtml(String versionId, String contextPath);

}
