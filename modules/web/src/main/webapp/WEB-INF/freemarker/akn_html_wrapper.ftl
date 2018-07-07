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
        <link rel="stylesheet" type="text/css" href="${webContextPath}/static/leos/css/bill_xml.css"/>
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
			function leg_setDataHint(eId) {
			    var element = document.getElementById(eId);
			    if(element) {
			        var datahint='';
			        var userId = element.hasAttribute("leos:userid") ? element.getAttribute("leos:userid") : '';
			        var userName = element.hasAttribute("leos:username") ? element.getAttribute("leos:username") : '';
			        var timestamp = element.hasAttribute("leos:datetime") ? element.getAttribute("leos:datetime") : new Date();
			        var commentText = element.hasAttribute("data-text") ? element.getAttribute("data-text") : undefined;
			
			        var formattedDate = getFormattedDate(new Date(timestamp));
			        if(element.localName === "popup") {//Comments
			            datahint = commentText + "\n------------------------------------\n[Commented by: "+ userName + "(" + userId +")" + " - " + formattedDate + "]";  
			        } else{//Highlight
			            datahint = "[Highlighted by: "+ userName + "(" + userId +")" + " - " + formattedDate + "]";  
			        }
			        element.setAttribute("data-hint", datahint);
			    }
			}
			function getFormattedDate(date) {
			    var year = date.getFullYear(), month = date.getMonth(), day = date.getDate(), hours = date.getHours(), minutes = date.getMinutes();
			    return year + "-" + addZero(month + 1) + "-" + addZero(day) + " " + addZero(hours) +":"+ addZero(minutes);
			}
			function addZero(i) {
			    return ("0" + i).slice(-2);
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