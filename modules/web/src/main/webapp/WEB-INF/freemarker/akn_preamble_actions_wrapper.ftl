<#ftl encoding="UTF-8"
      strict_syntax=true
      strip_whitespace=true
      strip_text=true
      ns_prefixes={"D":"http://www.akomantoso.org/2.0",
                   "leos":"urn:eu:europa:ec:leos"}>

<#--

    Copyright 2015 European Commission

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
<#local citationsId=.node.@id[0]!"cits">
<span id="preamble_${citationsId}" class="leos-preamble"
      ondblclick="javascript:leg_editCitations('${citationsId}'); leg_hidePreambleActions('${citationsId}');"
      onmouseover="javascript:leg_showPreambleActions('${citationsId}');"
      onmouseout="javascript:leg_hidePreambleActions('${citationsId}');">
    <#-- wrap citations content -->
    <span id="pre_text_preamble_${citationsId}" class="leos-preamble-content">
        <#-- generate citations content -->
        <#fallback>
    </span>
    <#-- generate citations actions -->
    <span id="all_actions_${citationsId}" class="leos-actions">
        <img id="edit_icon_${citationsId}" src="VAADIN/themes/leos/icons/16/edit-text.png" width="16" height="16"
             onclick="javascript:leg_editCitations('${citationsId}'); leg_hidePreambleActions('${citationsId}');"
             title="Edit citations"/>
        <img id="lock_icon_${citationsId}" src="VAADIN/themes/leos/icons/24/lock-yellow.png" width="16" height="16"
             onclick="javascript:leg_hidePreambleActions('${citationsId}');"
             title="Citations locked by another user" class="leos-action-inactive"/>
    </span>
</span>
</#macro>

<#macro recitals>
<#local recitalsId=.node.@id[0]!"recs">
<span id="preamble_${recitalsId}" class="leos-preamble"
      ondblclick="javascript:leg_editRecitals('${recitalsId}'); leg_hidePreambleActions('${recitalsId}');"
      onmouseover="javascript:leg_showPreambleActions('${recitalsId}');"
      onmouseout="javascript:leg_hidePreambleActions('${recitalsId}');">
    <#-- wrap preamble content -->
    <span id="pre_text_preamble_${recitalsId}" class="leos-preamble-content">
        <#-- generate recitals content -->
        <#fallback>
    </span>
    <#-- generate recitals actions -->
    <span id="all_actions_${recitalsId}" class="leos-actions">
        <img id="edit_icon_${recitalsId}" src="VAADIN/themes/leos/icons/16/edit-text.png" width="16" height="16"
             onclick="javascript:leg_editRecitals('${recitalsId}'); leg_hidePreambleActions('${recitalsId}');"
             title="Edit recitals"/>
        <img id="lock_icon_${recitalsId}" src="VAADIN/themes/leos/icons/24/lock-yellow.png" width="16" height="16"
             onclick="javascript:leg_hidePreambleActions('${recitalsId}');"
             title="Recitals locked by another user" class="leos-action-inactive"/>
    </span>
</span>
</#macro>