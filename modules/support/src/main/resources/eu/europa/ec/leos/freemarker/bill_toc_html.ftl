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

<#-- handler for preface -->
<#macro preface>
    <span class="leos-toc-item" id="toc-item-${.node.@id[0]!}">
        <a class="leos-toc-link" href="#${.node.@id[0]!}">Preface</a>
    </span>
</#macro>

<#-- handler for preamble -->
<#macro preamble>
    <span class="leos-toc-item" id="toc-item-${.node.@id[0]!}">
        <a class="leos-toc-link" href="#${.node.@id[0]!}">Preamble</a>
    </span>
</#macro>

<#-- handler for body. Using the first child of aknbody as aknbody anchor had problems with PDF -->
<#macro body>
    <span class="leos-toc-item" id="toc-item-${.node.@id[0]!}">
        <a class="leos-toc-link" id="toc-${.node.@id[0]!}" href="#${.node.*[0].@id[0]!}">Enacting Terms</a>
        <#recurse>
    </span>
</#macro>

<#-- handler for conclusions -->
<#macro conclusions>
    <span class="leos-toc-item" id="toc-item-${.node.@id[0]!}">
        <a class="leos-toc-link" href="#${.node.@id[0]!}">Signature</a>
    </span>
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

    <span class="leos-toc-item" id="toc-item-${.node.@id[0]!}">
        <a class="leos-toc-link" id="toc-${.node.@id[0]!}" href="#${.node.@id[0]!}">${tocName!}</a>
        <#recurse>
    </span>
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