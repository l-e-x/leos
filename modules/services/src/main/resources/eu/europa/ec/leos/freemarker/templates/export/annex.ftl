<#ftl encoding="UTF-8"
      output_format="XML"
      auto_esc=true
      strict_syntax=true
      strip_whitespace=true
      strip_text=true
      ns_prefixes={}>

<#--
    Copyright 2017 European Commission

    Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
    You may not use this work except in compliance with the Licence.
    You may obtain a copy of the Licence at:

        https://joinup.ec.europa.eu/software/page/eupl

    Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the Licence for the specific language governing permissions and limitations under the Licence.
-->

<#-- FTL imports -->
<#-- XML variable to reference the input node model -->

<#macro annex proposal annex>
    <#local proposalRef = proposal.getResourceId()>
    <#local bill = proposal.getChildResource('bill')>
    <#local billRef = bill.getResourceId()>
    <#local annexes = bill.getChildResources('annex')>
    <#local annexRef = annex.getResourceId()>
    <importJob filename="${annex.getLeosCategory().name()?capitalize}_${annex.getDocNumber()}"
               convertAnnotations="true">
        <leos>
            <resource ref="${proposalRef}">
                <resource ref="${billRef}">
                    <resource ref="${annexRef}">
                    </resource>
                </resource>
            </resource>
        </leos>
        <formats>
            <legisWrite>
                <format>docx</format>
            </legisWrite>
        </formats>
    </importJob>
</#macro>
