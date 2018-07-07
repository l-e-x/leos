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
package eu.europa.ec.leos.support;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.Ignore;

import eu.europa.ec.leos.support.xml.XmlMetaDataProcessorImpl;
import eu.europa.ec.leos.vo.MetaDataVO;

public class XmlMetaDataProcessorImplTest {

    private XmlMetaDataProcessorImpl xmlMetaDataProcessorImpl = new XmlMetaDataProcessorImpl();
    

    @Ignore
    @Test
    public void testCreateXmlForMeta()  {

        //setup
        MetaDataVO metaDataVO = new MetaDataVO("SJ-016", "EN", "Proposal", "for a Council Regulation", "on [...]", "");
        String xml = "<meta>" +
                "<identification><FRBRExpression><FRBRlanguage id=\"frbrexpression__frbrlanguage_1\" language=\"EN\">" +
                "</FRBRlanguage></FRBRExpression></identification>" +
                "<proprietary source=\"~leos\">" +
                "<leos:template id=\"proprietary__template\">SJ-016</leos:template>" +
                "<leos:docStage id=\"proprietary__docstage\">Proposal</leos:docStage>" +
                "<leos:docType id=\"proprietary__doctype\">for a Council Regulation</leos:docType>" +
                "<leos:docPurpose id=\"proprietary__docpurpose\">on [...]</leos:docPurpose>" +
                "<leos:internalRef></leos:internalRef>" +
                "</proprietary>"+
                "</meta>";

        //actual test
        String result = (String) xmlMetaDataProcessorImpl.toXML(metaDataVO);

        //verify
        assertThat(result, is(xml));
    }

    @Ignore
    @Test
    public void testCreateXmlForMeta_fromExistingXML()  {

        //setup
        String xml = "<meta>" +
                "<identification><FRBRExpression><FRBRlanguage language=\"\" id=\"frbrexpression__frbrlanguage_1\">" +
                "</FRBRlanguage></FRBRExpression></identification>" +
                "<xyz>abc</xyz>" +
                "<proprietary source=\"~leos\">" +
                "<leos:template id=\"proprietary__template\">SJ-016</leos:template>" +
                "<leos:docStage id=\"proprietary__docstage\">Proposal</leos:docStage>" +
                "<leos:docType id=\"proprietary__doctype\">for a Council Regulation</leos:docType>" +
                "<leos:docPurpose id=\"proprietary__docpurpose\">on</leos:docPurpose>" +
                "<leos:internalRef></leos:internalRef>" +
                "</proprietary>" +
                "</meta>";
        String xmlResult = "<meta>" +
                "<identification><FRBRExpression><FRBRlanguage id=\"frbrexpression__frbrlanguage_1\" language=\"EN\">" +
                "</FRBRlanguage></FRBRExpression></identification>" +
                "<xyz>abc</xyz>" +
                "<proprietary source=\"~leos\">" +
                "<leos:template id=\"proprietary__template\">SJ-016</leos:template>" +
                "<leos:docStage id=\"proprietary__docstage\">Proposal</leos:docStage>" +
                "<leos:docType id=\"proprietary__doctype\">for a Council Regulation</leos:docType>" +
                "<leos:docPurpose id=\"proprietary__docpurpose\">on [...]</leos:docPurpose>" +
                "<leos:internalRef></leos:internalRef>" +
                "</proprietary>" +
                "</meta>";
        MetaDataVO metaDataVO = new MetaDataVO("SJ-016","EN", "Proposal", "for a Council Regulation", "on [...]",null);
        
        //do the test
        String result = (String) xmlMetaDataProcessorImpl.toXML(metaDataVO, xml);

        //verify
        assertThat(result, is(xmlResult));
    }
    
    @Ignore
    @Test
    public void testCreateXmlForMeta_fromExistingXMLwithTwoProprietary()  {
        
        //setup
        String xml = "<meta>" +
                "<identification><FRBRExpression>" +
                "<FRBRlanguage id=\"frbrexpression__frbrlanguage_1\" language=\"en\">" +
                "</FRBRlanguage></FRBRExpression></identification>"+
                
                "<proprietary source=\"~notLeos\">" +
                "<template id=\"proprietary__template\">SJ-016</template>" +
                "<language>EN</language>" +
                "<docStage3 id=\"proprietary__docstage3\">unrelated</docStage3>" +
                "<docStage id=\"proprietary__docstage\">Proposal</docStage>" +
                "<docType id=\"proprietary__doctype\">for a Council Regulation</docType>" +
                "<docPurpose id=\"proprietary__docpurpose\">on [...]</docPurpose>" +
                "<internalRef></internalRef>"+
                "</proprietary>"+
                
                "<proprietary source=\"~leos\" >" +
                "<leos:template id=\"proprietary__template\">SJ-015</leos:template>" +
                "<leos:language>en</leos:language>" +
                "<leos:docStage3 id=\"proprietary__docstage3\">unrelated</leos:docStage3>" +
                "<leos:docStage id=\"proprietary__docstage\">Proposal</leos:docStage>" +
                "<leos:docType id=\"proprietary__doctype\">for a Council Regulation</leos:docType>" +
                "<leos:docPurpose id=\"proprietary__docpurpose\">on [...]</leos:docPurpose>" +
                "<leos:internalRef></leos:internalRef>"+
                "</proprietary>"+
                "</meta>";
        String xmlResult = "<meta>" +
                "<identification><FRBRExpression>" +
                "<FRBRlanguage id=\"frbrexpression__frbrlanguage_1\" language=\"fr\"></FRBRlanguage>" +
                "</FRBRExpression></identification>"+
                "<proprietary source=\"~notLeos\">" +
                "<template id=\"proprietary__template\">SJ-016</template>" +
                "<language>EN</language>" +
                "<docStage3 id=\"proprietary__docstage3\">unrelated</docStage3>" +
                "<docStage id=\"proprietary__docstage\">Proposal</docStage>" +
                "<docType id=\"proprietary__doctype\">for a Council Regulation</docType>" +
                "<docPurpose id=\"proprietary__docpurpose\">on [...]</docPurpose>" +
                "<internalRef></internalRef>"+
                "</proprietary>"+
                "<proprietary source=\"~leos\">" +
                "<leos:template id=\"proprietary__template\">SJ-015</leos:template>" +
                "<leos:language>en</leos:language>" +
                "<leos:docStage3 id=\"proprietary__docstage3\">unrelated</leos:docStage3>" +
                "<leos:docStage id=\"proprietary__docstage\">Proposal</leos:docStage>" +
                "<leos:docType id=\"proprietary__doctype\">for a Council Regulation</leos:docType>" +
                "<leos:docPurpose id=\"proprietary__docpurpose\">on matters important</leos:docPurpose>" +
                "<leos:internalRef></leos:internalRef>"+
                "</proprietary>"+
                "</meta>";
        MetaDataVO metaDataVO = new MetaDataVO("SJ-023","fr", "Proposal", "for a Council Regulation", "on matters important", null);//only language and doc purpose shd be updated

        //actual test
        String result = (String) xmlMetaDataProcessorImpl.toXML(metaDataVO, xml);

        //verify
        assertThat(result, is(xmlResult));
    }
    @Test
    public void testCreateMetaDataFromXml()  {

        //setup
        String xml =  "<meta>" +
                "<identification><FRBRExpression>" +
                "<FRBRlanguage id=\"frbrexpression__frbrlanguage_1\" language=\"en\"/>" +
                "</FRBRExpression></identification>"+
                "<proprietary source=\"~leos\">" +
                "<leos:template id=\"proprietary__template\">SJ-016</leos:template>" +
                "<leos:docStage id=\"proprietary__docstage\">Proposal</leos:docStage>" +
                "<leos:docType id=\"proprietary__doctype\">for a Council Regulation</leos:docType>" +
                "<leos:docPurpose id=\"proprietary__docpurpose\">on [...]</leos:docPurpose>" +
                "<leos:internalRef></leos:internalRef>"+
                "</proprietary>"+
                "</meta>";
        MetaDataVO metaDataVO = new MetaDataVO("SJ-016", "en", "Proposal", "for a Council Regulation", "on [...]", "");

        // DO THE ACTUAL TEST
        MetaDataVO result = (MetaDataVO) xmlMetaDataProcessorImpl.fromXML(xml);
   
        //verify
        assertThat(result, is(metaDataVO));
    }

    @Test
    public void testCreateMetaDataFromXml_when_unknownElement_should_ignoreTheUnknown()  {

        //setup
        String xml =  "<meta>" +
                "<identification><FRBRExpression>" +
                "<FRBRlanguage id=\"frbrexpression__frbrlanguage_1\" language=\"en\"/>" +
                "</FRBRExpression></identification>"+
                "<proprietary source=\"~leos\">" +
                "<leos:template id=\"proprietary__template\">SJ-016</leos:template>" +
                "<leos:language5>EN</leos:language5>" +
                "<leos:docStage3 id=\"proprietary__docstage3\">unrelated</leos:docStage3>" +
                "<leos:docStage id=\"proprietary__docstage\">Proposal</leos:docStage>" +
                "<leos:docType id=\"proprietary__doctype\">for a Council Regulation</leos:docType>" +
                "<leos:docPurpose id=\"proprietary__docpurpose\">on [...]</leos:docPurpose>" +
                "<leos:internalRef></leos:internalRef>"+
                "</proprietary>"+
                "</meta>";
        MetaDataVO metaDataVO = new MetaDataVO("SJ-016", "en", "Proposal", "for a Council Regulation", "on [...]", "");

        // DO THE ACTUAL TEST
        MetaDataVO result = (MetaDataVO) xmlMetaDataProcessorImpl.fromXML(xml);
        
        //verify
        assertThat(result, is(metaDataVO));
    }
    @Test
    public void testCreateMetaDataFromXml_when_noElement_should_ignoreTheUnknown()  {

        //setup
        MetaDataVO metaDataVO = new MetaDataVO();
        String xml =  "<meta>" +
                "<identification><FRBRExpression1>" +
                "<FRBRlanguage id=\"frbrexpression__frbrlanguage_1\" language=\"en\"/>" +
                "</FRBRExpression1></identification>"+
                "<proprietary source=\"~notLeos\">" +
                "<leos:template id=\"proprietary__template\">SJ-016</leos:template>" +
                "<leos:language>EN</leos:language>" +
                "<leos:docStage3 id=\"proprietary__docstage3\">unrelated</leos:docStage3>" +
                "<leos:docStage id=\"proprietary__docstage\">Proposal</leos:docStage>" +
                "<leos:docType id=\"proprietary__doctype\">for a Council Regulation</leos:docType>" +
                "<leos:docPurpose id=\"proprietary__docpurpose\">on [...]</leos:docPurpose>" +
                "<leos:internalRef/>"+
                "</proprietary>"+
                "</meta>";
        // DO THE ACTUAL TEST
        MetaDataVO result = (MetaDataVO) xmlMetaDataProcessorImpl.fromXML(xml);
        
        //verify
        assertThat(result, is(metaDataVO));
    }
    
    @Test
    public void testCreateMetaDataFromXml_when_twoProprietory()  {

        //setup
        String xml =  "<meta>" +
                "<identification><FRBRExpression>" +
                "<FRBRlanguage id=\"frbrexpression__frbrlanguage_1\" language=\"en\"/>" +
                "</FRBRExpression></identification>"+
                "<proprietary source=\"~notLeos\">" +
                "<leos:template id=\"proprietary__template\">SJ-025</leos:template>" +
                "<leos:language>EN</leos:language>" +
                "<leos:docStage3 id=\"proprietary__docstage3\">unrelated</leos:docStage3>" +
                "<leos:docStage id=\"proprietary__docstage\">Propoasdsal</leos:docStage>" +
                "<leos:docType id=\"proprietary__doctype\">for a Counciasdl Regulation</leos:docType>" +
                "<leos:docPurpose id=\"proprietary__docpurpose\">on [...asd]</leos:docPurpose>" +
                "<leos:internalRef/>"+
                "</proprietary>"+
                "<proprietary source=\"~leos\">" +
                "<leos:template id=\"proprietary__template\">SJ-016</leos:template>" +
                "<leos:language>en</leos:language>" +
                "<leos:docStage3 id=\"proprietary__docstage3\">unrelated</leos:docStage3>" +
                "<leos:docStage id=\"proprietary__docstage\">Proposal</leos:docStage>" +
                "<leos:docType id=\"proprietary__doctype\">for a Council Regulation</leos:docType>" +
                "<leos:docPurpose id=\"proprietary__docpurpose\">on [...]</leos:docPurpose>" +
                "<leos:internalRef/>"+
                "</proprietary>"+
                "</meta>";
        MetaDataVO metaDataVO = new MetaDataVO("SJ-016", "en", "Proposal", "for a Council Regulation", "on [...]", null);

        // DO THE ACTUAL TEST
        MetaDataVO result = (MetaDataVO) xmlMetaDataProcessorImpl.fromXML(xml);

        //verify
        assertThat(result, is(metaDataVO));
    }
}
