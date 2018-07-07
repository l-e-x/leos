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

    Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
    You may not use this work except in compliance with the Licence.
    You may obtain a copy of the Licence at:

        https://joinup.ec.europa.eu/software/page/eupl

    Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the Licence for the specific language governing permissions and limitations under the Licence.
-->

<#import "bill_toc.ftl" as htmltocFtl>
<#import "akn_generate_cover.ftl" as aknCoverFtl>

<#-----------------------------------------------------------------------------
    Akoma Ntoso wrappers for XML nodes
------------------------------------------------------------------------------>

<#macro akomaNtoso>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
        <link rel="stylesheet" type="text/css" href="${webContextPath}/static/decide/css/bill_xml.css"/>
        <script type="text/javascript" src="${webContextPath}/webjars/MathJax/2.5.3/MathJax.js?config=default"></script>
        <script type="text/javascript">
            function nav_navigateToContent(elementId) {
                var element = document.getElementById(elementId);
                if (element) {
                    var bgColor = element.style.backgroundColor;
                    element.style.backgroundColor = "cornsilk";
                    setTimeout(function () {
                        element.style.backgroundColor = bgColor;
                    }, 500);
                    element.scrollIntoView(true);
                }
            }
        </script>
    </head>
    <body xmlns:leos="urn:eu:europa:ec:leos">
        <#-- generate coverpage -->
        <@aknCoverFtl.coverPage/>

        <#-- generate table of content -->
        <@htmltocFtl.tableOfContents/>
        
        <#-- generate akomaNtoso content -->
        <#fallback>
        
        <#-- convert and show math formulas -->
        <script type="text/javascript"> 
            MathJax.Hub.Queue('["Typeset", MathJax.Hub]');
            MathJax.Hub.Config('{"HTML-CSS": {imageFont: null}}');
        </script> 
    </body>
  </html>
</#macro>

<#-- This removes all comments from compare and html/pdf preview  -->
<#macro popup></#macro>

<#-- This removes all highlights from compare and html/pdf preview  -->
<#macro span>
    <#if (.node["@refersto"][0]!'') == '~leoshighlight' ><#t>
        <#recurse><#t>
    <#else><#t>
        <#fallback><#t>
    </#if><#t>
</#macro>