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

<#-- FTL imports -->
<#import "bill_toc_html.ftl" as tocHtmlFtl>

<#-- FTL debug information -->
<#assign ftlFilename=.template_name>

<#macro tableOfContents>
<span id="leos-toc" class="leos-toc">
    <span class="leos-toc-header">Table of Contents</span>
    <#-- XML tree processing, starting from the bill node -->
    <#recurse .node.bill using tocHtmlFtl>
</span>
</#macro>