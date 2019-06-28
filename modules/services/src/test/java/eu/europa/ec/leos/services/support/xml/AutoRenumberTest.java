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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import eu.europa.ec.leos.test.support.LeosTest;

public class AutoRenumberTest extends LeosTest {

    @InjectMocks
    private NumberProcessor proposalNumberingProcessor = new ProposalNumberingProcessor();

    @InjectMocks
    private NumberProcessor mandateNumberingProcessor = new MandateNumberingProcessor();

    @Spy
    private ElementNumberingHelper elementNumberingHelper;
    
    @Test
    public void test_autoRenumberRecitals() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">" +
                "<recitals xml:id=\"recs\" leos:editable=\"false\">" +
                "<recital xml:id=\"rec_1\" leos:editable=\"true\"><num xml:id=\"rec_1__num\">(23)</num>" +
                "  <p xml:id=\"rec_1__p\">Recital...</p>" +
                " </recital>" +
                "<recital xml:id=\"rec_2\" leos:editable=\"true\"><num xml:id=\"rec_2__num\">(24)</num>" +
                " <p xml:id=\"rec_2__p\">Recital...</p>" +
                "</recital>" +
                "</recitals>" +
                "</akomaNtoso>";


        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">" +
                "<recitals xml:id=\"recs\" leos:editable=\"false\">" +
                "<recital xml:id=\"rec_1\" leos:editable=\"true\"><num>(1)</num>" +
                "  <p xml:id=\"rec_1__p\">Recital...</p>" +
                " </recital>" +
                "<recital xml:id=\"rec_2\" leos:editable=\"true\"><num>(2)</num>" +
                " <p xml:id=\"rec_2__p\">Recital...</p>" +
                "</recital>" +
                "</recitals>" +
                "</akomaNtoso>";

        byte[] result = proposalNumberingProcessor.renumberRecitals(xml.getBytes());

        assertThat(new String(result).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }


    @Test
    public void test_manualRenumberRecitals() {
        String xml ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">" +
                "<recitals xml:id=\"recs\" leos:editable=\"false\">" +
                "<recital xml:id=\"rec_1\" leos:editable=\"true\" leos:origin=\"ec\"><num xml:id=\"rec_1__num\"  leos:origin=\"ec\">(1)</num>" +
                "  <p xml:id=\"rec_1__p\">Recital...1</p>" +
                " </recital>" +
                "<recital xml:id=\"rec_2\" leos:editable=\"true\" leos:origin=\"cn\"><num xml:id=\"rec_2__num\">(#)</num>" +
                " <p xml:id=\"rec_2__p\">Recital...1a</p>" +
                " </recital>" +
                "<recital xml:id=\"rec_2\" leos:editable=\"true\" leos:origin=\"ec\"><num xml:id=\"rec_2__num\"  leos:origin=\"ec\">(2)</num>" +
                " <p xml:id=\"rec_2__p\">Recital...2</p>" +
                "</recital>" +
                "</recitals>" +
                "</akomaNtoso>";


        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">" +
                "<recitals xml:id=\"recs\" leos:editable=\"false\">" +
                "<recital xml:id=\"rec_1\" leos:editable=\"true\" leos:origin=\"ec\"><num>(1)</num>" +
                "  <p xml:id=\"rec_1__p\">Recital...1</p>" +
                " </recital>" +
                "<recital xml:id=\"rec_2\" leos:editable=\"true\" leos:origin=\"cn\"><num>(1a)</num>" +
                " <p xml:id=\"rec_2__p\">Recital...1a</p>" +
                " </recital>" +
                "<recital xml:id=\"rec_2\" leos:editable=\"true\" leos:origin=\"ec\"><num>(2)</num>" +
                " <p xml:id=\"rec_2__p\">Recital...2</p>" +
                "</recital>" +
                "</recitals>" +
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberRecitals(xml.getBytes());

        assertThat(new String(result).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

}
