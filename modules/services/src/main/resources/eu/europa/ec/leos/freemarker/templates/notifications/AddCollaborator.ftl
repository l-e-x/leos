<#ftl encoding="UTF-8"
      output_format="HTML"
      auto_esc=true
      strict_syntax=true
      strip_whitespace=true
      strip_text=true>

<#--
    Copyright 2018 European Commission

    Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
    You may not use this work except in compliance with the Licence.
    You may obtain a copy of the Licence at:

        https://joinup.ec.europa.eu/software/page/eupl

    Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the Licence for the specific language governing permissions and limitations under the Licence.
-->
<#assign notification = .data_model.notification>
<#assign owners = notification.owners>
<#assign link = notification.link>
<#assign contributors = notification.contributors>
<#assign reviewers = notification.reviewers>
<#assign leosAuthority = notification.leosAuthorityName>
<#assign title = notification.title>

<#macro body>
<br>
<b>You have been added as a ${leosAuthority} on the following initiative:</b>
<br>
${title}
<br>
To collaborate to this initiative, click here <a href="${link}">here</a>
<br><br>
<#if owners?? && owners != "">
<b>Author(s): (to be contacted in case of questions)</b>
<br>
${owners}
<br><br>
</#if>
<#if contributors?? && contributors != "">
<b>Contributor(s):</b>
<br>
${contributors}
<br><br>
</#if>
<#if reviewers?? && reviewers != "">
<b>Reviewer(s):</b>
<br>
${reviewers}
<br><br>
</#if>
<br>
</#macro>
