<#ftl encoding="UTF-8"
      output_format="XML"
      auto_esc=true
      strict_syntax=true
      strip_whitespace=true
      strip_text=true
      ns_prefixes={"D":"http://www.akomantoso.org/2.0",
                   "leos":"urn:eu:europa:ec:leos"}>

<#--
    Copyright 2016 European Commission

    Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
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
	<#assign authorialNoteList = authorialNoteList + [.node]>
    <#local marker=.node.@marker[0]!'*'>
    <#local noteText=.node.@@text?trim>
    <#local noteId=.node.@id[0]!''>
    <#if (noteId?length gt 0)>
        <authorialNote id="${noteId}" data-tooltip="${noteText}" onClick="nav_navigateToContent('endNote_${noteId}')">${marker}</authorialNote><#t>
    <#else>
    	<authorialNote data-tooltip="${noteText}">${marker}</authorialNote><#t>
    </#if>
</#macro>

<#-----------------------------------------------------------------------------
Cross Reference handler -->
------------------------------------------------------------------------------>
<#macro ref>
    <#local refId=.node.@href[0]!''>
    <ref ${.node.@@attributes_markup?no_esc} onClick="nav_navigateToContent('${refId}')"><#recurse></ref><#t>
</#macro>

<#-- AKN end-of-line handler -->
<#macro eol>
<br/>
</#macro>

<#-- print the footnotes in document -->
<#macro printAuthorialNotes>
    <#list authorialNoteList>
        <span id="leos-authnote-table-id" class="leos-authnote-table">
            <hr size="2"/>
        <#items as authNote>
            <#local noteMarker=authNote.@marker[0]!'*'>
            <#local noteText=authNote.@@text?trim>
            <#local noteId=authNote.@id[0]!''>
            <#if (noteId?length gt 0)>
                <span id="endNote_${noteId}" class="leos-authnote" onClick="nav_navigateToContent('${noteId}')">
                    <marker id="marker_${noteId}">${noteMarker}</marker>
                    <text id="text_${noteId}">${noteText}</text>
                </span>
            <#else>
                <span class="leos-authnote">
                    <marker>${noteMarker}</marker>
                    <text>${noteText}</text>
                </span>
            </#if>
        </#items>
        </span>
    </#list>
</#macro>

<#-----------------------------------------------------------------------------
    Default handlers for XML nodes
------------------------------------------------------------------------------>
<#-- default handler for element nodes -->
<#macro @element>
    <#local nodeName=.node?node_name>
    <#if (!aknIgnored?seq_contains(nodeName))>
        <#local nodeTag=aknMapped[nodeName]!nodeName>
        <#if (.node.@@?size gt 0)>
            <${nodeTag} ${.node.@@attributes_markup?no_esc}><#recurse></${nodeTag}><#t>
        <#else>
            <${nodeTag}><#recurse></${nodeTag}><#t>
        </#if>
    </#if>
</#macro>

<#-- default handler for text nodes -->
<#macro @text>
    <#if .node?trim?length gt 0>
        ${.node}<#t>
    </#if>
</#macro>