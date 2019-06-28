<#ftl encoding="UTF-8"
      output_format="HTML"
      auto_esc=false
      strict_syntax=true
      strip_whitespace=true>

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
<#import "../akn_xml_mapper.ftl" as xmlFtl>
<#assign xml=.data_model.xml_data>
<#assign styleSheetName = .data_model.styleSheetName>
<#assign toc_data = .data_model.toc_data>

<#setting url_escaping_charset="UTF-8">


<html>
<head>
    <meta charset="UTF-8">
    <link href="css/${styleSheetName}" rel="stylesheet" type="text/css" />
    <link href="css/jqtree.css" rel="stylesheet" type="text/css"/>
    <link href="css/leos-toc-rendition.css" rel="stylesheet" type="text/css"/>

    <script type="text/javascript">
        var toc_data = ${toc_data};
    </script>

    <script src="js/leos-toc-rendition.js"></script>
</head>


<body>
      <div class="renditionContainer">
          <div class="renditionTocContent">
              <span class="title">Table of content</span>

              <div id="notCompliantDiv" style="display: none; color: red; padding: 40px;">
                <span>TOC not available.</span> <br/>
                <span>Jquery found in the host is not compliant, should be 1.9+</span>
              </div>
              <div id="treeContainer"></div>
          </div>

          <div class="renditionAkomaNtosoContent">
             <#visit xml using xmlFtl>
          </div>
      </div>
</body>
</html>
