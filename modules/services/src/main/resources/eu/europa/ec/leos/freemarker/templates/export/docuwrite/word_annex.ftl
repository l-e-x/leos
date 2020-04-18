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
<#import "annex_single_resource.ftl" as annexFtl>

<#-- XML variable to reference the input node model -->
<#assign root=.data_model.resource_tree>

<#assign proposal = root>
<#assign proposalRef = proposal.getResourceId()>
<#assign bill = proposal.getChildResource('bill')>
<#assign billRef = bill.getResourceId()>
<#assign annexes = bill.getChildResources('annex')>
<#assign proposalCoverpageRef = proposal.getComponentId('coverPage')>

<@compress>
    <importOptions technicalKey="${proposal.getExportOptions().getTechnicalKey()!}">
        <#list annexes as annex>
            <@annexFtl.annex proposal annex/>
        </#list>
    </importOptions>
</@compress>
