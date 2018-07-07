<#ftl encoding="UTF-8"
      strict_syntax=true
      strip_whitespace=true
      strip_text=true
      ns_prefixes={"D":"http://www.akomantoso.org/2.0",
                   "leos":"urn:eu:europa:ec:leos"}>

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