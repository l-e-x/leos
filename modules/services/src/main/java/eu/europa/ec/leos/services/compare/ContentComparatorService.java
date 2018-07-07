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
package eu.europa.ec.leos.services.compare;

/**
 * @author: micleva
 * @date: 4/2/13 2:51 PM
 * @project: ETX
 */
public interface ContentComparatorService {

    final static String CONTENT_BLOCK_REMOVED_CLASS = "leos-marker-content-removed";
    final static String CONTENT_BLOCK_ADDED_CLASS = "leos-marker-content-added";
    final static String CONTENT_BLOCK_MODIFIED_CLASS = "leos-content-modified";
    final static String CONTENT_REMOVED_CLASS = "leos-content-removed";
    final static String CONTENT_ADDED_CLASS = "leos-content-new";

    /** this service compares the two XHTML input string passed 
     * and marks the modifed content with SPAN containing class in this interface for dispaly in single document form
     * @param firstContent
     * @param secondContent
     * @return marked XHTML String
     */
    String compareHtmlContents(String firstContent, String secondContent);

    /** this service compares the two XHTML input string passed 
     * and marks the modifed content with SPAN containing class in this interface for dispaly in two column format
     * @param firstContent
     * @param secondContent
     * @return two marked XHTML Strings in array
     */
    String[] twoColumnsCompareHtmlContents(String firstContent, String secondContent);
}
