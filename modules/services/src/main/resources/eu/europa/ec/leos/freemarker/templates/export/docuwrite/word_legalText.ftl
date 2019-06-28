<#ftl encoding="UTF-8"
      output_format="XML"
      auto_esc=true
      strict_syntax=true
      strip_whitespace=true
      strip_text=true
      ns_prefixes={}>

<#--
    Copyright 2019 European Commission

    Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
    You may not use this work except in compliance with the Licence.
    You may obtain a copy of the Licence at:

        https://joinup.ec.europa.eu/software/page/eupl

    Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the Licence for the specific language governing permissions and limitations under the Licence.
-->
<#-- XML variable to reference the input node model -->
<#assign root=.data_model.resource_tree>

<#assign proposal = root>
<#assign proposalRef = proposal.getResourceId()>
<#assign bill = proposal.getChildResource('bill')>
<#assign billRef = bill.getResourceId()>
<#assign proposalCoverpageRef = proposal.getComponentId('coverPage')>

<@compress>
    <importOptions technicalKey="${proposal.getExportOptions().getTechnicalKey()!}">
        <importJob filename="${bill.getLeosCategory().name()?capitalize}"
            convertAnnotations="${proposal.getExportOptions().isConvertAnnotations()?c}" comparisonType="${proposal.getExportOptions().getComparisonType().getType()!}">
            <leos>
                <resource ref="${proposalRef}">
                    <excludes>
                        <exclude ref="${proposalCoverpageRef!}"/>
                    </excludes>
                    <resource ref="${billRef}">
                    </resource>
                </resource>
            </leos>
            <formats>
                <legisWrite>
                    <format>docx</format>
                </legisWrite>
            </formats>
        </importJob>
    </importOptions>
</@compress>
