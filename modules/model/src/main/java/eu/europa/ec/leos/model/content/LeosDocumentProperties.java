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
package eu.europa.ec.leos.model.content;


public interface LeosDocumentProperties extends LeosFileProperties{


    public static final String TITLE= "leos:title";
    public static final String TEMPLATE= "leos:template";
    public static final String LANGUAGE ="leos:language";

    /** Returns the title of the document stored in repository 
     * @return title as string
     */
    String getTitle();

    /** returns the Language code stored in repository object (default=en)
     * @return langugae code
     */
    String getLanguage();

    /** returns template for leos document
     * @return template
     */
    String getTemplate();
}
