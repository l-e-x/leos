<#ftl encoding="UTF-8"
      strict_syntax=true
      strip_whitespace=true
      strip_text=true
      ns_prefixes={"D":"http://www.akomantoso.org/2.0",
                   "leos":"urn:eu:europa:ec:leos"}>

<#-- FTL imports -->
<#import "akn_xml_mapper.ftl" as xmlFtl>
<#import "akn_generate_cover.ftl" as coverFtl>

<#-- XML variable to reference the input node model -->
<#assign xml=.data_model.xml_data>

<#include "set_global_variables.ftl" parse=true>

<#-----------------------------------------------------------------------------
    Template entry point for processing Akoma Ntoso XML tree
------------------------------------------------------------------------------>
<#compress>
    <@coverFtl.coverPage/>
    <#visit xml.akomaNtoso using [xmlFtl]>
</#compress>