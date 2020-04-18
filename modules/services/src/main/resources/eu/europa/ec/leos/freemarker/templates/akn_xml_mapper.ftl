<#ftl encoding="UTF-8"
      output_format="XML"
      auto_esc=true
      strict_syntax=true
      strip_whitespace=true
      strip_text=true
      ns_prefixes={"D":"http://docs.oasis-open.org/legaldocml/ns/akn/3.0",
                   "leos":"urn:eu:europa:ec:leos",
                    "xml":"http://www.w3.org/XML/1998/namespace"}>

<#--
    Copyright 2017 European Commission

    Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
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
    'p':'aknP',
    'xml:id':'id'
}>

<#assign authorialNoteList = []>

<#-- Sequence of ignored Akoma Ntoso XML elements -->
<#assign aknIgnored=[
    'meta'
]>


<#macro akomaNtoso>
<akomaNtoso id="${getLeosRef(.node)}">
   <#recurse/>
</akomaNtoso>
</#macro>

<#macro bill>
    <#local nodeName=.node?node_name>
    <${nodeName}${handleAttributes(.node.@@)?no_esc}><#recurse><@printAuthorialNotes/></${nodeName}><#t>
</#macro>

<#macro doc>
    <#local nodeName=.node?node_name>
    <${nodeName}${handleAttributes(.node.@@)?no_esc}><#recurse><@printAuthorialNotes/></${nodeName}><#t>
</#macro>

<#-----------------------------------------------------------------------------
 AKN cover page specific handlers
------------------------------------------------------------------------------>
<#macro container>
    <#local language = (.node["@name"][0]!'') == 'language'>
    <#if (language)>
    <container id="${.node["@xml:id"][0]!}" name="language" data-lang="${.node.p}"/>
    <#else>
        <@@element/>
    </#if>
</#macro>
<#-----------------------------------------------------------------------------
 AKN authorial note handler 
------------------------------------------------------------------------------>
<#macro authorialNote>
	<#assign authorialNoteList = authorialNoteList + [.node]>
    <#local noteId=.node["@xml:id"][0]!''>
    <#if (noteId?length gt 0)>
        <authorialNote${handleAttributes(.node.@@)?no_esc} onClick="LEOS.scrollTo('endNote_${noteId}')"><#t>
        <#recurse><#t>
        </authorialNote><#t>
    <#else>
    	<authorialNote${handleAttributes(.node.@@)?no_esc}><#recurse></authorialNote><#t>
    </#if>
</#macro>

<#-----------------------------------------------------------------------------
Cross Reference handler
------------------------------------------------------------------------------>
<#macro ref>
    <#local refId=.node.@href[0]!''>
    <ref${handleAttributes(.node.@@)?no_esc} onClick="LEOS.scrollTo('${refId}')"><#recurse></ref><#t>
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
            <#local noteId=authNote["@xml:id"][0]!''>
            <#if (noteId?length gt 0)>
                <span id="endNote_${noteId}" class="leos-authnote" onClick="LEOS.scrollTo('${noteId}')">
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
        <${nodeTag}${handleAttributes(.node.@@)?no_esc}><#recurse></${nodeTag}><#t>
    </#if>
</#macro>

<#-- default handler for text nodes -->
<#macro @text>
    <#if .node?trim?length gt 0>
        ${.node}<#t>
    </#if>
</#macro>

<#-----------------------------------------------------------------------------
    Common function to generate updated attributes for XML nodes
------------------------------------------------------------------------------>
<#function handleAttributes attrList auto_esc=false>
    <#assign str = ''>
    <#if (attrList?size gt 0)>
        <#list attrList as attr>
            <#local attrName=aknMapped[attr.@@qname]!attr.@@qname>
            <#assign str += ' ${attrName}="${attr}"'>
        </#list>
    </#if>
    <#return str>
</#function>

<#function getLeosRef node auto_esc=false>
    <#assign refNode = .node["//leos:ref"]>
    <#return refNode?has_content?then(refNode.@@text, 'akomaNtoso')>
</#function>
