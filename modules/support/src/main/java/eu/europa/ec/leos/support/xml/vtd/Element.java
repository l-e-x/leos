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
package eu.europa.ec.leos.support.xml.vtd;

/**
 * @author: micleva
 * @date: 4/22/13 10:32 AM
 * @project: ETX
 */
public final class Element {

	private final int navigationIndex;
	private final int nodeIndex;
	private final boolean hasTextChild;
	private String tagIdentifier;
	private String tagContent;
 private String tagName;
 private String fullContent;//only for text matching purpose
 
	public Element(int navigationIndex, String tagId, String tagContent, int nodeIndex, boolean hasTextChild) {
		this.navigationIndex = navigationIndex;
		this.nodeIndex = nodeIndex;
		this.hasTextChild = hasTextChild;
		this.tagContent = tagContent;
		this.tagIdentifier = tagId!=null? tagId.replaceAll(" ", ""):null;
//		if(hasTextChild) {
//			this.tagIdentifier += "_" + nodeIndex;
//		}
	}
	public Element(int navigationIndex, String tagId,String tagName, String tagContent, int nodeIndex, boolean hasTextChild, String fullContent) {
	    this( navigationIndex,  tagId,  tagContent,  nodeIndex,  hasTextChild);
	    this.tagName=tagName;
	    this.fullContent=fullContent.replaceAll("<.*?>|\\r\\n","");
	}
	
	public String getTagName(){
	    return tagName;
	}
	
	public int getApproxLength(){
     return fullContent.length();
 }
 
	public String getTagId(){
     return tagIdentifier;
 }
	
	public int getNavigationIndex() {
		return navigationIndex;
	}
 
	public int getNodeIndex() {
     return nodeIndex;
 }
 
	public String getTagContent() {
		return tagContent;
	}

	public boolean hasTextChild() {
		return hasTextChild;
	}

	@Override
 public boolean equals(Object o) {
  if (this == o) return true;
  if (o == null || getClass() != o.getClass()) return false;

  Element element = (Element) o;

  if (tagIdentifier != null && element.tagIdentifier != null 
          && tagIdentifier.equals(element.tagIdentifier)) 
      return true;

  return false;
 }

	@Override
	public int hashCode() {
		return tagIdentifier != null ? tagIdentifier.hashCode() : tagName.hashCode();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Element");
		sb.append("{navigationIndex=").append(navigationIndex);
  sb.append("{tagName=").append(tagName);
		sb.append(", nodeIndex=").append(nodeIndex);
		sb.append(", tagIdentifier='").append(tagIdentifier).append('\'');
		sb.append('}');
		return sb.toString();
	}
	
	
	//used from http://stackoverflow.com/questions/955110/similarity-string-comparison-in-java
	public  double contentSimilarity(Element element) {
	    String s1=this.fullContent;
	    String s2=element.fullContent;
	    
	    String longer = s1, shorter = s2;
	    if (s1.length() < s2.length()) { // longer should always have greater length
	        longer = s2; shorter = s1;
	    }
	    int longerLength = longer.length();
	    if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
	    /* // If you have StringUtils, you can use it to calculate the edit distance:
	      return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) /
	                                 (double) longerLength; */
	    return (longerLength - editDistance(longer, shorter)) / (double) longerLength;

	}

	// Example implementation of the Levenshtein Edit Distance
	// See http://rosettacode.org/wiki/Levenshtein_distance#Java
	private int editDistance(String s1, String s2) {
	    s1 = s1.toLowerCase();
	    s2 = s2.toLowerCase();

	    int[] costs = new int[s2.length() + 1];
	    for (int i = 0; i <= s1.length(); i++) {
	        int lastValue = i;
	        for (int j = 0; j <= s2.length(); j++) {
	            if (i == 0)
	                costs[j] = j;
	            else {
	                if (j > 0) {
	                    int newValue = costs[j - 1];
	                    if (s1.charAt(i - 1) != s2.charAt(j - 1))
	                        newValue = Math.min(Math.min(newValue, lastValue),
	                                costs[j]) + 1;
	                    costs[j - 1] = lastValue;
	                    lastValue = newValue;
	                }
	            }
	        }
	        if (i > 0)
	            costs[s2.length()] = lastValue;
	    }
	    return costs[s2.length()];
	}
}
