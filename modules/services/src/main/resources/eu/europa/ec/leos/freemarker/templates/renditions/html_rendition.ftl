<#ftl encoding="UTF-8"
      output_format="HTML"
      auto_esc=true
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

<html>
<head>
<meta charset="UTF-8">
<link href="css/${styleSheetName}" rel="stylesheet" type="text/css" />
</head>
<body>
<#visit xml using xmlFtl>
</body>
</html>
