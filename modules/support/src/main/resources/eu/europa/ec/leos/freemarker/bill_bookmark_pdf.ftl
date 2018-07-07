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

<#-- FTL debug information -->
<#assign ftlFilename=.template_name>

<#-- Sequence of XML elements that should be included in the Table of Contents -->
<#assign tocElements=['part', 'title', 'chapter', 'section', 'subsection', 'article']>

<#macro bookmarks>
<#if generatePdfBookmarks>
    <bookmarks>
        <#-- XML tree processing, starting from the bill node -->
        <#recurse xml.akomaNtoso.bill>
    </bookmarks>
</#if>
</#macro>

<#-- handler for preface -->
<#macro preface>
    <bookmark name="Preface" href="#preface"/>
</#macro>

<#-- handler for preamble -->
<#macro preamble>
    <bookmark name="Preamble" href="#preamble"/>
</#macro>

<#-- handler for body -->
<#macro body>
    <bookmark name="Enacting Terms" href="#body">
        <#recurse>
    </bookmark>
</#macro>

<#-- handler for conclusions -->
<#macro conclusions>
    <bookmark name="Signature" href="#conclusions"/>
</#macro>

<#-- default handler for toc items -->
<#macro tocItem>
    <#if (.node.num??)>
        <#local tocNum=.node.num.@@text?trim>
        <#if (tocNum?length gt 0)>
            <#local tocName=tocNum>
        </#if>
    </#if>

    <#if (.node.heading??)>
        <#local tocDesc=.node.heading.@@text?trim>
        <#if (tocDesc?length gt 0)>
            <#if (tocName?? && tocName?length gt 0)>
                <#local tocName=tocName + " - " + tocDesc>
            <#else>
                <#local tocName=tocDesc>
            </#if>
        </#if>
    </#if>

    <bookmark name="${tocName!}" href="#${.node.@id[0]!}">
        <#recurse>
    </bookmark>
</#macro>

<#-- default handler for element nodes -->
<#macro @element>
    <#if (tocElements?seq_contains(.node?node_name))>
        <@tocItem/>
    </#if>
</#macro>

<#-- default handler for text nodes -->
<#macro @text>
<#compress>${.node?trim}</#compress>
</#macro>