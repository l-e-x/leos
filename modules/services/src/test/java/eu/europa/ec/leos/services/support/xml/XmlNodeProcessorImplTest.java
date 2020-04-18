/*
 * Copyright 2019 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.services.support.xml;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import eu.europa.ec.leos.services.util.TestUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import eu.europa.ec.leos.test.support.LeosTest;
import org.mockito.InjectMocks;

public class XmlNodeProcessorImplTest extends LeosTest {

    private static final Map<String, XmlNodeConfig> CONFIGURATIONS = setupConfig();

    @InjectMocks
    private XmlNodeProcessorImpl metaDataProcessor = new XmlNodeProcessorImpl();

    @Test
    public void getValues_values_present_test() {
        // setup
        byte[] xmlContent = TestUtils.getFileContent("/bill-test.xml");
        String[] keys = {"docStage", "template", "language"};

        // actual call
        Map<String, String> result = metaDataProcessor.getValuesFromXml(xmlContent, keys ,CONFIGURATIONS);

        // verify
        assertThat(result.size(), is(3));
        assertThat(result.get("template"), is("SJ-023"));
        assertThat(result.get("language"), is("EN"));
    }

    @Test
    public void getValues_values_not_present_test() {
        // setup
        byte[] xmlContent = TestUtils.getFileContent("/memorandum-test.xml");
        String[] keys = {"docStage", "template"};

        // actual call
        Map<String, String> result = metaDataProcessor.getValuesFromXml(xmlContent, keys, CONFIGURATIONS);

        // verify
        assertThat(result.size(), is(0));
    }
    
    @Test
    public void setValues_tags_present_test() throws Exception {// meta/identification/FRBRExpression/FRBRlanguage/@language
        // setup
        byte[] xmlContent = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">"
                + "<bill><meta><identification><FRBRExpression><FRBRlanguage xml:id=\"lang\" language=\"FR\"></FRBRlanguage></FRBRExpression></identification>"
                + "<proprietary>"
                + "<leos:docPurpose xml:id=\"xyyyzzz\">OLDVALUE</leos:docPurpose>"
                + "<leos:docStage xml:id=\"xyyyzzz\">OLDStage</leos:docStage>"
                + "</proprietary>"
                + "</meta>"
                + "<body><article xml:id=\"art486\"></article></body></bill>" +
                "</akomaNtoso>").getBytes(UTF_8);

        String expectedContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">"
                + "<bill><meta><identification><FRBRExpression><FRBRlanguage xml:id=\"lang\" language=\"EN\"></FRBRlanguage></FRBRExpression></identification>"
                + "<proprietary>"
                + "<leos:docPurpose xml:id=\"xyyyzzz\">NEWVALUE &amp;</leos:docPurpose>"
                + "<leos:docStage xml:id=\"xyyyzzz\">NEW Stage</leos:docStage>"
                + "</proprietary>"
                + "</meta>"
                + "<body><article xml:id=\"art486\"></article></body></bill>" +
                "</akomaNtoso>";

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("docPurpose", "NEWVALUE &");
        keyValue.put("docStage", "NEW Stage");
        keyValue.put("language", "EN");

        // actual call
        byte[] result = metaDataProcessor.setValuesInXml(xmlContent, keyValue,CONFIGURATIONS);

        // verify
        assertThat(new String(result, UTF_8), is(expectedContent));
    }

    @Test
    public void setValues_tags_not_present_test() throws Exception {// meta/identification/FRBRExpression/FRBRlanguage/@language
        // setup
        byte[] xmlContent = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\"><bill>"
                + "<meta>"
                + "</meta>"
                + "<body><article xml:id=\"art486\"></article></body></bill>" +
                "</akomaNtoso>").getBytes(UTF_8);

        String expectedContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\"><bill>"
                + "<meta><identification><FRBRExpression><FRBRlanguage language=\"EN\"></FRBRlanguage></FRBRExpression></identification>"
                + "<proprietary>"
                + "<leos:docPurpose xml:id=\"xyyyzzz\">NEWVALUE</leos:docPurpose>"
                + "<leos:docStage xml:id=\"xyyyzzz\">NEW Stage</leos:docStage>"
                + "</proprietary>"
                + "</meta>"
                + "<body><article xml:id=\"art486\"></article></body></bill>" +
                "</akomaNtoso>";

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("language", "EN");
        keyValue.put("docStage", "NEW Stage");
        keyValue.put("docPurpose", "NEWVALUE");

        // actual call
        byte[] result = metaDataProcessor.setValuesInXml(xmlContent, keyValue, CONFIGURATIONS);

        // verify
        Map<String, String> resultMap = metaDataProcessor.getValuesFromXml(result, new String[]{"language", "docStage", "docPurpose"}, CONFIGURATIONS);//Using a shortcut to validate test
        assertThat(resultMap.get("language"), is("EN"));
        assertThat(resultMap.get("docStage"), is("NEW Stage"));
    }

    @Test
    public void setValues_tags_not_present_in_big_xml_test() throws Exception {// meta/identification/FRBRExpression/FRBRlanguage/@language
        // setup
        byte[] xmlContent = TestUtils.getFileContent("/memorandum-test.xml");

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("language", "EN");
        keyValue.put("docStage", "NEW Stage");
        keyValue.put("docPurpose", "NEWVALUE");

        // actual call
        byte[] result = metaDataProcessor.setValuesInXml(xmlContent, keyValue, CONFIGURATIONS);

        // verify
        Map<String, String> resultMap = metaDataProcessor.getValuesFromXml(result, new String[]{"language", "docStage", "docPurpose"}, CONFIGURATIONS);
        // assertThat(new String(result, UTF_8).length(), is(expectedContent.length()));
        assertThat(resultMap.get("language"), is("EN"));
        assertThat(resultMap.get("docStage"), is("NEW Stage"));
    }

    @Test
    public void setValues_AttributeNodeSelectorAndCreateIsTrue() throws Exception {
        // setup
        byte[] xmlContent = ("<akomaNtoso>"
                            +"<meta xml:id=\"test\"></meta>"
                            +"<coverPage xml:id=\"testCover\"></coverPage>"
                            +"</akomaNtoso>").getBytes(UTF_8);

        byte[] expectedtResult = ("<akomaNtoso>"
                +"<meta xml:id=\"test\"></meta>"
                +"<coverPage xml:id=\"testCover\"><container name=\"annexNumber\"><p>Annex 1</p></container></coverPage>"
                +"</akomaNtoso>").getBytes(UTF_8);

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("annexNumberCover", "Annex 1");

        Map<String, XmlNodeConfig> configuration = new HashMap<>();
        configuration.put("annexNumberCover", new XmlNodeConfig("//coverPage/container[@name='annexNumber']/p",true,
                Collections.emptyList()));

        // actual call
        byte[] result = metaDataProcessor.setValuesInXml(xmlContent, keyValue, configuration);

        // verify
        assertThat(result, is(expectedtResult));
    }

    @Test
    public void setValues_AttributeNodeSelectorAndCreateIsFalse() throws Exception {
        // setup
        byte[] xmlContent = ("<akomaNtoso>"
                +"<meta xml:id=\"test\"></meta>"
                +"<coverPage xml:id=\"testCover\"></coverPage>"
                +"</akomaNtoso>").getBytes(UTF_8);

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("annexNumberCover", "Annex 1");

        Map<String, XmlNodeConfig> configuration = new HashMap<>();
        configuration.put("annexNumberCover", new XmlNodeConfig("//coverPage/container[@name='annexNumber']/p",false,
                Collections.emptyList()));

        // actual call
        byte[] result = metaDataProcessor.setValuesInXml(xmlContent, keyValue, configuration);

        // verify
        assertThat(result, is(xmlContent));
    }

    @Test
    public void getValues_AttributeNodeSelector() throws Exception {
        // setup
        byte[] xmlContent = ("<akomaNtoso>"
                +"<meta xml:id=\"test\"></meta>"
                +"<coverPage xml:id=\"testCover\">"
                +"<container name=\"x\"><p>Dummuy</p></container>"
                +"<container name=\"annexNumber\"><p>ValidValue &amp;</p></container>"
                +"<container name=\"test\"><p>InValidValue</p></container>"
                +"</coverPage>"
                +"<container name=\"annexNumber\"><p>InValidValue</p></container>"
                +"</akomaNtoso>").getBytes(UTF_8);

        String[] keys = {"annexNumberCover"};

        Map<String, XmlNodeConfig> configuration = new HashMap<>();
        configuration.put("annexNumberCover", new XmlNodeConfig("//coverPage/container[@name='annexNumber']/p",false,
                Collections.emptyList()));

        // actual call
        Map<String, String> result = metaDataProcessor.getValuesFromXml(xmlContent, keys, configuration);

        // verify
        assertThat(result.get(keys[0]), is("ValidValue &"));
    }

    private static Map<String, XmlNodeConfig> setupConfig() {
        Map<String, XmlNodeConfig> configuration = new HashMap<>();

        configuration.put("docPurpose", new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docPurpose",true,
                Arrays.asList(new XmlNodeConfig.Attribute("xml:id", "proprietary__docpurpose", "leos:docPurpose"),
                        new XmlNodeConfig.Attribute("source", "~leos", "proprietary"))));
        configuration.put("docStage", new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docStage",true,
                Arrays.asList(new XmlNodeConfig.Attribute("xml:id", "proprietary__docstage", "leos:docStage"))));
        configuration.put("docType", new XmlNodeConfig("/akomaNtoso//meta/proprietary/leos:docType",true,
                Arrays.asList(new XmlNodeConfig.Attribute("xml:id", "proprietary__doctype", "leos:docType"))));
        configuration.put("template", new XmlNodeConfig("//meta/proprietary/leos:template",true,
                Arrays.asList(new XmlNodeConfig.Attribute("xml:id", "proprietary__template","leos:template"))));
        configuration.put("language", new XmlNodeConfig("//meta/identification/FRBRExpression/FRBRlanguage/@language",true,
                Arrays.asList(new XmlNodeConfig.Attribute("xml:id", "frbrexpression__frbrlanguage_1","FRBRlanguage"))));
        configuration.put("annexNumberCover", new XmlNodeConfig("//coverPage/container[@name='annexNumber']/p",true,
                Arrays.asList(new XmlNodeConfig.Attribute("xml:id", "cover_annexNumber", "container"))));
        return configuration;
    }
}
