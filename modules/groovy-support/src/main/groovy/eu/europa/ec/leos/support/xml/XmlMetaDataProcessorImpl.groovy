/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.support.xml

import org.springframework.stereotype.Component

import eu.europa.ec.leos.vo.MetaDataVO
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilderHelper
import groovy.xml.StreamingMarkupBuilder

@Component
class XmlMetaDataProcessorImpl  implements XmlMetaDataProcessor {

    @Override
    @CompileStatic
    def String toXML(MetaDataVO metaVo) {
        toXML(metaVo, null);
    }
    
    
     @Override
    def String toXML(MetaDataVO metaVo, String xmlString) {
        
        //create or get meta tag
        GPathResult metaNode = new XmlSlurper(false,false).parseText((xmlString==null)
                                                                            ?createMetaSection(metaVo)
                                                                            :xmlString)
        //create or update language section
        updateLanguageSection(metaNode, metaVo)

        //create or update proprietary section section
        updateProprietarySection(metaNode, metaVo)

        def builder = new StreamingMarkupBuilder()
        builder.setUseDoubleQuotes(true)
        return builder.bind{ mkp.yield metaNode} 
    }

   
    
    @Override
    def MetaDataVO fromXML(String xmlString){

        XmlSlurper xmlSlurper = new XmlSlurper(false, false)
        GPathResult metaNode=xmlSlurper.parseText(xmlString)
        
        MetaDataVO metaVo = new MetaDataVO()

        //populate values from proprietary node
        GPathResult proprietaryNode=metaNode.'*'.find{it.name()=="proprietary" && it.@source =="~${DEFAULT_SOURCE}" }
        metaVo.template =proprietaryNode.'leos:template'.text()?:null
        metaVo.docStage =proprietaryNode.'leos:docStage'.text()?:null
        metaVo.docType  =proprietaryNode.'leos:docType'.text()?:null
        metaVo.docPurpose   =proprietaryNode.'leos:docPurpose'.text()?:null
        //metaVo.docPurposeId =proprietaryNode.'leos:docPurpose'.@id ?:null

        //populate values from language node
        GPathResult languageNode = metaNode.identification.FRBRExpression.FRBRlanguage
        metaVo.language   =languageNode.@language.text() ?: null

        return metaVo
    }



    def String createMetaSection (MetaDataVO metaVo){
        def builder = new StreamingMarkupBuilder()
        builder.setUseDoubleQuotes(true)

        def meta = builder.bind {
            meta {
            }
        }
        return meta.toString()
    }

    def void updateLanguageSection(GPathResult metaNode, MetaDataVO metaVo){
        GPathResult FRBRlanguageNode = metaNode.'**'.find{it.name()=="FRBRlanguage"}
        
        if(FRBRlanguageNode != null){
            FRBRlanguageNode.@language=metaVo.getLanguage()
        }
        else{
            def builder = new StreamingMarkupBuilder()
            builder.setUseDoubleQuotes(true)
            def languageNode = builder.bind{
                identification{
                    FRBRExpression{
                        FRBRlanguage(id:DEFAULT_LANG_ID, language:metaVo.getLanguage())
                        }
                    }
                }
            metaNode.appendNode(new XmlSlurper(false, false).parseText(languageNode.toString()))
           }
      }

    def void updateProprietarySection(GPathResult metaNode, MetaDataVO metaVo){
        GPathResult docPurpose = metaNode.'**'.find{it.name()=="leos:docPurpose"}

        if(docPurpose!=null){
            docPurpose.replaceBody metaVo.getDocPurpose()
        }
        else {
            StreamingMarkupBuilder builder = new StreamingMarkupBuilder()
            builder.setUseDoubleQuotes(true)
            def proprietary= builder.bind{
                proprietary(source:"~${DEFAULT_SOURCE}"){
                    "leos:template" id:DEFAULT_DOC_TEMPLATE_ID, metaVo.getTemplate()
                    "leos:docStage" id:DEFAULT_DOC_STAGE_ID, metaVo.getDocStage()
                    "leos:docType" id:DEFAULT_DOC_TYPE_ID, metaVo.getDocType()
                    "leos:docPurpose" id:DEFAULT_DOC_PURPOSE_ID, metaVo.getDocPurpose()
                    "leos:internalRef" null
                }
            }
            metaNode.appendNode(new XmlSlurper(false, false).parseText(proprietary.toString()));
        }

    }
}



