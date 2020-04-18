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

import eu.europa.ec.leos.domain.common.TocMode;
import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MandateMessageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.action.SoftActionType;
import eu.europa.ec.leos.model.user.Entity;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.content.ReferenceLabelService;
import eu.europa.ec.leos.services.content.TemplateStructureService;
import eu.europa.ec.leos.services.support.xml.ElementNumberingHelper;
import eu.europa.ec.leos.services.support.xml.MandateNumberingProcessor;
import eu.europa.ec.leos.services.support.xml.NumberProcessor;
import eu.europa.ec.leos.services.support.xml.ReferenceLabelServiceImplForMandate;
import eu.europa.ec.leos.services.support.xml.VtdXmlContentProcessorForMandate;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper;
import eu.europa.ec.leos.services.support.xml.ref.LabelArticleElementsOnly;
import eu.europa.ec.leos.services.support.xml.ref.LabelArticlesOrRecitalsOnly;
import eu.europa.ec.leos.services.support.xml.ref.LabelCitationsOnly;
import eu.europa.ec.leos.services.support.xml.ref.LabelHandler;
import eu.europa.ec.leos.services.support.xml.ref.LabelHigherOrderElementsOnly;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.services.toc.StructureServiceImpl;
import eu.europa.ec.leos.services.util.TestUtils;
import eu.europa.ec.leos.services.validation.handlers.AkomantosoXsdValidator;
import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.vo.toc.NumberingConfig;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
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

import javax.inject.Provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
public class VtdXmlContentProcessorForMandateTest_IT extends LeosTest {
	@Mock
	SecurityContext securityContext;
	@Mock
	Authentication authentication;
	@Mock
	UserDetails userDetails;

	private AkomantosoXsdValidator akomantosoXsdValidator = new AkomantosoXsdValidator();

 @Mock
 private Provider<StructureContext> structureContextProvider;

 @Mock
 private StructureContext structureContext;

	@InjectMocks
	private ElementNumberingHelper elementNumberingHelper = new ElementNumberingHelper(getMessageHelper(), structureContextProvider);

	@InjectMocks
	private VtdXmlContentProcessorForMandate vtdXmlContentProcessor = Mockito.spy(new VtdXmlContentProcessorForMandate());
	@InjectMocks
	private XmlTableOfContentHelper xmlTableOfContentHelper = Mockito.spy(new XmlTableOfContentHelper());
	@InjectMocks
	private NumberProcessor numberingProcessor = new MandateNumberingProcessor(elementNumberingHelper);
	@InjectMocks
	private XmlContentProcessor xmlContentProcessor = Mockito.spy(new VtdXmlContentProcessorForMandate());

    @InjectMocks
    private StructureServiceImpl structureServiceImpl;
    
    @Mock
	private TemplateStructureService templateStructureService;

	@Mock
	private LanguageHelper languageHelper;
	@InjectMocks
	private MessageHelper messageHelper = Mockito.spy(getMessageHelper());
	private MessageHelper getMessageHelper() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("test-servicesContext.xml");
		MessageSource servicesMessageSource = (MessageSource) applicationContext.getBean("servicesMessageSource");
		MessageHelper messageHelper = new MandateMessageHelper(servicesMessageSource);
		return messageHelper;
	}
	@InjectMocks
	private List<LabelHandler> labelHandlers = Mockito.spy(Stream.of(new LabelArticleElementsOnly(),
																	new LabelArticlesOrRecitalsOnly(),
																	new LabelCitationsOnly(),
																	new LabelHigherOrderElementsOnly())
															.collect(Collectors.toList()));
	
	@InjectMocks
	private ReferenceLabelService referenceLabelService = Mockito.spy(new ReferenceLabelServiceImplForMandate());

	private final static String PREFIX_DELETE = "/integration/mandate/delete/";
	private final static String PREFIX_UNDELETE = "/integration/mandate/undelete/";
    private final static String PREFIX_MOVE = "/integration/mandate/move/";
	private final static String PREFIX_ADD = "/integration/mandate/add/";

	private String docTemplate;
	private List<NumberingConfig> numberingConfigs;

	@Before
	public void onSetUp() throws Exception {
		when(languageHelper.getCurrentLocale()).thenReturn(new Locale("en"));
		when(userDetails.getUsername()).thenReturn(getTestUser().getLogin());
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);

		Mockito.doReturn("1900-01-01T00:00:00.000+01:00").when(vtdXmlContentProcessor).getXMLFormatDate();

		ReflectionTestUtils.setField(akomantosoXsdValidator, "SCHEMA_PATH", "eu/europa/ec/leos/xsd");
		ReflectionTestUtils.setField(akomantosoXsdValidator, "SCHEMA_NAME", "akomantoso30.xsd");
		akomantosoXsdValidator.initXSD();
		
        docTemplate = "BL-023";
        byte[] bytesFile = TestUtils.getFileContent("/structure-test.xml");
        when(templateStructureService.getStructure(docTemplate)).thenReturn(bytesFile);
        ReflectionTestUtils.setField(structureServiceImpl, "structureSchema", "toc/schema/structure_1.xsd");
		numberingConfigs = structureServiceImpl.getNumberingConfigs(docTemplate);

  when(structureContextProvider.get()).thenReturn(structureContext);
  when(structureContext.getNumberingConfigs()).thenReturn(numberingConfigs);
	}

	private User getTestUser() {
		String user1FirstName = "jane";
		String user1LastName = "demo";
		String user1Login = "jane";
		String user1Mail = "jane@test.com";
		List<Entity> entities = new ArrayList<Entity>();
		entities.add(new Entity("1", "EXT.A1", "Ext"));
		List<String> roles = new ArrayList<String>();
		roles.add("ADMIN");

		User user1 = new User(1l, user1Login, user1LastName + " " + user1FirstName, entities, user1Mail, roles);
		return user1;
	}

	private List<TableOfContentItemVO> buildTocFromSerializedList(String prefix, String fileNameWithSerializedToc) throws IOException, ClassNotFoundException {
		File f = new File("src\\test\\resources\\"+prefix + fileNameWithSerializedToc);
		FileInputStream fis = new FileInputStream(f);
		ObjectInputStream ois = new ObjectInputStream(fis);
		List<TableOfContentItemVO> toc = (List<TableOfContentItemVO>) ois.readObject();
		return toc;
	}

	/**
	 * Input:
	 * Article 1
	 * 		1. Paragraph
	 * 			subparagraph
	 * 			list
	 * 				(-a) point	(softmove_label="MOVED from point (b)", softaction="move_from", softmove_from, softactionroot, softdate, softuser)
	 * 				(a) point
	 * 				(b) point	(softmove_label="MOVED to point (-a)", softaction="move_to", softmove_from, softactionroot, softdate, softuser)
	 */
	@Test
	public void test_move__Art1PointB_beforePointA_SameList() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_MOVE, "bill_3Articles__move_Art1Par1PointB_beforePointA_sameList_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_MOVE, "bill_3Articles__move_Art1Par1PointB_beforePointA_sameList_toc.ser");

		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	/**
	 * Output:
	 * Article 1
	 * 		1. Paragraph
	 * 			subparagraph
	 * 			list
	 * 				(-a) point (added to all structure: softmove_label="MOVED from Article 3(1), point (a)", softaction="move_from", softactionroot="true", softmove_from, softdate, softuser)
	 * 	 				alinea
	 * 	  				list
	 * 	 					(1) point
	 * 	  					(1) point
	 * 	  			(a) point
	 * 				(b) point
	 * Article 2
	 * 	 	Paragraph
	 * Article 3
	 * 	 	1. Paragraph
	 * 	 		subparagraph
	 * 			list
	 * 	 			(a) point
	 * 	 				alinea
	 * 	 				list  (added to all structure: softmove_label="MOVED to Article 1(1), point (-a)", softaction="move_from", softactionroot="true", softmove_to , softdate, softuser)
	 * 	 					(1) point
	 * 	 					(2) point
	 * 	 			(b) point
	 */
	@Test
	public void test_move__Art3Par1PointAAndChilds_beforeArt1Par1PointA() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_MOVE, "bill_3Articles__move_Art3Par1PointAAndChilds_beforeArt1Par1PointA_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_MOVE, "bill_3Articles__move_Art3Par1PointAAndChilds_beforeArt1Par1PointA_toc.ser");

		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	/**
	 * Output:
	 * Article 1
	 * 		1. Paragraph
	 * 			subparagraph
	 * 			list
	 * 	  			(a) point
	 * 				(b) point
	 * Article 2
	 * 	 	Paragraph
	 * Article 3
	 * 	 	1. Paragraph
	 * 	 		subparagraph
	 * 			list
	 * 	 			(a) point
	 * 	 				alinea
	 * 	 				list
	 * 	 					(1) point
	 * 	 					(2) point
	 * 	 			(b) point
	 */
	@Test
	public void test_move__Art3Par1PointAAndChilds_beforeArt1Par1PointA_thenBackOriginalPosition1() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent(PREFIX_MOVE, "bill_3Articles__move_Art3Par1PointAAndChilds_beforeArt1Par1PointA_output.xml");
		final byte[] xmlExpected = TestUtils.getFileContent("/integration/", "bill_3Articles_afterRestored.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_MOVE, "bill_3Articles__move_Art3Par1PointAAndChilds_beforeArt1Par1PointA_thenBackOriginalPosition1.ser");

		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	/**
	 * Output:
	 * Article 1
	 * 		1. Paragraph
	 * 			subparagraph
	 * 			list
	 * 	  			(a) point
	 * 				(b) point
	 * Article 2
	 * 	 	Paragraph
	 * 	 		subparagraph
	 * 	 		list
	 * 				(a) point	(softmove_label="MOVED from Article 3(1), point (a)", 					softaction="move_from", softactionroot="true", 	softmove_from, softdate, softuser)
	 * 	 				alinea	(softmove_label="MOVED from Article 3(1), point (a), first sub-point", 	softaction="move_from", softactionroot="false", softmove_from, softdate, softuser)
	 * 	  				list	(softmove_label="MOVED from Article 3(1), point (a)", 					softaction="move_from", softactionroot="false", softmove_from, softdate, softuser)
	 * 	 					(1) point	(softmove_label="MOVED from Article 3(1), point (a)(1)", 		softaction="move_from", softactionroot="false", softmove_from, softdate, softuser)
	 * 	  					(2) point	(softmove_label="MOVED from Article 3(1), point (a)(2)", 		softaction="move_from", softactionroot="false", softmove_from, softdate, softuser)
	 * 	 			(b) point
	 * Article 3
	 * 	 	1. Paragraph
	 * 	 		subparagraph
	 * 			list
	 * 	 			(a) point	(softmove_label="MOVED to Article 2, first paragraph, point (-a)", 					softaction="move_to", softactionroot="true",  softmove_to, softdate, softuser)
	 * 	 				alinea	(softmove_label="MOVED to Article 2, first paragraph, point (-a), first sub-point", softaction="move_to", softactionroot="false", softmove_to, softdate, softuser)
	 * 	 				list	(softmove_label="MOVED to Article 2, first paragraph, point (-a)", 					softaction="move_to", softactionroot="false", softmove_to, softdate, softuser)
	 * 	 					(1) point	(softmove_label="MOVED to Article 2, first paragraph, point (-a)(1)", 		softaction="move_to", softactionroot="false", softmove_to, softdate, softuser)
	 * 	 					(2) point	(softmove_label="MOVED to Article 2, first paragraph, point (-a)(2)", 		softaction="move_to", softactionroot="false", softmove_to, softdate, softuser)
	 * 	 			(b) point
	 */
	@Test
	public void test_move__Art3Par1PointAAndChilds_beforeArt1Par1PointA_thenAgainBeforeArt2Par1PointA() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent(PREFIX_MOVE, "bill_3Articles__move_Art3Par1PointAAndChilds_beforeArt1Par1PointA_output.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_MOVE, "bill_3Articles__move_Art3Par1PointAAndChilds_beforeArt1Par1PointA_thenAgainBeforeArt2Par1PointA_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_MOVE, "bill_3Articles__move_Art3Par1PointAAndChilds_beforeArt1Par1PointA_thenAgainBeforeArt2Par1PointA_toc.ser");

		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	/**
	 * Output:
	 * Article 1
	 * 		1. Paragraph
	 * 			subparagraph
	 * 			list
	 * 	  			(a) point	(softmove_label="MOVED from Article 3(1), point (a)", 					softaction="move_from", softactionroot="true", 	softmove_from, softdate, softuser)
	 * 	  				alinea	(softmove_label="MOVED from Article 3(1), point (a), first sub-point",  softaction="move_from", softactionroot="false", softmove_from, softdate, softuser)
	 * 	  				list	(softmove_label="MOVED from Article 3(1), point (a)", 					softaction="move_from", softactionroot="false", softmove_from, softdate, softuser)
	 * 	  					(1) point	(softmove_label="MOVED from Article 3(1), point (a)(2)", 		softaction="move_from", softactionroot="false", softmove_from, softdate, softuser)
	 * 				(b) point
	 * Article 2
	 * 	 	Paragraph
	 * 	 		subparagraph
	 * 	 		list
	 * 				(-a) point	(softmove_label="MOVED from Article 3(1), point (a)(1)", 				softaction="move_from", softactionroot="true", 	softmove_from, softdate, softuser)
	 * 				(a) point
	 * 	 			(b) point
	 * Article 3
	 * 	 	1. Paragraph
	 * 	 		subparagraph
	 * 			list
	 * 	 			(a) point	(softmove_label="MOVED to Article 1(1), point (-a)", 					softaction="move_to", softactionroot="true",  softmove_to, softdate, softuser)
	 * 	 				alinea	(softmove_label="MOVED to Article 1(1), point (-a), first sub-point",	softaction="move_to", softactionroot="false", softmove_to, softdate, softuser)
	 * 	 				list	(softmove_label="MOVED to Article 1(1), point (-a)", 					softaction="move_to", softactionroot="false", softmove_to, softdate, softuser)
	 * 	 					(1) point	(softmove_label="MOVED to Article 2, first paragraph, point (-a)", 		softaction="move_to", softactionroot="true", softmove_to, softdate, softuser)
	 * 	 					(2) point	(softmove_label="MOVED to Article 1(1), point (-a)(1)", 		softaction="move_to", softactionroot="false", softmove_to, softdate, softuser)
	 * 	 			(b) point
	 */
	@Test
	public void test_move__Art3Par1PointAAndChilds_beforeArt1Par1PointA_thenArt1Par1PointAPoint1_beforeArt2Par1PointA() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent(PREFIX_MOVE, "bill_3Articles__move_Art3Par1PointAAndChilds_beforeArt1Par1PointA_output.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_MOVE, "bill_3Articles__move_Art3Par1PointAAndChilds_beforeArt1Par1PointA_thenArt1Par1PointAPoint1_beforeArt2Par1PointA_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_MOVE, "bill_3Articles__move_Art3Par1PointAAndChilds_beforeArt1Par1PointA_thenArt1Par1PointAPoint1_beforeArt2Par1PointA_toc.ser");

		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	/**
	 * OUTPUT:
	 * Article 1
	 * 	 	-1. Paragraph	(added to all structure: softmove_label="MOVED from Article 3(1)", softaction="move_from", softmove_from, softactionroot, softdate, softuser)
	 * 	 		subparagraph
	 * 			list
	 * 	 			(a)	point
	 * 	 				alinea
	 * 	 				list
	 * 	 					(1) point
	 * 	 					(2) point (2)
	 * 	 			(b) point
	 * 	 			(b) point
	 * 		1. Paragraph
	 * 			subparagraph
	 * 			list
	 * 				(a) point
	 * 				(b) point
	 * Article 2
	 * 		Paragraph
	 * 	  		subparagraph
	 *  		list
	 *  			(a) point
	 *  			(b) point
	 * Article 3
	 * 	 	1. Paragraph	(strike on 1) (added to all structure: softmove_label="MOVED to Article 1(-1)", softaction="move_to", softmove_to, softactionroot, softdate, softuser)
	 * 	 		subparagraph
	 * 			list
	 * 	 			(a) point
	 * 	 				alinea
	 * 	 				list
	 * 	 					(1) point
	 * 	 					(2) point
	 * 	 			(b) point
	 */
	@Test
	public void test_move__Art3Par1AndChilds_beforeArt1Par1() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_MOVE, "bill_3Articles__move_Art3Par1AndChilds_beforeArt1Par1_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_MOVE, "bill_3Articles__move_Art3Par1AndChilds_beforeArt1Par1_toc.ser");

		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	/**
	 * Input: any bill.xml
	 * Output: same structure should be present after processing since the toc didn't change
	 */
	@Test
	public void test_saveNotChangedToc() throws IOException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent(PREFIX_UNDELETE, "bill_softdeleted_Paragraph_noPoints.xml");
		final List<TableOfContentItemVO> toc = xmlTableOfContentHelper.buildTableOfContent("bill", xmlInput, TocMode.NOT_SIMPLIFIED);

		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlInput));
		assertThat(result, is(expected));
	}

	@Test
	public void test_delete___Art1Par1() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_DELETE, "bill_3Articles__deleteArt1Par1_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_3Articles__deleteArt1Par1_toc.ser");

		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	@Test
	public void test_delete___Article1() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_DELETE, "bill_3Articles__deleteArt1_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_3Articles__deleteArt1_toc.ser");

		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
	}

	@Test
	public void test_delete___Art1Par1PointA() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_DELETE, "bill_3Articles__deleteArt1Par1PointA_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_3Articles__deleteArt1Par1PointA_toc.ser");

		// When
		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	@Test
	public void test_delete___movedArt3pointA_beforeArt1PointA__deleteArticle3() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles_movedArt3PointA_beforeArt1PointA.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_DELETE, "bill_3Articles_movedArt3PointA_beforeArt1PointA__deleteArt3_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_3Articles_movedArt3PointA_beforeArt1PointA__deleteArt3_toc.ser");

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	@Test
	public void test_delete__movedArt3pointA_beforeArt1PointA_deleteArticle1() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles_movedArt3PointA_beforeArt1PointA.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_DELETE, "bill_3Articles_movedArt3PointA_beforeArt1PointA__deleteArt1_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_3Articles_movedArt3PointA_beforeArt1PointA__deleteArt1_toc.ser");

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

 		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}
	/**
	 * Input:
	 * Article 1
	 * 		1. Paragraph
	 * 			subparagraph
	 * 			list
	 * 				(a) point
	 * 				(b) point
	 * 			subparagraph (CN)
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
	@Test
	public void test_delete___ECSub_CNSub__deleteCNSub__shouldRemainOnlyECSub() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent(PREFIX_DELETE, "bill_3Articles__ECSubCNSub__deleteCNSub.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_DELETE, "bill_3Articles__ECSubCNSub__deleteCNSub_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_3Articles__ECSubCNSub__deleteCNSub_toc.ser");

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	@Test
	public void test_delete___CNArt1Par1PointA__deletePointA__shouldRemoveListAndSub() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent(PREFIX_DELETE, "bill_3Articles__CNArt1Par1PointA__deletePointA.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_DELETE, "bill_3Articles__CNArt1Par1PointA__deletePointA_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_3Articles__CNArt1Par1PointA__deletePointA_toc.ser");

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	@Test
	public void test_delete___CNArt1Par1PointAPoint1__deletePoint1__shouldRemoveListAndAlinea() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent(PREFIX_DELETE, "bill_3Articles__CNArt1Par1PointAPoint1__deletePoint1.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_DELETE, "bill_3Articles__CNArt1Par1PointAPoint1__deletePoint1_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_DELETE, "bill_3Articles__CNArt1Par1PointAPoint1__deletePoint1_toc.ser");

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}


	/**
	 * Input:
	 * Article 1
	 * 		- Paragraph 1 (editable,deletable="false";softaction="del";softactionroot="true";softuser;softdate;)
	 *
	 * 	Output: Suffix "deleted_" removed from the undeleted elements. New structure:
	 * 	Article 1
	 * 	 	- Paragraph 1
	 */
	@Test
	public void test_undeleteSoftDeleted_Paragraph_noPoints() throws IOException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent(PREFIX_UNDELETE,"bill_softdeleted_Paragraph_noPoints.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_UNDELETE,"bill_softdeleted_Paragraph_noPoints_output.xml");

		final List<TableOfContentItemVO> toc = xmlTableOfContentHelper.buildTableOfContent("bill", xmlInput, TocMode.NOT_SIMPLIFIED);
        final TableOfContentItemVO art1 = toc.get(0).getChildItems().get(0);
		setAsUndeleted(art1.getChildItems().get(0));

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
	}

	/**
	 * Input:
	 * Article 1
	 * 		- Paragraph 1 		(editable,deletable="false";softaction="del";softactionroot="true";softuser;softdate;)
	 * 			- subparagraph 	(editable,deletable="false";softaction="del";softactionroot="false";softuser;softdate;)
	 * 			- list 			(editable,deletable="false";softaction="del";softactionroot="false";softuser;softdate;)
	 * 				- point		(editable,deletable="false";softaction="del";softactionroot="false";softuser;softdate;)
	 *
	 * 	Output: Suffix "deleted_" removed from the undeleted elements. New structure:
	 * 	Article 1
	 * 	 	- Paragraph 1
	 * 	 		- subparagraph
	 * 	 		- list
	 * 	 			- point
	 */
	@Test
	public void test_undeleteSoftDeleted_Paragraph_WithPoints() throws IOException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent(PREFIX_UNDELETE,"bill_softdeleted_Paragraph_withPoints.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_UNDELETE,"bill_softdeleted_Paragraph_withPoints_output.xml");

		final List<TableOfContentItemVO> toc = xmlTableOfContentHelper.buildTableOfContent("bill", xmlInput, TocMode.NOT_SIMPLIFIED);
		final TableOfContentItemVO art1_par1 = toc.get(0).getChildItems().get(0).getChildItems().get(0);
		setAsUndeleted(art1_par1);

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
	}

	/**
	 * Input:
	 * Article 1	 			(editable,deletable="false";softaction="del";softactionroot="true";softuser;softdate;)
	 * 		- Paragraph 1 		(editable,deletable="false";softaction="del";softactionroot="false";softuser;softdate;)
	 * 			- subparagraph 	(editable,deletable="false";softaction="del";softactionroot="false";softuser;softdate;)
	 * 			- list 			(editable,deletable="false";softaction="del";softactionroot="false";softuser;softdate;)
	 * 				- point		(editable,deletable="false";softaction="del";softactionroot="false";softuser;softdate;)
	 *
	 * Output: Suffix "deleted_" removed from the undeleted elements. New structure:
	 * Article 1				(editable,deletable="false")
	 * 		- Paragraph 1
	 * 			- subparagraph
	 * 			- list
	 * 				- point
	 */
	@Test
	public void test_undeleteSoftDeleted_FullArticle() throws IOException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent(PREFIX_UNDELETE,"bill_softdeleted_fullArticle.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_UNDELETE,"bill_softdeleted_fullArticle_output.xml");

		final List<TableOfContentItemVO> toc = xmlTableOfContentHelper.buildTableOfContent("bill", xmlInput, TocMode.NOT_SIMPLIFIED);
		final TableOfContentItemVO art1 = toc.get(0).getChildItems().get(0);
		setAsUndeleted(art1);

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
	}

	/**
	 * Input:
	 * Article 1
	 * 		- Paragraph 1 		(editable,deletable="false";softaction="del";softactionroot="true";softuser;softdate;)
	 * 			- subparagraph 	(editable,deletable="false";softaction="del";softactionroot="false";softuser;softdate;)
	 * 			- list 			(editable,deletable="false";softaction="del";softactionroot="false";softuser;softdate;)
	 * 				- point		(editable,deletable="false";softaction="del";softactionroot="false";softuser;softdate;)
	 * 				- point		(MOVED_TO)
	 *
	 * Output: Suffix "deleted_" removed from the undeleted elements. New structure:
	 * Article 1
	 * 		- Paragraph 1
	 * 			- subparagraph
	 * 			- list
	 * 				- point
	 * 				- point		(not changed, still MOVED_TO and all appropriate soft attributes)
	 */
	@Test
	public void test_undeleteSoftDeleted_withMovedList() throws IOException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent(PREFIX_UNDELETE,"bill_softdeleted_Article_withSoftMovedPoint.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_UNDELETE,"bill_softdeleted_Article_withSoftMovedPoint_output.xml");

		final List<TableOfContentItemVO> toc = xmlTableOfContentHelper.buildTableOfContent("bill", xmlInput, TocMode.NOT_SIMPLIFIED);
		final TableOfContentItemVO art1 = toc.get(0).getChildItems().get(0);
		setAsUndeleted(art1);

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreSoftDate(new String(xmlResult));
		String expected = ignoreSoftDate(new String(xmlExpected));
		assertThat(result, is(expected));
	}

	@Test
	public void test_add__citation() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_ADD, "bill_add__citation_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__citation_toc.ser");

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	@Test
	public void test_add__recital() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_ADD, "bill_add__recital_output.xml");
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
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_ADD, "bill_add__part_output.xml");
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
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_ADD, "bill_add__title_output.xml");
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
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_ADD, "bill_add__chapter_output.xml");
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
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_ADD, "bill_add__section_output.xml");
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
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_ADD, "bill_add__article_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__article_toc.ser");

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	@Test
	public void test_add__paragraph() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_ADD, "bill_add__paragraph_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__paragraph_toc.ser");

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	@Test
	public void test_add__subparagraph() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_ADD, "bill_add__subparagraph_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__subparagraph_toc.ser");

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	@Test
	public void test_add__point_over_point_shouldCreateAlinea() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_ADD, "bill_add__point_overPoint_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__point_overPoint_toc.ser");

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}


	/**
	 * Input:
	 * Article 1
	 * 		1. Paragraph
	 * 			subparagraph
	 * 			list
	 * 				(a) point
	 * 				(b) point
	 * Article 1a (CN)
	 * 		1. Paragraph
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
	@Test
	public void test_add__point_over_paragraph_shouldCreateSub() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent(PREFIX_ADD, "bill_add__point_overParagraph_input.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_ADD, "bill_add__point_overParagraph_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__point_overParagraph_toc.ser");

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	@Test
	public void test_add__article_FullStructureWith3LevelPoints() throws IOException, ClassNotFoundException {
		// Given
		final byte[] xmlInput = TestUtils.getFileContent("/integration/", "bill_3Articles.xml");
		final byte[] xmlExpected = TestUtils.getFileContent(PREFIX_ADD, "bill_add__articleFullStructureWith3LevelPoints_output.xml");
		final List<TableOfContentItemVO> toc = buildTocFromSerializedList(PREFIX_ADD, "bill_add__articleFullStructureWith3LevelPoints_toc.ser");

		// When
 		byte[] xmlResult = processSaveToc(xmlInput, toc);

		// Then
		String result = ignoreXmlId(ignoreSoftDate(new String(xmlResult)));
		String expected = ignoreXmlId(ignoreSoftDate(new String(xmlExpected)));
		assertThat(result, is(expected));
		assertTrue(akomantosoXsdValidator.validate(xmlResult));
	}

	private void setAsUndeleted(TableOfContentItemVO toc) {
		if (SoftActionType.DELETE.equals(toc.getSoftActionAttr())) {
			toc.setSoftActionAttr(null);
			toc.setSoftActionRoot(null);
			toc.setSoftDateAttr(null);
			toc.setSoftMoveTo(null);
			toc.setUndeleted(true);
		}

		toc.getChildItems().forEach(child -> setAsUndeleted(child));
	}

	/**
	 * Once we remove Kotlin this block will be replaced by testing directly billService.saveTableOfContent()
	 */
	private byte[] processSaveToc(byte[] xmlInput, List<TableOfContentItemVO> toc) {
		byte[] xmlResult = vtdXmlContentProcessor.createDocumentContentWithNewTocList(toc, xmlInput, getTestUser());
		xmlResult = numberingProcessor.renumberArticles(xmlResult);
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
