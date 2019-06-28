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
package eu.europa.ec.leos.services.content.processor;

import eu.europa.ec.leos.services.support.xml.VtdXmlContentProcessorForProposal;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.test.support.LeosTest;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.InjectMocks;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AttachmentProcessorTest extends LeosTest {

    private XmlContentProcessor xmlContentProcessor = new VtdXmlContentProcessorForProposal();

    @InjectMocks
    private AttachmentProcessor attachmentProcessor = new AttachmentProcessorImpl(xmlContentProcessor);

    @Test
    public void test_addAttachment_NoAttachmentsTag() throws Exception {
        // setup
        String xml = "<bill>" +
                "<meta xml:id=\"ElementId\"></meta>" +
                "</bill>";
        String href = "annex_href";
        String showAs = "";
        String expectedXml = "<bill>" +
                "<meta xml:id=\"ElementId\"></meta>" +
                "<attachments>" +
                "<attachment xml:id=.+?><documentRef xml:id=.+? href=\"" + href + "\" showAs=\"" + showAs + "\"/></attachment>" +
                "</attachments>" +
                "</bill>";

        // make the actual call
        byte[] result = attachmentProcessor.addAttachmentInBill(xml.getBytes(UTF_8), href, showAs);

        // verify
        assertTrue(new String(result, UTF_8).matches(expectedXml));
    }

    @Test
    public void test_addAttachment_WithAttachmentsTag() throws Exception {
        // setup
        String xml = "<bill>" +
                "<meta xml:id=\"ElementId\"></meta>" +
                "<attachments xml:id=\"attachs\">" +
                "<attachment xml:id=\"atta\"><documentRef xml:id=\"docref\" href=\"someHref\" showAs=\"\"/></attachment>" +
                "</attachments>" +
                "</bill>";
        String href = "annex_href";
        String showAs = "";
        String expectedXml = "<bill>" +
                "<meta xml:id=\"ElementId\"></meta>" +
                "<attachments xml:id=\"attachs\">" +
                "<attachment xml:id=\"atta\"><documentRef xml:id=\"docref\" href=\"someHref\" showAs=\"\"/></attachment>" +
                "<attachment xml:id=.+?><documentRef xml:id=.+? href=\"" + href + "\" showAs=\"" + showAs + "\"/></attachment>" +
                "</attachments>" +
                "</bill>";

        // make the actual call
        byte[] result = attachmentProcessor.addAttachmentInBill(xml.getBytes(UTF_8), href, showAs);

        // verify
        assertTrue(new String(result, UTF_8).matches(expectedXml));
    }

    @Test
    public void test_removeAttachmentWithSingleAttachment() throws Exception {
        // setup
        String href = "annex_href";
        String showAs = "";

        String xml = "<bill>" +
                "<meta xml:id=\"ElementId\"></meta>" +
                "<attachments xml:id=\"id1\">" +
                "<attachment xml:id=\"id2\"><documentRef xml:id=\"id3\" href=\"" + href + "\" showAs=\"" + showAs + "\"/></attachment>" +
                "</attachments>" +
                "</bill>";

        String expectedXml = "<bill>" +
                "<meta xml:id=\"ElementId\"></meta>" +
                "</bill>";

        // make the actual call
        byte[] result = attachmentProcessor.removeAttachmentFromBill(xml.getBytes(UTF_8), href);

        // verify
        assertThat(new String(result, UTF_8), Matchers.equalTo(expectedXml));
    }

    @Test
    public void test_removeAttachmentWithMultipleAttachment() throws Exception {
        // setup
        String href = "annex_href";
        String showAs = "";
        String href2 = "annex_href2";
        String showAs2 = "";

        String xml = "<bill>" +
                "<meta xml:id=\"ElementId\"></meta>" +
                "<attachments xml:id=\"id1\">" +
                "<attachment xml:id=\"id2\"><documentRef xml:id=\"id3\" href=\"" + href + "\" showAs=\"" + showAs + "\"/></attachment>" +
                "<attachment xml:id=\"id2\"><documentRef xml:id=\"id3\" href=\"" + href2 + "\" showAs=\"" + showAs2 + "\"/></attachment>" +
                "</attachments>" +
                "</bill>";

        String expectedXml = "<bill>" +
                "<meta xml:id=\"ElementId\"></meta>" +
                "<attachments xml:id=\"id1\">" +
                "<attachment xml:id=\"id2\"><documentRef xml:id=\"id3\" href=\"" + href2 + "\" showAs=\"" + showAs2 + "\"/></attachment>" +
                "</attachments>" +
                "</bill>";;

        // make the actual call
        byte[] result = attachmentProcessor.removeAttachmentFromBill(xml.getBytes(UTF_8), href);

        // verify
        assertThat(new String(result, UTF_8), Matchers.equalTo(expectedXml));
    }
}