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
package eu.europa.ec.leos.services.compare;

public interface ContentComparatorService {

    String ATTR_NAME = "class";
    String CONTENT_BLOCK_REMOVED_CLASS = "leos-marker-content-removed";
    String CONTENT_BLOCK_ADDED_CLASS = "leos-marker-content-added";
    String CONTENT_BLOCK_MODIFIED_CLASS = "leos-content-modified";
    String CONTENT_REMOVED_CLASS = "leos-content-removed";
    String CONTENT_ADDED_CLASS = "leos-content-new";
    String CONTENT_SOFT_ADDED_CLASS = "leos-content-soft-new";
    String CONTENT_SOFT_REMOVED_CLASS = "leos-content-soft-removed";

    String DOUBLE_COMPARE_REMOVED_CLASS = "leos-double-compare-removed";
    String DOUBLE_COMPARE_ADDED_CLASS = "leos-double-compare-added";
    String DOUBLE_COMPARE_RETAIN_CLASS = "leos-double-compare-retain";
    
    String DOUBLE_COMPARE_INTERMEDIATE_STYLE = "-intermediate";
    String DOUBLE_COMPARE_ORIGINAL_STYLE = "-original";
    
    /** this service compares the two XML string in the context
     * and marks the modifed content with SPAN containing attrName set to either removedValue or addedValue
     * or the whole removed/added element with the same attrName in this interface for dispaly in single document form
     * @param context of the comparison
     * @return marked XML String
     */
    String compareContents(ContentComparatorContext context);

    /** this service compares the two XML input string in the context
     * and marks the modifed content with SPAN containing class in this interface for dispaly in two column format
     * @param context of the comparison
     * @return two marked XML Strings in array
     */
    String[] twoColumnsCompareContents(ContentComparatorContext context);
}
