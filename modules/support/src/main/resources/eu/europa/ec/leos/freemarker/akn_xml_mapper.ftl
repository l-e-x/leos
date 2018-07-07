<#ftl encoding="UTF-8"
      strict_syntax=true
      strip_whitespace=true
      strip_text=true
      ns_prefixes={"D":"http://www.akomantoso.org/2.0",
                   "leos":"urn:eu:europa:ec:leos"}>
<#--

    Copyright 2016 European Commission

    Licensed under the EUPL, Version 1.1 or � as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
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
<#macro bill>
    <bill id="${.node.@id[0]!'bill'}">
        <#recurse>
    </bill>
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
    <#local marker=.node.@marker[0]!'*'><#t>
    <#local noteText=.node.@@text?trim?xml><#t>
    <#local noteId=.node.@id[0]!''><#t>
    <#if (noteId?length gt 0)><#t>
        <authorialNote id="${noteId}" data-tooltip="${noteText}" onClick="nav_navigateToContent('endNote_${noteId}')">${marker}</authorialNote><#t>
    <#else><#t>
    	<authorialNote data-tooltip="${noteText}">${marker}</authorialNote><#t>
    </#if><#t>
	<#assign authorialNoteList = authorialNoteList + [getAuthorialNoteSpan(noteId, marker, noteText)]><#t>
</#macro>

<#-----------------------------------------------------------------------------
popup tag handler -->
------------------------------------------------------------------------------>
<#macro popup>
<#t><popup data-leosComments="popover" data-content='${.node.@@text?trim?xml}' ${.node.@@attributes_markup}></popup>
</#macro>
<#-----------------------------------------------------------------------------
Cross Reference handler -->
------------------------------------------------------------------------------>
<#macro ref>
<#local refId=.node.@href[0]!''>
<ref ${.node.@@attributes_markup} onClick="nav_navigateToContent('${refId}')"><#recurse></ref></#macro>

<#-- AKN end-of-line handler -->
<#macro eol>
<br/>
</#macro>

<#-- create the representation of authorialNote -->
<#function getAuthorialNoteSpan noteId marker noteText>
       <#if (noteId?length gt 0)>
           <#return '<span id="endNote_${noteId}" class="leos-authnote" onClick="nav_navigateToContent(\'${noteId}\')"><marker id="marker_${noteId}">${marker}</marker><text id="text_${noteId}">${noteText}</text></span>'>
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