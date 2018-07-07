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
    Akoma Ntoso wrappers for blockContainer nodes
------------------------------------------------------------------------------>
<#-- AKN handler for edition, wrapped to activate/deactivate actions -->
<#macro blockContainer>
    <@wrapper/>
</#macro>

<#macro wrapper>
    <#local wrappedId = .node.@GUID[0]!>
    <#local wrappedType = .node?node_name>
    <#local editable = (.node["@leos:editable"][0]!'false') == 'true'>
    <#local deletable = (.node["@leos:deletable"][0]!'false') == 'true'>
<#if (editable || deletable)>
<div class="leos-wrap" data-wrapped-id="${wrappedId}" data-wrapped-type="${wrappedType}"
                       data-wrapped-editable="${editable?c}" data-wrapped-deletable="${deletable?c}">
    <div class="leos-wrap-content">
        <#fallback>
    </div>
    <div class="leos-wrap-widgets">
            <img src="VAADIN/themes/leos/icons/16/insert-before.png" data-widget-type="insert.before"/>
        <#if editable>
            <img src="VAADIN/themes/leos/icons/16/edit-text.png" data-widget-type="edit"/>
        </#if>
        <#if deletable>
            <img src="VAADIN/themes/leos/icons/16/cross.png" data-widget-type="delete"/>
        </#if>
            <img src="VAADIN/themes/leos/icons/16/insert-after.png" data-widget-type="insert.after"/>
    </div>
</div>
<#else>
<#-- generate content -->
    <#fallback>
</#if>
</#macro>