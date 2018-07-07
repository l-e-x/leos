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
        <link rel="stylesheet" type="text/css" href="${webContextPath}/VAADIN/themes/leos/css/bill_xml.css"/>
        <script type="text/javascript" src="http://cdn.mathjax.org/mathjax/2.3-latest/MathJax.js?config=default"></script>
        <script type="text/javascript">
            function leg_scrollIntoView(eId) {
                var e = document.getElementById(eId);
                if (e){
                     if (e.scrollIntoView) {
                        e.scrollIntoView();
                    }
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