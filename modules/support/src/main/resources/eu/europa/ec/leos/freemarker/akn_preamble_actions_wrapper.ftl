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

    Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
    You may not use this work except in compliance with the Licence.
    You may obtain a copy of the Licence at:

        https://joinup.ec.europa.eu/software/page/eupl

    Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the Licence for the specific language governing permissions and limitations under the Licence.
-->

<#-----------------------------------------------------------------------------
    Akoma Ntoso wrappers for preamble nodes
------------------------------------------------------------------------------>
<#-- AKN preamble handler for edition, wrapped to activate/deactivate actions -->
<#macro citations>
    <@wrapper/>
</#macro>

<#macro recitals>
    <@wrapper/>
</#macro>

<#macro wrapper>
    <#local wrappedId =.node.@id[0]!>
    <#local wrappedType = .node?node_name>
<div class="leos-wrap" data-wrapped-id="${wrappedId}" data-wrapped-type="${wrappedType}">
    <div class="leos-wrap-content">
        <#fallback>
    </div>
    <div class="leos-wrap-widgets">
        <img src="VAADIN/themes/decide/icons/16/edit-text.png"
             data-widget-type="edit" width="16" height="16"/>
        <img src="VAADIN/themes/decide/icons/16/lock-yellow.png"
             data-widget-type="lock" width="16" height="16"
             class="leos-inactive"/>
    </div>
</div>
</#macro>