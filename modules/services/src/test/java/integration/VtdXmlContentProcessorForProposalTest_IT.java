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
package integration;

import eu.europa.ec.leos.i18n.ProposalMessageHelper;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.support.xml.ElementNumberingHelper;
import eu.europa.ec.leos.services.support.xml.NumberProcessor;
import eu.europa.ec.leos.services.support.xml.ProposalNumberingProcessor;
import eu.europa.ec.leos.services.support.xml.ReferenceLabelProcessor;
import eu.europa.ec.leos.services.support.xml.ReferenceLabelServiceImplForProposal;
import eu.europa.ec.leos.services.support.xml.VtdXmlContentProcessorForProposal;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.ref.LabelArticleElementsOnly;
import eu.europa.ec.leos.services.support.xml.ref.LabelArticlesOrRecitalsOnly;
import eu.europa.ec.leos.services.support.xml.ref.LabelCitationsOnly;
import eu.europa.ec.leos.services.support.xml.ref.LabelHandler;
import eu.europa.ec.leos.services.support.xml.ref.LabelHigherOrderElementsOnly;
import eu.europa.ec.leos.services.validation.handlers.AkomantosoXsdValidator;
import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.LegalTextProposalTocItemType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Input:
 * Article 1
 * 		1. Paragraph
 * 			subparagraph
 * 			list
 * 				(a) point
 * 				(b) point
 * Article 2
 * 	 	Paragraph
 * 	  		subparagraph
 *  		list
 *  			(a) point
 * Article 3
 * 	 	1. Paragraph
 * 	 		subparagraph
 * 			list
 * 	 			(a) point
 * 	 				alinea
 * 	 				list
 * 	 					(1) point
 * 	 					(2) point
 * 	 			(b)	point
 */
public class VtdXmlContentProcessorForProposalTest_IT extends LeosTest {
	@Mock
	SecurityContext securityContext;
	@Mock
	Authentication authentication;
	@Mock
	UserDetails userDetails;
	
	private AkomantosoXsdValidator akomantosoXsdValidator = new AkomantosoXsdValidator();

	@InjectMocks
	private VtdXmlContentProcessorForProposal vtdXmlContentProcessor = Mockito.spy(new VtdXmlContentProcessorForProposal());
	@InjectMocks
	private NumberProcessor numberingProcessor = new ProposalNumberingProcessor();
	@InjectMocks
	private XmlContentProcessor xmlContentProcessor = Mockito.spy(new VtdXmlContentProcessorForProposal());
	
	//All following InjectMocks are used inside numberingProcessor and xmlContentProcessor beans
	@Mock
	private LanguageHelper languageHelper;
	@InjectMocks
	private MessageHelper messageHelper = Mockito.spy(getMessageHelper());
	private MessageHelper getMessageHelper() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("test-servicesContext.xml");
		MessageSource servicesMessageSource = (MessageSource) applicationContext.getBean("servicesMessageSource");
		MessageHelper messageHelper = new ProposalMessageHelper(servicesMessageSource);
		return messageHelper;
	}
	@InjectMocks
	private List<LabelHandler> labelHandlers = Mockito.spy(Stream.of(new LabelArticleElementsOnly(),
																	new LabelArticlesOrRecitalsOnly(),
																	new LabelCitationsOnly(),
																	new LabelHigherOrderElementsOnly())
															.collect(Collectors.toList()));
	@InjectMocks
	private ElementNumberingHelper elementNumberingHelper = Mockito.spy(new ElementNumberingHelper());
	@InjectMocks
	private ReferenceLabelProcessor referenceLabelProcessor = Mockito.spy(new ReferenceLabelServiceImplForProposal());
	
	private final static String PREFIX_DELETE = "/integration/proposal/delete/";
    private final static String PREFIX_MOVE = "/integration/proposal/move/";
	private final static String PREFIX_ADD = "/integration/proposal/add/";
	
	@Before
	public void onSetUp() throws Exception {
		when(userDetails.getUsername()).thenReturn(getTestUser().getLogin());
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		
		//Mockito.doReturn("1900-01-01T00:00:00.000+01:00").when(vtdXmlContentProcessor).getXMLFormatDate();
		
		ReflectionTestUtils.setField(akomantosoXsdValidator, "SCHEMA_PATH", "eu/europa/ec/leos/xsd");
		ReflectionTestUtils.setField(akomantosoXsdValidator, "SCHEMA_NAME", "akomantoso30.xsd");
		akomantosoXsdValidator.initXSD();
	}
	
	private User getTestUser() {
		String user1FirstName = "jane";
		String user1LastName = "demo";
		String user1Login = "jane";
		String user1Mail = "jane@test.com";
		String entity = "Entity";
		List<String> roles = new ArrayList<String>();
		roles.add("ADMIN");
		
		User user1 = new User(1l, user1Login, user1LastName + " " + user1FirstName, entity, user1Mail, roles);
		return user1;
	}
	
	private byte[] getFileContent(String prefix, String fileName) throws IOException {
		InputStream inputStream = this.getClass().getResource(prefix + fileName).openStream();
		byte[] content = new byte[inputStream.available()];
		inputStream.read(content);
		inputStream.close();
		return content;
	}
	
	private List<TableOfContentItemVO> buildTocFromSerializedList(String prefix, String fileNameWithSerializedToc) throws IOException, ClassNotFoundException {
		File f = new File("src\\test\\resources\\"+prefix + fileNameWithSerializedToc);
		FileInputStream fis = new FileInputStream(f);
		ObjectInputStream ois = new ObjectInputStream(fis);
		List<TableOfContentItemVO> toc = (List<TableOfContentItemVO>) ois.readObject();
		return toc;
	}
	
	/** In all add tests, add new element after the first one*/
	
	@Test
	public void test_add__citation() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_ADD, "bill_add__citation_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__citation_toc.ser");
	
		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);
 		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		
		System.out.println("result:" + result);
		System.out.println("expected:" + expected);
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_add__recital() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_ADD, "bill_add__recital_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__recital_toc.ser");
		
		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);
 		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_add__part() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_ADD, "bill_add__part_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__part_toc.ser");
		
		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);
 		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_add__title() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_ADD, "bill_add__title_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__title_toc.ser");
		
		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);
 		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_add__chapter() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_ADD, "bill_add__chapter_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__chapter_toc.ser");
		
		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);
 		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_add__section() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_ADD, "bill_add__section_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__section_toc.ser");
		
		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);
 		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_add__article() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_ADD, "bill_add__article_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__article_toc.ser");
		
		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);
 		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	/**
	 * In all delete tests, delete the second element
	 */
	
	@Test
	public void test_delete__citation() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_DELETE, "bill_delete__citation_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_delete__citation_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		
		System.out.println("result:" + result);
		System.out.println("expected:" + expected);
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_delete__recital() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_DELETE, "bill_delete__recital_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_delete__recital_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_delete__part() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_DELETE, "bill_delete__part_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_delete__part_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_delete__title() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_DELETE, "bill_delete__title_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_delete__title_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_delete__chapter() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_DELETE, "bill_delete__chapter_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_delete__chapter_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_delete__section() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_DELETE, "bill_delete__section_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_delete__section_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	/**
	 * Delete Article 2
	 */
	@Test
	public void test_delete__article() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_DELETE, "bill_delete__article_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_delete__article_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_move__citation() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_MOVE, "bill_move__citation_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_MOVE, "bill_move__citation_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		
		System.out.println("result:" + result);
		System.out.println("expected:" + expected);
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_move__recital() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_MOVE, "bill_move__recital_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_MOVE, "bill_move__recital_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_move__part() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_MOVE, "bill_move__part_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_MOVE, "bill_move__part_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_move__title() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_MOVE, "bill_move__title_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_MOVE, "bill_move__title_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_move__chapter() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_MOVE, "bill_move__chapter_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_MOVE, "bill_move__chapter_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_move__section() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_MOVE, "bill_move__section_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_MOVE, "bill_move__section_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	
	@Test
	public void test_move__article() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = getFileContent("/integration/", "bill_ec_chapter_section_title_3Articles.xml");
		final byte[] xmlExpected = getFileContent(PREFIX_MOVE, "bill_move__article_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_MOVE, "bill_move__article_toc.ser");
		
		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);
		
		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	/**
	 * Once we remove Kotlin this block will be replaced by testing directly billService.saveTableOfContent()
	 */
	private byte[] processSaveToc(byte[] xmlInput, List<TableOfContentItemVO> toc) {
		byte[] xmlResult = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, toc, xmlInput, getTestUser());
		xmlResult = numberingProcessor.renumberArticles(xmlResult, "en");
		xmlResult = numberingProcessor.renumberRecitals(xmlResult);
		xmlResult = xmlContentProcessor.doXMLPostProcessing(xmlResult);
		return xmlResult;
	}
	
	private String ignoreSoftDate(String xmlInput) {
		final String regexSoftDate = "leos:softdate=\"\\d{4}-\\d{2}-\\d{2}.\\d{2}:\\d{2}:\\d{2}[.]\\d{3}[+]\\d{2}:\\d{2}\"";
		final String replacePattern = "leos:softdate=\"\"";
		xmlInput = xmlInput.replaceAll("\\s+", " ")
				.replaceAll("> ", ">")
				.replaceAll(" >", ">")
				.replaceAll("< ", "<")
				.replaceAll(" <", "<")
				.replaceAll(regexSoftDate, replacePattern).trim();
		return xmlInput;
	}
	
	private String ignoreXmlId(String xmlInput) {
		final String regexId = "xml:id=\".+?\"";
		final String replaceIdPattern = "xml:id=\"dummyId\"";
		xmlInput = new String(xmlInput)
				.replaceAll(regexId, replaceIdPattern).trim();
		return xmlInput;
	}
}
