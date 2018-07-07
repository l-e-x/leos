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
<#assign xml=.data_model.xml_data>

<#assign aknMapped={
    'p':'aknP'
}>

<#-- Sequence of ignored Akoma Ntoso XML elements -->
<#assign aknIgnored=[
    'meta','popup'
]>
<#-----------------------------------------------------------------------------
Entry point
------------------------------------------------------------------------------>
<#compress>
<#visit xml>
</#compress>

<#macro aknFragment>
<aknFragment><bill>
	<#recurse>
</bill></aknFragment>
</#macro>
<#-----------------------------------------------------------------------------
 AKN authorial note handler -->
------------------------------------------------------------------------------>
<#macro authorialNote>
<#local marker=.node.@marker[0]!'*'>
<authorialNote>${marker}</authorialNote></#macro>

<#-----------------------------------------------------------------------------
Cross Reference handler -->
------------------------------------------------------------------------------>
<#macro ref><ref><#recurse></ref>
</#macro>

<#-----------------------------------------------------------------------------
    Default handlers for XML nodes
------------------------------------------------------------------------------>
<#-- default handler for element nodes -->
<#macro @element><#local nodeName=.node?node_name><#if (!aknIgnored?seq_contains(nodeName))><#local nodeTag=aknMapped[nodeName]!nodeName><#if (.node.@@attributes_markup?length gt 0)><${nodeTag} ${.node.@@attributes_markup}><#recurse></${nodeTag}><#else><${nodeTag}><#recurse></${nodeTag}></#if></#if></#macro>

<#-- default handler for text nodes -->	
<#macro @text>${.node?xml}</#macro>