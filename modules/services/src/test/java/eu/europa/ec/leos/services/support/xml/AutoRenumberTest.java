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

import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MandateMessageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.services.content.TemplateStructureService;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.services.toc.StructureServiceImpl;
import eu.europa.ec.leos.services.util.TestUtils;
import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.vo.toc.NumberingConfig;
import eu.europa.ec.leos.vo.toc.TocItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import javax.inject.Provider;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class AutoRenumberTest extends LeosTest {
    
    @InjectMocks
    private MessageHelper messageHelper = Mockito.spy(getMessageHelper());
    
    @Mock
    private LanguageHelper languageHelper;

    @Mock
    private Provider<StructureContext> structureContextProvider;

    @Mock
    private StructureContext structureContext;

    @InjectMocks
    private ElementNumberingHelper elementNumberingHelper = new ElementNumberingHelper(messageHelper, structureContextProvider);

    @InjectMocks
    private StructureServiceImpl structureServiceImpl;

    @Mock
    private TemplateStructureService templateStructureService;

    @InjectMocks
    private NumberProcessor proposalNumberingProcessor = new ProposalNumberingProcessor(elementNumberingHelper, messageHelper);

    @InjectMocks
    private NumberProcessor mandateNumberingProcessor = new MandateNumberingProcessor(elementNumberingHelper);
    
    @InjectMocks
    private VtdXmlContentProcessor vtdXmlContentProcessor = new VtdXmlContentProcessorForProposal();

    private String  docTemplate;
    private List<TocItem> tocItemList;
    private List<NumberingConfig> numberingConfigs;

    @Before
    public void setUp() throws Exception{
        docTemplate = "BL-023";
        byte[] bytesFile = TestUtils.getFileContent("/structure-test.xml");
        when(templateStructureService.getStructure(docTemplate)).thenReturn(bytesFile);
        ReflectionTestUtils.setField(structureServiceImpl, "structureSchema", "toc/schema/structure_1.xsd");
        tocItemList = structureServiceImpl.getTocItems(docTemplate);
        numberingConfigs = structureServiceImpl.getNumberingConfigs(docTemplate);

        when(structureContextProvider.get()).thenReturn(structureContext);
        when(structureContext.getTocItems()).thenReturn(tocItemList);
        when(structureContext.getNumberingConfigs()).thenReturn(numberingConfigs);
    }
    
    private MessageHelper getMessageHelper() {
        try(ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("test-servicesContext.xml")){
            MessageSource servicesMessageSource = (MessageSource) applicationContext.getBean("servicesMessageSource");
            MessageHelper messageHelper = new MandateMessageHelper(servicesMessageSource);
            return messageHelper;
        }
    }

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
    
    @Test
    public void test_renumberArticle() {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                + "<akomaNtoso xml:id=\"akn\">" +
                "<article xml:id=\"art486\">" +
                "<num xml:id=\"aknum\">Article 486</num>" +
                "<heading xml:id=\"aknhead\">1st article</heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "<num xml:id=\"aknnum2\"></num>" +
                "<heading xml:id=\"aknhead2\">2th articl<authorialNote marker=\"101\" xml:id=\"a1\"><p xml:id=\"p1\">TestNote1</p></authorialNote>e</heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "<heading xml:id=\"aknhead3\">3th article<authorialNote marker=\"90\" xml:id=\"a2\"><p xml:id=\"p2\">TestNote2</p></authorialNote></heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "</article>" +
                "</akomaNtoso>";
        
        byte[] result = proposalNumberingProcessor.renumberArticles(xml.getBytes());
        result = vtdXmlContentProcessor.doXMLPostProcessing(result);


        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                + "<akomaNtoso xml:id=\"akn\">" +
                "<article xml:id=\"art486\">" +
                "<num>Article 1</num>" +
                "<heading xml:id=\"aknhead\">1st article</heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "<num>Article 2</num>" +
                "<heading xml:id=\"aknhead2\">2th articl<authorialNote marker=\"1\" xml:id=\"a1\"><p xml:id=\"p1\">TestNote1</p></authorialNote>e</heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "<num>Article 3</num>" +
                "<heading xml:id=\"aknhead3\">3th article<authorialNote marker=\"2\" xml:id=\"a2\"><p xml:id=\"p2\">TestNote2</p></authorialNote></heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "<num>Article 4</num>" +
                "</article>" +
                "</akomaNtoso>";

        assertThat(new String(result).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_manualRenumberingArticle() {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.-->" +
                "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xml:id=\"akn\">" +
                "<body leos:origin=\"ec\" xml:id=\"body\">" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num leos:origin=\"cn\">Article #</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num leos:origin=\"cn\">Article #</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num  leos:origin=\"cn\">Article #</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"ec\">" +
                "<num  leos:origin=\"ec\">Article 3</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num  leos:origin=\"cn\">Article #</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"ec\">" +
                "<num  leos:origin=\"ec\">Article 5</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num  leos:origin=\"cn\">Article #</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num leos:origin=\"cn\">Article #</num>" +
                "</article>" +
                "</body>" +
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                + "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xml:id=\"akn\">" +
                "<body leos:origin=\"ec\" xml:id=\"body\">" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num leos:origin=\"cn\">Article -3</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num leos:origin=\"cn\">Article -2</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num  leos:origin=\"cn\">Article -1</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"ec\">" +
                "<num  leos:origin=\"ec\">Article 3</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num  leos:origin=\"cn\">Article 3a</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"ec\">" +
                "<num  leos:origin=\"ec\">Article 5</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num  leos:origin=\"cn\">Article 5a</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num leos:origin=\"cn\">Article 5b</num>" +
                "</article>" +
                "</body>" +
                "</akomaNtoso>";

        assertThat(new String(result).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_manualRenumberingRecitals() {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"+
                "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xml:id=\"akn\">" +
                "<recitals xml:id=\"recs\" leos:editable=\"false\" leos:origin=\"ec\">"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num xml:id=\"recs_WiyG0V\"  leos:origin=\"cn\">(#)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...-3</p>"+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num xml:id=\"recs_WiyG0V\" leos:origin=\"cn\">(#)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...-2</p>"+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num xml:id=\"recs_WiyG0V\" leos:origin=\"cn\">(#)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...-1</p>"+
                "</recital>"+
                "<recital xml:id=\"rec_1\" leos:editable=\"true\" leos:origin=\"ec\">"+
                "<num xml:id=\"rec_1__num\" leos:origin=\"ec\">(1)</num>"+
                "<p xml:id=\"rec_1__p\" leos:origin=\"ec\">Regulation (EC) No 91/2003 of the European Parliament and of the Council</p>"+
                "</recital>"+
                "<recital xml:id=\"imp_rec_d1e89_s3AqGd\" leos:editable=\"true\" leos:origin=\"ec\">"+
                "<num xml:id=\"recs_clDXkw\" leos:origin=\"ec\">(2)</num>"+
                "<p xml:id=\"recs_vY7Jn8\" leos:origin=\"ec\">Regulation (EC) No 91/2003 of the European Parliament and of the Council</p>     "+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num xml:id=\"recs_WiyG0V\" leos:origin=\"cn\">(#)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...2a</p>"+
                "</recital>"+
                "<recital xml:id=\"imp_rec_d1e89_quAXDO\" leos:editable=\"true\" leos:origin=\"ec\">"+
                "<num xml:id=\"recs_o4jNQj\" leos:origin=\"ec\">(3)</num>"+
                "<p xml:id=\"recs_hAbYkK\" leos:origin=\"ec\">Regulation (EC) No 91/2003 of the European Parliament and of the Council </p>"+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num xml:id=\"recs_WiyG0V\"  leos:origin=\"cn\">(#)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...3a</p>"+
                "</recital>"+
                "<recital xml:id=\"recs_Lt8Khg\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num xml:id=\"recs_nRwj01\"  leos:origin=\"cn\">(#)</num>"+
                "<p xml:id=\"recs_m67lb6\">blah blah blah....3b</p>"+
                "</recital>"+
                "</recitals>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberRecitals(xml.getBytes());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                +"<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xml:id=\"akn\">" +
                "<recitals xml:id=\"recs\" leos:editable=\"false\" leos:origin=\"ec\">"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num>(-3)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...-3</p>"+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num>(-2)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...-2</p>"+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num>(-1)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...-1</p>"+
                "</recital>"+
                "<recital xml:id=\"rec_1\" leos:editable=\"true\" leos:origin=\"ec\">"+
                "<num>(1)</num>"+
                "<p xml:id=\"rec_1__p\" leos:origin=\"ec\">Regulation (EC) No 91/2003 of the European Parliament and of the Council</p>"+
                "</recital>"+
                "<recital xml:id=\"imp_rec_d1e89_s3AqGd\" leos:editable=\"true\" leos:origin=\"ec\">"+
                "<num>(2)</num>"+
                "<p xml:id=\"recs_vY7Jn8\" leos:origin=\"ec\">Regulation (EC) No 91/2003 of the European Parliament and of the Council</p>     "+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num>(2a)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...2a</p>"+
                "</recital>"+
                "<recital xml:id=\"imp_rec_d1e89_quAXDO\" leos:editable=\"true\" leos:origin=\"ec\">"+
                "<num>(3)</num>"+
                "<p xml:id=\"recs_hAbYkK\" leos:origin=\"ec\">Regulation (EC) No 91/2003 of the European Parliament and of the Council </p>"+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num>(3a)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...3a</p>"+
                "</recital>"+
                "<recital xml:id=\"recs_Lt8Khg\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num>(3b)</num>"+
                "<p xml:id=\"recs_m67lb6\">blah blah blah....3b</p>"+
                "</recital>"+
                "</recitals>"+
                "</akomaNtoso>";

        assertThat(new String(result).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }


    @Test
    public void test_manualRenumberingParagraphs() {


        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"cn\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"cn\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_1\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__a\">aaaa</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_1\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__b\">bbbbb</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para2</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_1\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__c\">cccc</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para3</p>"+
                "</content>"+
                "</paragraph>"+
                "</article>"+
                "<article leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"akn_art_YnSKbj_N7bQVT\">Article 2</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_dqTi7R\">Article heading...</heading>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_x\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">xxxxxxx</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_y\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">yyyyyyyy</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_zrk0Mz\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_akUEgv\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_RmxNQr\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_UqAJE4\">ergregregerg</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yiJDrd\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_alkc60\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yR3ne2\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_ayJnjd\">tgtrg</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_y\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">xxxxxxx</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_y\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">aaaaaaaa</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_CsZvQu\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_3nkQqe\">2.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_RmxNQr\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_UqAJE4\">ergregregerg</p>"+
                "</content>"+
                "</subparagraph>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_z\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">xxxxxxx</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes());

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"cn\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"cn\" leos:editable=\"false\" xml:id=\"art_1__num\">Article -1</num>"+
                "<heading leos:origin=\"cn\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_1\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__a\">1.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_1\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__b\">2.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para2</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_1\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__c\">3.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para3</p>"+
                "</content>"+
                "</paragraph>"+
                "</article>"+
                "<article leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"akn_art_YnSKbj_N7bQVT\">Article 2</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_dqTi7R\">Article heading...</heading>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_x\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">-2.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_y\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">-1.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_zrk0Mz\" >"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_akUEgv\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_RmxNQr\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_UqAJE4\">ergregregerg</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(-b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(-a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(aa)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yiJDrd\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_alkc60\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yR3ne2\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_ayJnjd\">tgtrg</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(ba)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(bb)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_y\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">1a.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_y\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">1b.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_CsZvQu\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_3nkQqe\">2.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_RmxNQr\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_UqAJE4\">ergregregerg</p>"+
                "</content>"+
                "</subparagraph>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_z\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">2a.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }



    @Test
    public void test_elementNumbering_when_mandate_list_added_to_proposal_paragraph() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes());

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_elementNumbering_when_nested_list_added() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes());

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(2)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_pointsNumbering_when_point_having_single_level_nested_list() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes());

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(2)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(2)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_pointsNumbering_when_point_having_multi_level_nested_list_1() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes());

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(2)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_pointsNumbering_when_point_having_multi_level_nested_list_2() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes());

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(2)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(2)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_RenumberingPoints_with_nested_lists() {


        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"akn_art_YnSKbj_N7bQVT\">Article 2</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_dqTi7R\">Article heading...</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_zrk0Mz\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_akUEgv\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_RmxNQr\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_UqAJE4\">ergregregerg</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yiJDrd\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_alkc60\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yR3ne2\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_ayJnjd\">tgtrg</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes());

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"akn_art_YnSKbj_N7bQVT\">Article 2</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_dqTi7R\">Article heading...</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_zrk0Mz\" >"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_akUEgv\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_RmxNQr\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_UqAJE4\">ergregregerg</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(-b)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(-a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(aa)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yiJDrd\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_alkc60\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yR3ne2\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_ayJnjd\">tgtrg</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(ba)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(bb)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_pointsNumbering_when_point_added_on_negative_side() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(-a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes());

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(-a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(-a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_pointsNumbering_when_point_added_between_proposal_point() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(aa)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(ba)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes());

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(aa)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(aa)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(ba)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(ba)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_pointsNumbering_when_nested_point_added_to_proposal_point() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">-</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point-</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">-</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point-</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes());

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">-</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point-</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">-</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point-</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_pointsNumbering_when_nested_point_added_to_mandate_point() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(aa)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(aa)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(aa1)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(ba)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(ba)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(ba1)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes());

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(aa)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(aa)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(aa1)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(ba)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(ba)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(ba1)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_pointsNumbering_when_point_added_to_unnumbered_paragraph() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes());

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(ba)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }
}
