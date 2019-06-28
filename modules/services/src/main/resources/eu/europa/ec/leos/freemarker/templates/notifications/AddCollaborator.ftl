<#ftl encoding="UTF-8"
output_format="HTML"
auto_esc=true
strict_syntax=true
strip_whitespace=true
strip_text=true>

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
<#assign notification = .data_model.notification>
<#assign collaborators = notification.collaboratorsMap>
<#assign notes = notification.collaboratorNoteMap>
<#assign plural = notification.collaboratorPlural>
<#assign link = notification.link>
<#assign leosAuthority = notification.leosAuthorityName>
<#assign title = notification.title>

<#macro body>
    <br>
    <b>You have been added as a ${leosAuthority} on the following initiative:</b>
    <br>
    ${title}
    <br>
    To collaborate to this initiative, click here <a href="${link}">here</a>
    <br>
    <#list collaborators?keys as key>
        <#local title = key>
        <#local note = notes[key]>
        <#local users = collaborators[key]>
        <#if users?? && users != "">
            <br>
            <b>${title}${plural} ${note}</b>
            <br>
            ${users}
            <br>
        </#if>
    </#list>
    <br>
</#macro>
