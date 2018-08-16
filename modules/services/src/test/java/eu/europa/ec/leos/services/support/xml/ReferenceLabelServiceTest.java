/*
 * Copyright 2018 European Commission
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

import eu.europa.ec.leos.services.support.xml.ref.LabelArticlesOrRecitalsOnly;
import eu.europa.ec.leos.services.support.xml.ref.LabelHigherOrderElementsOnly;
import eu.europa.ec.leos.services.support.xml.ref.LabelNumberedElementOnly;
import eu.europa.ec.leos.services.support.xml.ref.LabelUnnumberedElementOnly;
import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.domain.common.ErrorCode;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ReferenceLabelServiceTest extends LeosTest {

    private byte[] docContent;

    @Before
    public void setup() {
        super.setup();
        String doc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"
                + "    <bill name=\"regulation\" contains=\"originalVersion\">"
                + "        <coverPage GUID=\"coverpage\">"
                + "            <container name=\"institutionOwnerLogo\" GUID=\"coverpage__container_1\">"
                + "                <p>bla</p>"
                + "            </container>"
                + "            <longTitle GUID=\"coverpage__longtitle\">"
                + "                <p GUID=\"coverpage__longtitle__p\">"
                + "                    <docStage GUID=\"coverpage__longtitle__p__docstage\">Proposal for a</docStage>"
                + "                    <docType GUID=\"coverpage__longtitle__p__doctype\">REGULATION OF THE EUROPEAN PARLIAMENT AND OF THE COUNCIL</docType>"
                + "                    <docPurpose GUID=\"coverpage__longtitle__p__docpurpose\">on Test</docPurpose>"
                + "                </p>"
                + "            </longTitle>"
                + "            <container name=\"docLabel\" GUID=\"coverpage__container_2\">"
                + "                <p GUID=\"akn_mPCLZl\" >LEGAL TEXT</p>"
                + "            </container>"
                + "            <container name=\"language\" GUID=\"coverpage__container_3\" refersTo=\"#frbrLanguage\">"
                + "                <p GUID=\"coverpage__container_3__p\">EN</p>"
                + "            </container>"
                + "        </coverPage>"
                + "        <preface GUID=\"preface\">"
                + "            <longTitle GUID=\"preface__longtitle\">"
                + "                <p GUID=\"preface__longtitle__p\">"
                + "                    <docStage GUID=\"preface__longtitle__p__docstage\">Proposal for a</docStage>"
                + "                    <docType GUID=\"preface__longtitle__p__doctype\">REGULATION OF THE EUROPEAN PARLIAMENT AND OF THE COUNCIL</docType>"
                + "                    <docPurpose GUID=\"preface__longtitle__p__docpurpose\">on Test</docPurpose>"
                + "                </p>"
                + "            </longTitle>"
                + "        </preface>"
                + "        <preamble GUID=\"preamble\">"
                + "            <formula GUID=\"preamble__formula_1\" name=\"authorities\">"
                + "                <p GUID=\"preamble__formula_1__p\">THE EUROPEAN PARLIAMENT AND THE COUNCIL OF THE EUROPEAN UNION,</p>"
                + "            </formula>"
                + "            <citations GUID=\"cits\" leos:editable=\"true\">"
                + "                <citation GUID=\"cit_1\">"
                + "                    <p GUID=\"cit_1__p\">Having regard to the Treaty on the Functioning of the European Union, and in particular Article [...] thereof,</p>"
                + "                </citation>"
                + "                <citation GUID=\"cit_2\">"
                + "                    <p GUID=\"cit_2__p\">Having regard to the proposal from the European Commission,</p>"
                + "                </citation>"
                + "                <citation GUID=\"cit_3\">"
                + "                    <p GUID=\"cit_3__p\">After transmission of the draft legislative act to the national Parliaments,</p>"
                + "                </citation>"
                + "                <citation GUID=\"cit_4\">"
                + "                    <p GUID=\"cit_4__p\">Having regard to the opinion of the European Economic and Social Committee<authorialNote GUID=\"authorialnote_1\" marker=\"1\" placement=\"bottom\">"
                + "                        <p GUID=\"authorialNote_1__p\">OJ C [...], [...], p. [...]</p>"
                + "                    </authorialNote>,</p>"
                + "                </citation>"
                + "            </citations>"
                + "            <block GUID=\"preamble__block\" name=\"recitalsIntro\">Whereas:</block>"
                + "            <recitals GUID=\"recs\" leos:editable=\"true\">"
                + "                <recital GUID=\"rec_1\">"
                + "                    <num GUID=\"rec_1__num\">(1)</num>"
                + "                    <p GUID=\"rec_1__p\">Recital...</p>"
                + "                </recital>"
                + "                <recital GUID=\"rec_2\">"
                + "                    <num GUID=\"rec_2__num\">(2)</num>"
                + "                    <p GUID=\"rec_2__p\">Recital...</p>"
                + "                </recital>"
                + "                <recital GUID=\"rec_3\">"
                + "                    <num GUID=\"rec_3__num\">(3)</num>"
                + "                    <p GUID=\"rec_3__p\">Recital...</p>"
                + "                </recital>"
                + "                <recital GUID=\"rec_4\">"
                + "                    <num GUID=\"rec_4__num\">(4)</num>"
                + "                    <p GUID=\"rec_4__p\">Recital...</p>"
                + "                </recital>"
                + "            </recitals>"
                + "            <formula GUID=\"preamble__formula_2\" name=\"adoption\">"
                + "                <p GUID=\"preamble__formula_2__p\">HAVE ADOPTED THIS REGULATION:</p>"
                + "            </formula>"
                + "        </preamble>"
                + "        <body GUID=\"body\">"
                + "             <part GUID=\"part11\">"
                + "                <num GUID=\"part1n1\" class=\"PartNumber\">Part XI</num>"
                + "                <heading GUID=\"part1h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                + "                <article GUID=\"a1\">"
                + "                    <num GUID=\"a1n1\" class=\"ArticleNumber\">Article 1</num>"
                + "                    <paragraph GUID=\"a1p1\">"
                + "                        <num  GUID=\"a1p1n1\" class=\"Paragraph(numbered)\">1.</num>"
                + "                        <content GUID=\"a1p1c1\" >"
                + "                            <p  GUID=\"a1p1c1p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"2\" GUID=\"a1au1\"><p GUID=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                + "                        </content>"
                + "                    </paragraph>"
                + "                    <paragraph GUID=\"a1p2\">"
                + "                        <num  GUID=\"a1p2n1\" class=\"Paragraph(numbered)\">2.</num>"
                + "                        <content GUID=\"a1p2c1\">"
                + "                            <p  GUID=\"a1p2c1p1\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"4\" GUID=\"a2a2\"><p GUID=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                + "                        </content>"
                + "                    </paragraph>"
                + "                    <paragraph GUID=\"a1p3\">"
                + "                        <num  GUID=\"a1p3n1\" class=\"Paragraph(numbered)\">3.</num>"
                + "                        <content GUID=\"a1p3c1\">"
                + "                            <p  GUID=\"a1p3c1p1\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015.</p>"
                + "                        </content>"
                + "                    </paragraph>"
                + "                </article>"
                + "                <article GUID=\"a2\">"
                + "                    <num  GUID=\"a2n3\" class=\"ArticleNumber\">Article 20</num>"
                + "                    <alinea GUID=\"a2aal1\">"
                + "                        <content GUID=\"a2c1\">"
                + "                            <p GUID=\"a2c1p1\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i GUID=\"i2\">Official Journal of the European<authorialNote marker=\"8\" GUID=\"a2au3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                + "                        </content>"
                + "                    </alinea>"
                + "                </article>"
                + "            </part>"
                + "             <part GUID=\"part22\">"
                + "                <num GUID=\"part2n1\" class=\"PartNumber\">Part XII</num>"
                + "                <heading GUID=\"part2h1\" class=\"PartHeading\">POST  PROVISIONS</heading>"
                + "                <article GUID=\"a3\">"
                + "                    <num  GUID=\"a3n3\" class=\"ArticleNumber\">Article 21</num>"
                + "                    <alinea GUID=\"a3aal1\">"
                + "                        <content GUID=\"a3c1\">"
                + "                            <p GUID=\"a3c1p1\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i GUID=\"i2\">Official Journal of the European<authorialNote marker=\"8\" GUID=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                + "                        </content>"
                + "                    </alinea>"
                + "                </article>"
                + "             </part>"
                + "             <part GUID=\"part3\">"
                + "                <num GUID=\"part3n1\" class=\"PartNumber\">Part XIV</num>"
                + "                <heading GUID=\"part3h1\" class=\"PartHeading\">POST FINAL PROVISIONS</heading>"
                + "                 <section GUID=\"p3s1\">"
                + "                    <num GUID=\"part3n1\" class=\"PartNumber\">Section X</num>"
                + "                    <heading GUID=\"part3h1\" class=\"PartHeading\">POST FINAL PROVISIONS</heading>"
                + "                    <article GUID=\"a4\">"
                + "                        <num  GUID=\"a4n3\" class=\"ArticleNumber\">Article 45</num>"
                + "                        <paragraph GUID=\"art_1_i0IkfN\">"
                + "                             <num GUID=\"art_1_uB6sEl\">1.</num>"
                + "                            <subparagraph GUID=\"art_1_D48DVp\">"
                + "                                <content GUID=\"art_1_bFb2ix\">"
                + "                                    <p GUID=\"art_1_iXITCG\">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris libero odio, suscipit eu tincidunt at, viverra sit amet nisl."
                + "                                    </p>"
                + "                                 </content>"
                + "                             </subparagraph>"
                + "                    <list GUID=\"art_1_bUwn2R\">"
                + "                        <point GUID=\"art_1_4T9QRZ\">"
                + "                            <num GUID=\"art_1_VNANTx\">(a)</num>"
                + "                            <alinea GUID=\"art_1_pQXVCt\">"
                + "                                <content GUID=\"art_1_CUCaDf\">"
                + "                                    <p GUID=\"art_1_aW5OHt\">Quisque quis est tortor. Etiam ac lectus in eros mollis mollis nec et lacus. Duis non arcu ut mi volutpat feugiat."
                + "                                    </p>"
                + "                                </content>"
                + "                            </alinea>"
                + "                            <list GUID=\"art_1_4DUkpJ\">"
                + "                                <point GUID=\"art_1_w74QP2\">"
                + "                                    <num GUID=\"art_1_GMlAFk\">(i)</num>"
                + "                                    <content GUID=\"art_1_l3LI05\">"
                + "                                        <p GUID=\"art_1_zpb87z\">Nam venenatis sapien tellus, sed iaculis est facilisis non. Sed ut elit erat. Interdum et malesuada fames ac ante ipsum primis in faucibus."
                + "                                        </p>"
                + "                                    </content>"
                + "                                </point>"
                + "                                <point GUID=\"art_1_PcA2LM\">"
                + "                                    <num GUID=\"art_1_2ewtEH\">(ii)</num>"
                + "                                    <content GUID=\"art_1_KhZCZd\">"
                + "                                        <p GUID=\"art_1_GFgFMf\">Suspendisse ac ante sit amet magna consectetur semper vehicula in tellus. Suspendisse non mollis dolor."
                + "                                        </p>"
                + "                                    </content>"
                + "                                </point>"
                + "                            </list>"
                + "                        </point>"
                + "                        <point GUID=\"art_1_1uV2VS\">"
                + "                            <num GUID=\"art_1_N5soNE\">(b)</num>"
                + "                            <content GUID=\"art_1_teYKVT\">"
                + "                                <p GUID=\"art_1_1Det6t\">Donec quis est sapien. Donec efficitur nulla non erat egestas consectetur eu eu sem."
                + "                                </p>"
                + "                            </content>"
                + "                        </point>"
                + "                    </list>"
                + "                </paragraph>"
                + "                <paragraph GUID=\"art_1_TxunI0\">"
                + "                    <num GUID=\"art_1_ad3Rbl\">2.</num>"
                + "                    <content GUID=\"art_1_fISEsZ\">"
                + "                        <p GUID=\"art_1_oxdTif\">Pellentesque congue orci ac tincidunt ultrices. Mauris volutpat quam ut feugiat auctor. Phasellus eros est, pharetra nec sollicitudin commodo, dictum quis ligula."
                + "                        </p>"
                + "                    </content>"
                + "                </paragraph>"
                + "                <paragraph GUID=\"art_1_jqI9E1\">"
                + "                    <num GUID=\"art_1_ORVJhj\">3.</num>"
                + "                    <subparagraph GUID=\"art_1_8BEsrS\">"
                + "                        <content GUID=\"art_1_UyWhui\">"
                + "                            <p GUID=\"art_1_TKV1Yb\">Nullam commodo auctor arcu, quis fringilla metus malesuada convallis. Pellentesque placerat justo in malesuada scelerisque. Mauris vel lorem nulla. Mauris nunc est, tincidunt ac justo sed, molestie aliquam lacus."
                + "                            </p>"
                + "                        </content>"
                + "                    </subparagraph>"
                + "                    <list GUID=\"art_1_ViIaZT\">"
                + "                        <point GUID=\"art_1_jMGiAd\">"
                + "                            <num GUID=\"art_1_0PiSVo\">(a)</num>"
                + "                            <alinea GUID=\"art_1_w3g0rn\">"
                + "                                <content GUID=\"art_1_LzQCtQ\">"
                + "                                    <p GUID=\"art_1_HnxDcU\">Nulla facilisi. In porta ultricies orci non sollicitudin. Curabitur gravida neque eu sem vestibulum, sed gravida ex ultricies."
                + "                                    </p>"
                + "                                </content>"
                + "                            </alinea>"
                + "                            <list GUID=\"art_1_hvcWBw\">"
                + "                                <point GUID=\"art_1_OrhWbv\">"
                + "                                    <num GUID=\"art_1_2DSp05\">(i)</num>"
                + "                                    <content GUID=\"art_1_VkisV8\">"
                + "                                        <p GUID=\"art_1_MzfGNC\">Duis erat neque, consectetur eu odio nec, suscipit euismod orci. Mauris euismod interdum malesuada."
                + "                                        </p>"
                + "                                    </content>"
                + "                                </point>"
                + "                                <point GUID=\"art_1_Uxo4c1\">"
                + "                                    <num GUID=\"art_1_GevAET\">(ii)</num>"
                + "                                    <alinea GUID=\"art_1_jM5Tig\">"
                + "                                        <content GUID=\"art_1_ezShzW\">"
                + "                                            <p GUID=\"art_1_Rjqlk5\">asdadasd</p>"
                + "                                        </content>"
                + "                                    </alinea>"
                + "                                    <list GUID=\"art_1_iWQoV4\">"
                + "                                        <point GUID=\"art_1_l3MS0E\">"
                + "                                            <num GUID=\"art_1_IJgtM3\">-</num>"
                + "                                            <alinea GUID=\"art_1_5wNT45\">"
                + "                                                <content GUID=\"art_1_FCHhwd\">"
                + "                                                    <p GUID=\"art_1_HGUYv5\">Ut eget massa quis erat pulvinar finibus sed pretium sem. Sed id nunc ac odio posuere hendrerit sed ac tellus. Aenean dapibus dignissim diam, in laoreet risus condimentum a. Aliquam tincidunt tellus non enim maximus blandit."
                + "                                                    </p>"
                + "                                                </content>"
                + "                                            </alinea>"
                + "                                            <list GUID=\"art_1_vU7GDV\">"
                + "                                                <point GUID=\"art_1_DBRTQn\">"
                + "                                                    <num GUID=\"art_1_YUGkcC\">-</num>"
                + "                                                    <content GUID=\"art_1_MgBQ2G\">"
                + "                                                        <p GUID=\"art_1_KG1KdL\">"
                + "                                                            Pellentesque accumsan dapibus velit, ac malesuada sapien aliquet ut. Aliquam quis enim non dui sodales aliquet. In sed ante in ligula blandit malesuada. Maecenas nec metus iaculis, porta ex id, mollis felis."
                + "                                                        </p>"
                + "                                                    </content>"
                + "                                                </point>"
                + "                                            </list>"
                + "                                        </point>"
                + "                                    </list>"
                + "                                </point>"
                + "                                <point GUID=\"art_1_CY6Nsa\">"
                + "                                    <num GUID=\"art_1_ETB4nx\">(iii)</num>"
                + "                                    <content GUID=\"art_1_O2aavx\">"
                + "                                        <p GUID=\"art_1_7o6VRy\">Sed sem magna, cursus et commodo ultricies, ultrices sed libero. Ut vel ex odio. Suspendisse viverra tempor sapien, facilisis suscipit quam porttitor in."
                + "                                        </p>"
                + "                                    </content>"
                + "                                </point>"
                + "                            </list>"
                + "                        </point>"
                + "                        <point GUID=\"art_1_Orvvv\">"
                + "                              <num GUID=\"art_1_2Dcccc\">(b)</num>"
                + "                              <content GUID=\"art_1_Vkcc8\">"
                + "                                   <p GUID=\"art_1_Mccc\">Duis erat neque, consectetur eu odio nec, suscipit euismod orci. Mauris euismod interdum malesuada."
                + "                                   </p>"
                + "                              </content>"
                + "                        </point>"
                + "                    </list>"
                + "                </paragraph>"
                + "                <paragraph GUID=\"art_1_kpm2jb\">"
                + "                    <num GUID=\"art_1_H1XU47\">4.</num>"
                + "                    <subparagraph GUID=\"art_1_S9i3Fz\">"
                + "                        <content GUID=\"art_1_oczbxe\">"
                + "                            <p GUID=\"art_1_eyHMBk\">Nulla ut odio rutrum, tempus lorem a, tincidunt nisl. Fusce convallis pellentesque nisi, sit amet iaculis neque pharetra ut. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae;"
                + "                            </p>"
                + "                        </content>"
                + "                    </subparagraph>"
                + "                    <list GUID=\"art_1_kloBJf\">"
                + "                        <point GUID=\"art_1_vZe95X\">"
                + "                            <num GUID=\"art_1_2TXvp3\">(a)</num>"
                + "                            <content GUID=\"art_1_2UKqwj\">"
                + "                                <p GUID=\"art_1_elCZdB\">Phasellus efficitur viverra ex, maximus volutpat erat feugiat ac. Cras augue nunc, elementum vitae porttitor sit amet, posuere nec ex."
                + "                                </p>"
                + "                            </content>"
                + "                        </point>"
                + "                        <point GUID=\"art_1_830kLA\">"
                + "                            <num GUID=\"art_1_gkz6kf\">(b)</num>"
                + "                            <alinea GUID=\"art_1_Cfr97w\">"
                + "                                <content GUID=\"art_1_xmNDzv\">"
                + "                                    <p GUID=\"art_1_JDj92t\">Vestibulum egestas, ante dignissim auctor vulputate, turpis nisi pulvinar enim, molestie lacinia dolor justo nec leo."
                + "                                    </p>"
                + "                                </content>"
                + "                            </alinea>"
                + "                            <list GUID=\"art_1_rUHNOL\">"
                + "                                <point GUID=\"art_1_hZrAiD\">"
                + "                                    <num GUID=\"art_1_cQhlH3\">(i)</num>"
                + "                                    <content GUID=\"art_1_UsubFK\">"
                + "                                        <p GUID=\"art_1_rSidlP\">Phasellus ullamcorper, tellus ac pellentesque volutpat, neque eros placerat erat, eu suscipit tellus lacus at risus. I"
                + "                                        </p>"
                + "                                    </content>"
                + "                                </point>"
                + "                                <point GUID=\"art_1_NibNsa\">"
                + "                                    <num GUID=\"art_1_2daSks\">(ii)</num>"
                + "                                    <content GUID=\"art_1_8Z1O9o\">"
                + "                                        <p GUID=\"art_1_GTysXx\">nteger nec diam enim. Duis sit amet maximus elit. Morbi lacus risus, ornare sed tincidunt eget, elementum eu arcu."
                + "                                        </p>"
                + "                                    </content>"
                + "                                </point>"
                + "                            </list>"
                + "                        </point>"
                + "                        <point GUID=\"art_1_CkbJ1I\">"
                + "                            <num GUID=\"art_1_9jmaxo\">(c)</num>"
                + "                            <content GUID=\"art_1_vqv65v\">"
                + "                                <p GUID=\"art_1_CB3nb2\">Vestibulum ac neque tempor, pellentesque arcu accumsan, blandit metus. Proin luctus justo vitae lectus congue tempus."
                + "                                </p>"
                + "                            </content>"
                + "                        </point>"
                + "                    </list>"
                + "                </paragraph>"
                + "                <paragraph GUID=\"art_1_A42pW6\">"
                + "                    <num GUID=\"art_1_AYrfwX\">5.</num>"
                + "                    <content GUID=\"art_1_vvi8Re\">"
                + "                        <p GUID=\"art_1_m9ZMyE\">Donec consequat neque aliquet, scelerisque dolor sit amet, vehicula enim. Morbi ullamcorper ligula ac cursus sagittis. Aenean fringilla enim quis varius molestie. Sed ut massa tortor. Nam bibendum nisl eu mi faucibus, quis aliquet augue molestie. Curabitur venenatis ligula leo, non bibendum est suscipit at."
                + "                        </p>"
                + "                    </content>"
                + "                </paragraph>"
                + "                <paragraph GUID=\"art_1_BGt5eN\">"
                + "                    <num GUID=\"art_1_5fZk0t\">6.</num>"
                + "                    <content GUID=\"art_1_GlIzqV\">"
                + "                        <p GUID=\"art_1_HmBP4y\">Etiam placerat, nisl sit amet consequat facilisis, augue mauris tristique orci, vel fermentum odio eros non ipsum. Nunc auctor auctor nibh eu ultrices. Cras nunc odio, varius hendrerit vehicula nec, vehicula a ex."
                + "                        </p>"
                + "                    </content>"
                + "                </paragraph>"
                + "                     </article>"
                + "                 </section>"
                + "                  <section GUID=\"p3s2\">"
                + "                    <num GUID=\"part3ns2\" class=\"PartNumber\">Section XI</num>"
                + "                    <heading GUID=\"part3h123\" class=\"PartHeading\">POST FINAL PROVISIONS</heading>"
                + "                 </section>"
                + "             </part>"
                + "          </body>"
                + "        </bill>"
                + "</akomaNtoso> ";

        docContent = doc.getBytes(UTF_8);
        ReflectionTestUtils.setField(referenceLabelGenerator, "labelHandlers",
                Arrays.asList(new LabelHigherOrderElementsOnly(),
                        new LabelArticlesOrRecitalsOnly(),
                        new LabelNumberedElementOnly(),
                        new LabelUnnumberedElementOnly()));
    }

    @InjectMocks
    private ReferenceLabelServiceImpl referenceLabelGenerator = new ReferenceLabelServiceImpl();

    @Test
    public void generateLabel_singleRefWithArticle() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",a1"), "", docContent, "en");
        String expectedResults = "Article <ref GUID=\"\" href=\"a1\">1</ref>";
        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Section_multiRefWithSection() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",p3s1"), "", docContent, "en");
        String expectedResults = "Part XIV Section <ref GUID=\"\" href=\"p3s1\">X</ref>";
        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Section_multiRefWith2Section() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",p3s2", ",p3s1"), "", docContent, "en");
        String expectedResults = "Part XIV Section <ref GUID=\"\" href=\"p3s1\">X</ref> and <ref GUID=\"\" href=\"p3s2\">XI</ref>";
        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Higher_multiRefWith2Articles() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",a3", ",a2"), "", docContent, "en");
        String expectedResults = "Article "
                + "<ref GUID=\"\" href=\"a2\">20</ref>"
                + " and "
                + "<ref GUID=\"\" href=\"a3\">21</ref>";
        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Higher_multiRefWith2siblingArticles() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",a2", ",a1"), "", docContent, "en");
        String expectedResults = "Article "
                + "<ref GUID=\"\" href=\"a1\">1</ref>"
                + " and "
                + "<ref GUID=\"\" href=\"a2\">20</ref>";
        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Recitals_multiRefWith3Recitals() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",rec_4", ",rec_1", ",rec_2"), "", docContent, "en");
        String expectedResults = "Recital "
                + "<ref GUID=\"\" href=\"rec_1\">(1)</ref>"
                + ", <ref GUID=\"\" href=\"rec_2\">(2)</ref>"
                + " and "
                + "<ref GUID=\"\" href=\"rec_4\">(4)</ref>";
        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Recitals_multiRefWith2siblingRecitals() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",rec_1", ",rec_3"), "", docContent, "en");
        String expectedResults = "Recital "
                + "<ref GUID=\"\" href=\"rec_1\">(1)</ref>"
                + " and "
                + "<ref GUID=\"\" href=\"rec_3\">(3)</ref>";
        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Recitals_multiRefWith1Recitals() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",rec_1"), "", docContent, "en");
        String expectedResults = "Recital "
                + "<ref GUID=\"\" href=\"rec_1\">(1)</ref>";
        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Higher_multiRefWith3Articles() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",a3", ",a1", ",a2"), "", docContent, "en");
        String expectedResults = "Article <ref GUID=\"\" href=\"a1\">1</ref>, "
                + "<ref GUID=\"\" href=\"a2\">20</ref>"
                + " and "
                + "<ref GUID=\"\" href=\"a3\">21</ref>";
        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Higher_singleRefWithParts() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",part11"), "", docContent, "en");
        String expectedResults = "Part <ref GUID=\"\" href=\"part11\">XI</ref>";
        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Article_withSingleParagraphReference() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",a1p3"), "", docContent, "en");
        String expectedResults = "Article 1<ref GUID=\"\" href=\"a1p3\">(3)</ref>";
        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Article_withTwoParagraphReferenceAtLevel1() throws Exception {
        //part XII > Article 1 > Paragraph 3
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",a1p3", ",a1p2"), "", docContent, "en");
        String expectedResults = "Article 1<ref GUID=\"\" href=\"a1p2\">(2)</ref>"
                + " and "
                + "<ref GUID=\"\" href=\"a1p3\">(3)</ref>";
        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Article_withOnePointReferenceAtLevel2() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",art_1_jMGiAd"), "", docContent, "en");
        String expectedResults = "Article 45(3)<ref GUID=\"\" href=\"art_1_jMGiAd\">(a)</ref>";

        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Article_withMultiplePointReferenceAtLevel2() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",art_1_OrhWbv", ",art_1_Uxo4c1", ",art_1_CY6Nsa"), "", docContent, "en");
        String expectedResults = "Article 45(3)(a)<ref GUID=\"\" href=\"art_1_OrhWbv\">(i)</ref>"
                + ", <ref GUID=\"\" href=\"art_1_Uxo4c1\">(ii)</ref>"
                + " and <ref GUID=\"\" href=\"art_1_CY6Nsa\">(iii)</ref>";

        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Article_withMultiplePointReferenceAtLevel2_same_article() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",art_1_OrhWbv", ",art_1_Uxo4c1", ",art_1_CY6Nsa"), "art_1_oxdTif", docContent, "en");
        String expectedResults = "(3)(a)<ref GUID=\"\" href=\"art_1_OrhWbv\">(i)</ref>"
                + ", <ref GUID=\"\" href=\"art_1_Uxo4c1\">(ii)</ref>"
                + " and <ref GUID=\"\" href=\"art_1_CY6Nsa\">(iii)</ref>";

        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Article_withMultiplePointReferenceAtLevel2_same_paragraph() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",art_1_OrhWbv", ",art_1_Uxo4c1", ",art_1_CY6Nsa"), "art_1_TKV1Yb", docContent, "en");
        String expectedResults = "(a)<ref GUID=\"\" href=\"art_1_OrhWbv\">(i)</ref>"
                + ", <ref GUID=\"\" href=\"art_1_Uxo4c1\">(ii)</ref>"
                + " and <ref GUID=\"\" href=\"art_1_CY6Nsa\">(iii)</ref>";

        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_Article_withMultiplePointReferenceAtLevel2_same_point() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",art_1_OrhWbv", ",art_1_Uxo4c1", ",art_1_CY6Nsa"), "art_1_HnxDcU", docContent, "en");
        String expectedResults = "<ref GUID=\"\" href=\"art_1_OrhWbv\">(i)</ref>"
                + ", <ref GUID=\"\" href=\"art_1_Uxo4c1\">(ii)</ref>"
                + " and <ref GUID=\"\" href=\"art_1_CY6Nsa\">(iii)</ref>";

        assertEquals(expectedResults, result.get());
    }


    @Test
    public void generateLabel_Recitals_withMultipleRecitals_same_recitals() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",rec_1", ",rec_2", ",rec_3"), "rec_2", docContent, "en");
        String expectedResults = "Recital <ref GUID=\"\" href=\"rec_1\">(1)</ref>"
                + ", <ref GUID=\"\" href=\"rec_2\">(2)</ref>"
                + " and <ref GUID=\"\" href=\"rec_3\">(3)</ref>";

        assertEquals(expectedResults, result.get());
    }

    @Test
    @Ignore
    //This case across levels is not handled
    public void generateLabel_Article_withMultiplePointReferenceAtLevel2andLevel3() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",art_1_OrhWbv", ",art_1_Uxo4c1", ",art_1_Orvvv", ",art_1_CY6Nsa"), "", docContent, "en");
        String expectedResults = "Article 45(3)<ref GUID=\"\" href=\"art_1_OrhWbv\">(i)</ref>"
                + ", (a)<ref GUID=\"\" href=\"art_1_Uxo4c1\">(ii)</ref>"
                + ", (a)<ref GUID=\"\" href=\"art_1_CY6Nsa\">(iii)</ref>"
                + " and <ref GUID=\"\" href=\"art_1_Uxo4c1\">(b)</ref>";

        assertEquals(expectedResults, result.get());
    }

    @Test
    public void generateLabel_broken_refs_same_type() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",a1p2", ",a2aal1"), "", docContent, "en");
        assertEquals(true, result.isError());
        assertEquals(ErrorCode.DOCUMENT_REFERENCE_NOT_VALID, result.getErrorCode().get());
        assertEquals(null, result.get());
    }

    @Test
    public void generateLabel_broken_refs() throws Exception {
        Result<String> result = referenceLabelGenerator.generateLabel(Arrays.asList(",a1p2", ",a2aal"), "", docContent, "en");
        assertEquals(true, result.isError());
        assertEquals(ErrorCode.DOCUMENT_REFERENCE_NOT_VALID, result.getErrorCode().get());
        assertEquals(null, result.get());
    }
}