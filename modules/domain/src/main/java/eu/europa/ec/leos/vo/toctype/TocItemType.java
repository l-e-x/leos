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
package eu.europa.ec.leos.vo.toctype;

public interface TocItemType {
    
    final String NUM_HEADING_SEPARATOR = " - ";
    
    final String CONTENT_SEPARATOR = " ";

    boolean isRoot();

    boolean isDraggable();

    boolean isToBeDisplayed();
    
    boolean hasItemNumber();
    
    boolean hasItemHeading();

    String getName();

    void setName(String name);

    boolean areChildrenAllowed();
    
    String getNumHeadingSeparator();
    
    String getContentSeparator();
    
    boolean hasItemDescription();
    
    boolean isNumberEditable();
    
    boolean isContentDisplayed();
    
    boolean isDeletable();
    
    boolean hasNumWithType();

    boolean isExpandedByDefault();
    
    boolean isSameParentAsChild();
}
