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

<#-- FTL imports -->
<#import "akn_article_actions_wrapper.ftl" as articleActionsFtl>
<#import "akn_preamble_actions_wrapper.ftl" as preambleActionsFtl>
<#import "akn_xml_mapper.ftl" as xmlFtl>
<#import "akn_generate_cover.ftl" as coverFtl>

<#-- XML variable to reference the input node model -->
<#assign xml=.data_model.xml_data>

<#include "set_global_variables.ftl" parse=true>

<#-----------------------------------------------------------------------------
    Template entry point for processing Akoma Ntoso XML tree
------------------------------------------------------------------------------>
<@compress single_line=true>
	<@coverFtl.coverPage/>
	<#visit xml.akomaNtoso using [preambleActionsFtl, articleActionsFtl, xmlFtl]>
</@compress>