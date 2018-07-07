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
<#import "akn_xml_mapper.ftl" as aknFtl>

<#assign xml=.data_model.xml_data>

<#macro coverPage>
<span id="leos-coverpage" class="leos-coverpage">
    <span id="leos-cover-logo" class="leos-cover-logo">
        <img src="${webContextPath}/static/decide/img/logo-ce-horizontal-en.png"
             width="326" height="86"
             alt="European Commission"
             title="European Commission"/>
    </span>
    <span id="leos-cover-cote" class="leos-cover-cote"> Brussels, 
    		<span class="leos-cover-placeholder">XXX</span><br/>
    		<span class="leos-cover-placeholder">[...]</span> (2014)  
    		<span class="leos-cover-placeholder">XXX</span> draft
    </span>
    <span id="leos-cover-preface" class="leos-preface-on-cover">
        <#if (xml.akomaNtoso.bill.preface.longTitle.p.docTitle?size gt 0)>
            <#visit xml.akomaNtoso.bill.preface.longTitle.p.docTitle using aknFtl>
        </#if>
    </span>
    <span class="leos-cover-legal-text">LEGAL TEXT</span>
    <span id="cover-lang" class="leos-cover-lang" data-lang="EN"></span>
</span>
</#macro>