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
    Akoma Ntoso wrappers for XML nodes
------------------------------------------------------------------------------>
<#-- AKN article handler for edition, wrapped to activate/deactivate actions -->

<#macro article>
    <#local articleId = .node.@id[0]!>
    <#local editable = (.node["@leos:editable"][0]!'true') == 'true'>
    <#local deletable = (.node["@leos:deletable"][0]!'true') == 'true'>
<#if (editable || deletable)>
<span id="art_articleId_${articleId}" class="leos-article"
    <#if editable>
      ondblclick="javascript:leg_editArticle('${articleId}');  leg_hideArticleActions('${articleId}');"
    </#if>
      onmouseover="javascript:leg_showArticleActions('${articleId}');"
      onmouseout="javascript: leg_hideArticleActions('${articleId}');">
    <#-- wrap article content -->
    <span id="art_txt_${articleId}" class="leos-article-content">
        <#-- generate article content -->
        <#fallback>
    </span>
    <#-- generate article actions -->
    <span id="all_actions_${articleId}" class="leos-actions">
            <img id="ins_bef_icon_${articleId}" src="VAADIN/themes/leos/icons/16/insert-before.png" width="16" height="16"
                 onclick="javascript:leg_insertArticleBefore('${articleId}');  leg_hideArticleActions('${articleId}');"
                 title="Insert article before"/>
        <#if editable>
            <img id="edit_icon_${articleId}" src="VAADIN/themes/leos/icons/16/edit-text.png" width="16" height="16"
                 onclick="javascript:leg_editArticle('${articleId}');  leg_hideArticleActions('${articleId}');" 
                 title="Edit Text"/>
        </#if>
        <#if deletable>
            <img id="delete_icon_${articleId}" src="VAADIN/themes/leos/icons/16/cross.png" width="16" height="16"
                 onclick="javascript:leg_deleteArticle('${articleId}');  leg_hideArticleActions('${articleId}');"
                 title="Delete article"/>
        </#if>
            <img id="ins_aft_icon_${articleId}" src="VAADIN/themes/leos/icons/16/insert-after.png" width="16" height="16"
                 onclick="javascript:leg_insertArticleAfter('${articleId}');  leg_hideArticleActions('${articleId}');"
                 title="Insert article after"/>
            <img id="lock_icon_${articleId}" src="VAADIN/themes/leos/icons/24/lock-yellow.png" width="16" height="16"
                 onclick="javascript:leg_hideArticleActions('${articleId}');"
                 title="Article locked by another user" class="leos-action-inactive"/>
    </span>
</span>
<#else>
    <#-- generate article content -->
    <#fallback>
</#if>
</#macro>