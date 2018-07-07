<#ftl encoding="UTF-8"
      strict_syntax=true
      strip_whitespace=true
      strip_text=true
      ns_prefixes={"D":"http://www.akomantoso.org/2.0",
                   "leos":"urn:eu:europa:ec:leos"}>
<#--

    Copyright 2015 European Commission

    Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
    You may not use this work except in compliance with the Licence.
    You may obtain a copy of the Licence at:

        https://joinup.ec.europa.eu/software/page/eupl

    Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the Licence for the specific language governing permissions and limitations under the Licence.

-->

<#-- Hash of mapped Akoma Ntoso XML elements where:
       Key = Akoma Ntoso element name
     Value = Mapped element name
-->
<#assign aknMapped={
    'body':'aknBody',
    'title':'aknTitle',
    'p':'aknP'
}>

<#assign authorialNoteList = []>

<#-- Sequence of ignored Akoma Ntoso XML elements -->
<#assign aknIgnored=[
    'meta'
]>

<#-----------------------------------------------------------------------------
    Akoma Ntoso handlers for XML nodes
------------------------------------------------------------------------------>
<#-- AKN root element handler -->
<#macro akomaNtoso>
    <akomaNtoso id="${.node.@id[0]!'akomaNtoso'}">
        <#recurse>
        <@printAuthorialNotes/>
    </akomaNtoso>
</#macro>

<#-- AKN  citations handler -->
<#macro citations>
    <@@element/>
</#macro>

<#-- AKN  recitals handler -->
<#macro recitals>
    <@@element/>
</#macro>

<#-- AKN article element handler -->
<#macro article>
    <@@element/>
</#macro>

<#-----------------------------------------------------------------------------
 AKN authorial note handler -->
------------------------------------------------------------------------------>
<#macro authorialNote>
    <#local marker=.node.@marker[0]!'*'>
    <#local noteText=.node.@@text?trim?xml>
    <#local noteId=.node.@id[0]!''>
    <#if (noteId?length gt 0)>
        <authorialNote id="${noteId}" data-tooltip="${noteText}" onClick="leg_scrollIntoView('endNote_${noteId}')">${marker}</authorialNote>
    <#else>
    	<authorialNote data-tooltip="${noteText}">${marker}</authorialNote>
    </#if>
	<#assign authorialNoteList = authorialNoteList + [getAuthorialNoteSpan(noteId, marker, noteText)]>
</#macro>

<#-- AKN end-of-line handler -->
<#macro eol>
<br/>
</#macro>

<#-- create the representation of authorialNote -->
<#function getAuthorialNoteSpan noteId marker noteText>
       <#if (noteId?length gt 0)>
           <#return '<span id="endNote_${noteId}" class="leos-authnote" onClick="leg_scrollIntoView(\'${noteId}\')"><marker id="marker_${noteId}">${marker}</marker><text id="text_${noteId}">${noteText}</text></span>'>
        <#else>
           <#return '<span class="leos-authnote"><marker>${marker}</marker><text>${noteText}</text></span>'>
        </#if>
</#function>

<#-- print the footnotes in document -->
<#macro printAuthorialNotes>
	<#if authorialNoteList?has_content>
    	<span id="leos-authnote-table-id" class="leos-authnote-table">
        	<hr size="2"/>
	        <#list authorialNoteList as authNote>
    	        ${authNote}
        	 </#list>
	    </span>
	 </#if>
</#macro>

<#-----------------------------------------------------------------------------
    Default handlers for XML nodes
------------------------------------------------------------------------------>
<#-- default handler for element nodes -->
<#macro @element><#local nodeName=.node?node_name><#if (!aknIgnored?seq_contains(nodeName))><#local nodeTag=aknMapped[nodeName]!nodeName><#if (.node.@@attributes_markup?length gt 0)><${nodeTag} ${.node.@@attributes_markup}><#recurse></${nodeTag}><#else><${nodeTag}><#recurse></${nodeTag}></#if></#if></#macro>

<#-- default handler for text nodes -->	
<#macro @text>${.node?xml}</#macro>