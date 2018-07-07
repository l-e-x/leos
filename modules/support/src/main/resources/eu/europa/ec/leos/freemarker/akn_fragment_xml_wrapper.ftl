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
<#assign xml=.data_model.xml_data>

<#assign aknMapped={
    'p':'aknP'
}>

<#-- Sequence of ignored Akoma Ntoso XML elements -->
<#assign aknIgnored=['meta', 'popup']>

<#-----------------------------------------------------------------------------
Entry point
------------------------------------------------------------------------------>
<@compress single_line=true>
    <#visit xml>
</@compress>

<#macro aknFragment>
<aknFragment>
    <bill>
	    <#recurse>
    </bill>
</aknFragment>
</#macro>

<#-----------------------------------------------------------------------------
 AKN authorial note handler -->
------------------------------------------------------------------------------>
<#macro authorialNote>
    <authorialNote>${.node.@marker[0]!'*'}</authorialNote><#t>
</#macro>

<#-----------------------------------------------------------------------------
Cross Reference handler -->
------------------------------------------------------------------------------>
<#macro ref>
    <ref><#recurse></ref><#t>
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