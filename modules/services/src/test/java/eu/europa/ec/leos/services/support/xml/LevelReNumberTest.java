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

public class LevelReNumberTest extends LeosTest {
    
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


    private String  docTemplate;
    private List<TocItem> tocItemList;
    private List<NumberingConfig> numberingConfigs;

    @Before
    public void setUp() throws Exception{
        docTemplate = "SG-017";
        byte[] bytesFile = TestUtils.getFileContent("/structure-test2.xml");
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
    public void test_renumberLevels_when_new_level_added_as_sibling() {
        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                        "<doc name=\"ANNEX\">" +
                        "<mainBody xml:id=\"body\">" +
                        "<level xml:id=\"body_level_1\" leos:depth='1'>"+
                          "<num xml:id=\"body_level_1_num\">1.</num>"+
                          "<content xml:id=\"body_level_1_content\">" +
                              "<p xml:id=\"body_level_1_content_p\">Text...</p>" +
                          "</content>" +
                        "</level>" +
                        "<level xml:id=\"body_level_2\" leos:depth='1'>"+
                            "<num xml:id=\"body_level_2_num\">#</num>"+
                            "<content xml:id=\"body_level_2_content\">" +
                                "<p xml:id=\"body_level_2_content_p\">Text...</p>" +
                            "</content>" +
                        "</level>" +
                        "<level xml:id=\"body_level_1_1\" leos:depth='2'>"+
                            "<num xml:id=\"body_level_1_1_num\">1.1.</num>"+
                            "<content xml:id=\"body_level_1_1_content\">" +
                                "<p xml:id=\"body_level_1_1_content_p\">Text...</p>" +
                            "</content>" +
                        "</level>" +  
                        "<level xml:id=\"body_level_2\" leos:depth='1'>"+
                            "<num xml:id=\"body_level_2_num\">2.</num>"+
                            "<content xml:id=\"body_level_2_content\">" +
                                "<p xml:id=\"body_level_2_content_p\">Text...</p>" +
                            "</content>" +
                        "</level>" + 
                        "</mainBody>" +
                      "</doc>" +
                    "</akomaNtoso>";
        
         byte[] result = proposalNumberingProcessor.renumberLevel(xml.getBytes());
         
         String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                 "<doc name=\"ANNEX\">" +
                 "<mainBody xml:id=\"body\">" +
                 "<level xml:id=\"body_level_1\" >"+
                   "<num xml:id=\"body_level_1_num\">1.</num>"+
                   "<content xml:id=\"body_level_1_content\">" +
                       "<p xml:id=\"body_level_1_content_p\">Text...</p>" +
                   "</content>" +
                 "</level>" +
                 "<level xml:id=\"body_level_2\" >"+
                     "<num xml:id=\"body_level_2_num\">2.</num>"+
                     "<content xml:id=\"body_level_2_content\">" +
                         "<p xml:id=\"body_level_2_content_p\">Text...</p>" +
                     "</content>" +
                 "</level>" +
                 "<level xml:id=\"body_level_1_1\" >"+
                     "<num xml:id=\"body_level_1_1_num\">2.1.</num>"+
                     "<content xml:id=\"body_level_1_1_content\">" +
                         "<p xml:id=\"body_level_1_1_content_p\">Text...</p>" +
                     "</content>" +
                 "</level>" +  
                 "<level xml:id=\"body_level_2\" >"+
                     "<num xml:id=\"body_level_2_num\">3.</num>"+
                     "<content xml:id=\"body_level_2_content\">" +
                         "<p xml:id=\"body_level_2_content_p\">Text...</p>" +
                     "</content>" +
                 "</level>" +
                 "</mainBody>" +
               "</doc>" +
             "</akomaNtoso>";
         
         assertThat(new String(result, UTF_8), is(expected));
    }

    @Test
    public void test_renumberLevels_when_new_level_added_as_child() {
        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                        "<doc name=\"ANNEX\">" +
                        "<mainBody xml:id=\"body\">" +
                        "<level xml:id=\"body_level_1\" leos:depth='1'>"+
                          "<num xml:id=\"body_level_1_num\">1.</num>"+
                          "<content xml:id=\"body_level_1_content\">" +
                              "<p xml:id=\"body_level_1_content_p\">Text...</p>" +
                          "</content>" +
                        "</level>" +
                        "<level xml:id=\"body_level_1_1\" leos:depth='2'>"+
                            "<num xml:id=\"body_level_1_1_num\">#</num>"+
                            "<content xml:id=\"body_level_1_1_content\">" +
                                "<p xml:id=\"body_level_1_1_content_p\">Text...</p>" +
                            "</content>" +
                        "</level>" +
                        "<level xml:id=\"body_level_1_2\" leos:depth='2'>"+
                            "<num xml:id=\"body_level_1_1_2_num\">1.1.</num>"+
                            "<content xml:id=\"body_level_1_1_2_content\">" +
                                "<p xml:id=\"body_level_1_2_content_p\">Text...</p>" +
                            "</content>" +
                        "</level>" +  
                        "<level xml:id=\"body_level_2\" leos:depth='1'>"+
                            "<num xml:id=\"body_level_2_num\">2.</num>"+
                            "<content xml:id=\"body_level_2_content\">" +
                                "<p xml:id=\"body_level_2_content_p\">Text...</p>" +
                            "</content>" +
                        "</level>" + 
                        "</mainBody>" +
                      "</doc>" +
                    "</akomaNtoso>";
        
         byte[] result = proposalNumberingProcessor.renumberLevel(xml.getBytes());
         
         String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                 "<doc name=\"ANNEX\">" +
                 "<mainBody xml:id=\"body\">" +
                 "<level xml:id=\"body_level_1\" >"+
                   "<num xml:id=\"body_level_1_num\">1.</num>"+
                   "<content xml:id=\"body_level_1_content\">" +
                       "<p xml:id=\"body_level_1_content_p\">Text...</p>" +
                   "</content>" +
                 "</level>" +
                 "<level xml:id=\"body_level_1_1\" >"+
                     "<num xml:id=\"body_level_1_1_num\">1.1.</num>"+
                     "<content xml:id=\"body_level_1_1_content\">" +
                         "<p xml:id=\"body_level_1_1_content_p\">Text...</p>" +
                     "</content>" +
                  "</level>" +
                  "<level xml:id=\"body_level_1_2\" >"+
                      "<num xml:id=\"body_level_1_1_2_num\">1.2.</num>"+
                      "<content xml:id=\"body_level_1_1_2_content\">" +
                          "<p xml:id=\"body_level_1_2_content_p\">Text...</p>" +
                      "</content>" +
                  "</level>" +  
                  "<level xml:id=\"body_level_2\" >"+
                     "<num xml:id=\"body_level_2_num\">2.</num>"+
                     "<content xml:id=\"body_level_2_content\">" +
                         "<p xml:id=\"body_level_2_content_p\">Text...</p>" +
                     "</content>" +
                 "</level>" +
                 "</mainBody>" +
               "</doc>" +
             "</akomaNtoso>";
         
         assertThat(new String(result, UTF_8), is(expected));
    }
    
    @Test
    public void test_renumberLevels_when_new_level_added_at_multiple_sublevel() {
        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                        "<doc name=\"ANNEX\">" +
                        "<mainBody xml:id=\"body\">" +
                        "<level xml:id=\"body_level_1\" leos:depth='1'>"+
                          "<num xml:id=\"body_level_1_num\">1.</num>"+
                          "<content xml:id=\"body_level_1_content\">" +
                              "<p xml:id=\"body_level_1_content_p\">Text...</p>" +
                          "</content>" +
                        "</level>" +
                        "<level xml:id=\"body_level_1_1\" leos:depth='2'>"+
                            "<num xml:id=\"body_level_1_1_num\">1.1.</num>"+
                            "<content xml:id=\"body_level_1_1_content\">" +
                                "<p xml:id=\"body_level_1_1_content_p\">Text...</p>" +
                            "</content>" +
                        "</level>" +
                        "<level xml:id=\"body_level_1_1_1\" leos:depth='3'>"+
                            "<num xml:id=\"body_level_1_1_num\">#</num>"+
                            "<content xml:id=\"body_level_1_1_content\">" +
                                "<p xml:id=\"body_level_1_1_content_p\">Text...</p>" +
                            "</content>" +
                        "</level>" +
                        "<level xml:id=\"body_level_1_1_1_1\" leos:depth='4'>"+
                        "<num xml:id=\"body_level_1_1_num\">#</num>"+
                        "<content xml:id=\"body_level_1_1_content\">" +
                            "<p xml:id=\"body_level_1_1_content_p\">Text...</p>" +
                        "</content>" +
                        "</level>" +                            
                        "<level xml:id=\"body_level_2\" leos:depth='1'>"+
                            "<num xml:id=\"body_level_2_num\">2.</num>"+
                            "<content xml:id=\"body_level_2_content\">" +
                                "<p xml:id=\"body_level_2_content_p\">Text...</p>" +
                            "</content>" +
                        "</level>" + 
                        "</mainBody>" +
                      "</doc>" +
                    "</akomaNtoso>";
        
         byte[] result = proposalNumberingProcessor.renumberLevel(xml.getBytes());
         
         String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                 "<doc name=\"ANNEX\">" +
                 "<mainBody xml:id=\"body\">" +
                 "<level xml:id=\"body_level_1\" >"+
                   "<num xml:id=\"body_level_1_num\">1.</num>"+
                   "<content xml:id=\"body_level_1_content\">" +
                       "<p xml:id=\"body_level_1_content_p\">Text...</p>" +
                   "</content>" +
                 "</level>" +
                 "<level xml:id=\"body_level_1_1\" >"+
                     "<num xml:id=\"body_level_1_1_num\">1.1.</num>"+
                     "<content xml:id=\"body_level_1_1_content\">" +
                         "<p xml:id=\"body_level_1_1_content_p\">Text...</p>" +
                     "</content>" +
                  "</level>" +
                  "<level xml:id=\"body_level_1_1_1\" >"+
                      "<num xml:id=\"body_level_1_1_num\">1.1.1.</num>"+
                      "<content xml:id=\"body_level_1_1_content\">" +
                          "<p xml:id=\"body_level_1_1_content_p\">Text...</p>" +
                      "</content>" +
                  "</level>" +
                  "<level xml:id=\"body_level_1_1_1_1\" >"+
                      "<num xml:id=\"body_level_1_1_num\">1.1.1.1.</num>"+
                      "<content xml:id=\"body_level_1_1_content\">" +
                         "<p xml:id=\"body_level_1_1_content_p\">Text...</p>" +
                      "</content>" +
                  "</level>" +  
                  "<level xml:id=\"body_level_2\" >"+
                     "<num xml:id=\"body_level_2_num\">2.</num>"+
                     "<content xml:id=\"body_level_2_content\">" +
                         "<p xml:id=\"body_level_2_content_p\">Text...</p>" +
                     "</content>" +
                 "</level>" +
                 "</mainBody>" +
               "</doc>" +
             "</akomaNtoso>";
         
         assertThat(new String(result, UTF_8), is(expected));
    }
    
}
