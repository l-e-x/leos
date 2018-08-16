<#ftl encoding="UTF-8"
      output_format="XML"
      auto_esc=true
      strict_syntax=true
      strip_whitespace=true
      strip_text=true
      ns_prefixes={"D":"http://docs.oasis-open.org/legaldocml/ns/akn/3.0",
                   "leos":"urn:eu:europa:ec:leos"}>

<#--
    Copyright 2017 European Commission

    Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
    You may not use this work except in compliance with the Licence.
    You may obtain a copy of the Licence at:

        https://joinup.ec.europa.eu/software/page/eupl

    Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the Licence for the specific language governing permissions and limitations under the Licence.
-->

<#-----------------------------------------------------------------------------
    Akoma Ntoso wrappers for XML nodes
------------------------------------------------------------------------------>
<#-- AKN article handler for edition, wrapped to activate/deactivate actions -->
<#macro article>
    <@@createWrapper/>
</#macro>

<#macro recital>
    <@@createWrapper/>
</#macro>

<#macro @createWrapper>
    <#local elementId = .node.@GUID[0]!>
    <#local elementName=.node?node_name>
    <#local compliant = (.node["@leos:compliant"][0]!'true') == 'true'>
    <#if (elementId?length gt 0) && (compliant)>
        <div class="leos-import-wrapper">
            <input type="checkbox" data-element-type="import" data-wrapped-type="${elementName}" value="${elementId}">
            <div class="leos-wrapped-content">
                <@xmlFtl.@element/>
            </div>
        </div>
    <#else>
        <div class="leos-non-import-wrapper">
            <@xmlFtl.@element/>
        </div>
    </#if>
</#macro>
