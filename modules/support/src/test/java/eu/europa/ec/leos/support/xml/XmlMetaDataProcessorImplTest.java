/**
 * Copyright 2015 European Commission
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
package eu.europa.ec.leos.support.xml;

import eu.europa.ec.leos.vo.MetaDataVO;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class XmlMetaDataProcessorImplTest {

    private XmlMetaDataProcessorImpl xmlMetaDataProcessorImpl = new XmlMetaDataProcessorImpl();

    @Test
    public void testCreateXmlForProprietary() throws XMLStreamException {
        MetaDataVO metaDataVO = new MetaDataVO("SJ-016", "EN", "Proposal", "for a Council Regulation", "on [...]", "");

        String result = xmlMetaDataProcessorImpl.createXmlForProprietary(metaDataVO);

        String xml = "<proprietary source=\"EC DIGIT LEOS Prototype\">" +
                "<leos:template>SJ-016</leos:template>" +
                "<leos:language>EN</leos:language>" +
                "<leos:docStage>Proposal</leos:docStage>"+
                "<leos:docType>for a Council Regulation</leos:docType>"+ 
                "<leos:docPurpose>on [...]</leos:docPurpose>"+
                "<leos:internalRef></leos:internalRef>"+
                "</proprietary>";

        assertThat(result, is(xml));
    }

    @Test
    public void testCreateMetaDataFromXml() throws XMLStreamException {

        String xml = "<proprietary source=\"EC DIGIT LEOS Prototype\">" +
                "<leos:template>SJ-016</leos:template>" +
                "<leos:language>EN</leos:language>" +
                "<leos:docStage>Proposal</leos:docStage>"+
                "<leos:docType>for a Council Regulation</leos:docType>"+ 
                "<leos:docPurpose>on [...]</leos:docPurpose>"+
                "<leos:internalRef></leos:internalRef>"+
                "</proprietary>";

        // DO THE ACTUAL TEST
        MetaDataVO result = xmlMetaDataProcessorImpl.createMetaDataVOFromXml(xml);

        MetaDataVO metaDataVO = new MetaDataVO("SJ-016", "EN", "Proposal", "for a Council Regulation", "on [...]", "");
        assertThat(result.getTemplate(), is(metaDataVO.getTemplate()));
    }

    @Test
    public void testCreateMetaDataFromXml_when_unknownElement_should_ignoreTheUnknown() throws XMLStreamException {

        String xml = "<proprietary source=\"EC DIGIT LEOS Prototype\">" +
                "<leos:template>SJ-016</leos:template>" +
                "<leos:language5>EN</leos:language5>" +
                "<leos:docStage>Proposal</leos:docStage>"+
                "<leos:docType>for a Council Regulation</leos:docType>"+ 
                "<leos:docPurpose>on [...]</leos:docPurpose>"+
                "<leos:internalRef/>" +
                "</proprietary>";

        // DO THE ACTUAL TEST
        MetaDataVO result = xmlMetaDataProcessorImpl.createMetaDataVOFromXml(xml);

        MetaDataVO metaDataVO = new MetaDataVO("SJ-016", null, "Proposal", "for a Council Regulation", "on [...]", "");
        assertThat(result.getTemplate(), is(metaDataVO.getTemplate()));
    }
}
