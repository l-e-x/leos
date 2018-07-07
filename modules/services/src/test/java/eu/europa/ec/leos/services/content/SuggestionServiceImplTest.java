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
package eu.europa.ec.leos.services.content;

        import eu.europa.ec.leos.model.content.LeosDocument;
        import eu.europa.ec.leos.model.security.SecurityContext;
        import eu.europa.ec.leos.model.user.Department;
        import eu.europa.ec.leos.model.user.User;
        import eu.europa.ec.leos.support.xml.XmlContentProcessor;
        import eu.europa.ec.leos.test.support.LeosTest;
        import eu.europa.ec.leos.test.support.model.ModelHelper;
        import org.hamcrest.CoreMatchers;
        import org.junit.Test;
        import org.mockito.InjectMocks;
        import org.mockito.Mock;

        import java.io.ByteArrayInputStream;
        import java.nio.charset.Charset;
        import java.text.ParseException;

        import static org.hamcrest.Matchers.equalTo;
        import static org.hamcrest.Matchers.is;
        import static org.junit.Assert.assertThat;
        import static org.mockito.Mockito.*;

public class SuggestionServiceImplTest extends LeosTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private ElementService elementService;

    @Mock
    private XmlContentProcessor xmlContentProcessor;

    @Mock
    private SecurityContext leosSecurityContext;

    @InjectMocks
    private SuggestionService suggestionService = new SuggestionServiceImpl();

    @Test
    public void test_getSuggestion_NoSuggestionId() throws Exception {
        // setup
        byte[] baseXmlBytes = new byte[]{1, 2, 3};
        byte[] updatedXmlBytes = new byte[]{5, 6, 8};
        byte[] aggregateXmlBytes = new byte[]{9, 10, 11};
        String originalElementId = "oid_1";
        User user = ModelHelper.buildUser(45L, "login", "name");
        user.setDepartment(new Department(null));
        when(leosSecurityContext.getUser()).thenReturn(user);
        String expectedXML = "<popup id=\"\" refersTo=\"~leosSuggestion\" leos:userid=\"login\" leos:username=\"name\" leos:dg=\"\" leos:datetime=\"\"><p original-id=\"oid_1\">some text <b original-id=\"xy\">bold</b> ends</p></popup>";
        String originalXML = "<p id=\"oid_1\">some text <b id=\"xy\">bold</b> ends</p>";


        LeosDocument document = mock(LeosDocument.class);
        when(document.getContentStream()).thenReturn(new ByteArrayInputStream(baseXmlBytes));
        when(xmlContentProcessor.getElementById(baseXmlBytes,originalElementId)).thenReturn(originalXML);
        when(xmlContentProcessor.removeElements(originalXML.getBytes(Charset.forName("UTF-8")), "//*[@refersTo='~leosSuggestion' or @refersto='~leosSuggestion']"))
                .thenReturn(originalXML.getBytes(Charset.forName("UTF-8")));
        when(xmlContentProcessor.removeElements(originalXML.getBytes(Charset.forName("UTF-8")), "//*[@refersTo='~leosComment' or @refersto='~leosComment']"))
        .thenReturn(originalXML.getBytes(Charset.forName("UTF-8")));

        //Actual Call
        String result = suggestionService.getSuggestion(document, originalElementId, null);

        assertThat(new String(result).replaceAll(" id=(\\s*?)\".+?\"", " id=\"\"").replaceAll("leos:datetime=\".+?\"", "leos:datetime=\"\""),
                CoreMatchers.is(expectedXML));
        verify(xmlContentProcessor).getElementById(baseXmlBytes, originalElementId);
        verify(xmlContentProcessor).removeElements(originalXML.getBytes(Charset.forName("UTF-8")), "//*[@refersTo='~leosSuggestion' or @refersto='~leosSuggestion']");
        verify(xmlContentProcessor).removeElements(originalXML.getBytes(Charset.forName("UTF-8")), "//*[@refersTo='~leosComment' or @refersto='~leosComment']");
        verifyNoMoreInteractions( xmlContentProcessor, elementService, documentService);
    }

    @Test
    public void test_getSuggestion_WithSuggestionId() throws ParseException {
        // setup
        byte[] byteContent = new byte[]{1, 2, 3};
        String suggestionId = "suggestion_1";
        String originalElementId = "oid_1";
        String suggestionXML = "<popup id=\"suggestion_1\"><p id=\"p_id\">some text <b id=\"xy\">bold</b> ends</p></popup>";
        LeosDocument document = mock(LeosDocument.class);
        when(document.getContentStream()).thenReturn(new ByteArrayInputStream(byteContent));
        when(elementService.getElement(document, "popup", suggestionId)).thenReturn(suggestionXML);

        //Actual Call
        String results = suggestionService.getSuggestion(document, originalElementId, suggestionId);

        //verify
        assertThat(results, is(suggestionXML));
        verify(elementService).getElement(document, "popup", suggestionId);
        verifyNoMoreInteractions( xmlContentProcessor, elementService, documentService);
    }

    @Test
    public void test_saveSuggestion_SuggestionDoesNotExist() throws Exception {
        // setup
        byte[] baseXmlBytes = new byte[]{1, 2, 3};
        byte[] updatedXmlBytes = new byte[]{5, 6, 8};
        byte[] expectedXMLBytes = new byte[]{9, 10, 11};
        User user = ModelHelper.buildUser(45L, "login", "name");
        when(leosSecurityContext.getUser()).thenReturn(user);

        String originalElementId = "oid_1";
        String suggestionId = "suggestion_1";
        String newSuggestionXML = "<popup id =\"suggestion_1\"><p original-id=\"oid_1\">new text <b original-=\"xy\">bold</b> ends</p></popup>";
        String originalXML = "<p id=\"p_id\">some text <b id=\"xy\">bold</b> ends</p>";
        String docId = "doc-id";
        LeosDocument document = mock(LeosDocument.class);
        when(document.getLeosId()).thenReturn(docId);
        when(document.getContentStream()).thenReturn(new ByteArrayInputStream(baseXmlBytes));

        LeosDocument updatedDocument = mock(LeosDocument.class);
        when(updatedDocument.getLeosId()).thenReturn(docId);
        when(updatedDocument.getContentStream()).thenReturn(new ByteArrayInputStream(expectedXMLBytes));

        when(elementService.getElement(document, "popup", suggestionId)).thenReturn(null);
        when(xmlContentProcessor.insertCommentInElement(baseXmlBytes, originalElementId, newSuggestionXML,true)).thenReturn(updatedXmlBytes);
        when(documentService.updateDocumentContent(docId, "login", updatedXmlBytes, "operation.suggestion.updated")).thenReturn(updatedDocument);

        //Actual Call
        LeosDocument result = suggestionService.saveSuggestion(document, originalElementId, suggestionId, newSuggestionXML);

        //verify
        assertThat(result, is(updatedDocument));
        verify(elementService).getElement(document, "popup", suggestionId);
        verify(xmlContentProcessor).insertCommentInElement(baseXmlBytes, originalElementId, newSuggestionXML,true);
        verify(documentService).updateDocumentContent(docId, "login", updatedXmlBytes, "operation.suggestion.updated");
        verifyNoMoreInteractions( xmlContentProcessor, elementService, documentService);
    }

    @Test
    public void test_saveSuggestion_SuggestionExists() throws Exception {
        // setup
        byte[] baseXmlBytes = new byte[]{1, 2, 3};
        byte[] updatedXmlBytes = new byte[]{5, 6, 8};
        byte[] expectedXMLBytes = new byte[]{9, 10, 11};
        User user = ModelHelper.buildUser(45L, "login", "name");
        when(leosSecurityContext.getUser()).thenReturn(user);

        String originalElementId = "oid_1";
        String suggestionId = "suggestion_1";
        String oldSuggestionXML = "<popup id =\"suggestion_1\"><p original-id=\"oid_1\">some text <b original-=\"xy\">bold</b> ends</p></popup>";
        String newSuggestionXML = "<popup id =\"suggestion_1\"><p original-id=\"oid_1\">new text <b original-=\"xy\">bold</b> ends</p></popup>";

        String docId = "doc-id";
        LeosDocument document = mock(LeosDocument.class);
        when(document.getLeosId()).thenReturn(docId);
        when(document.getContentStream()).thenReturn(new ByteArrayInputStream(baseXmlBytes));

        LeosDocument updatedDocument = mock(LeosDocument.class);
        when(updatedDocument.getLeosId()).thenReturn(docId);
        when(updatedDocument.getContentStream()).thenReturn(new ByteArrayInputStream(expectedXMLBytes));

        when(elementService.getElement(document, "popup", suggestionId)).thenReturn(oldSuggestionXML);
        when(xmlContentProcessor.replaceElementByTagNameAndId(argThat(equalTo(baseXmlBytes)), argThat(is(newSuggestionXML)), argThat(equalTo("popup")), argThat(equalTo(suggestionId)))).thenReturn(updatedXmlBytes);
        when(documentService.updateDocumentContent(docId, "login", updatedXmlBytes, "operation.suggestion.updated")).thenReturn(updatedDocument);

        //Actual Call
        LeosDocument result = suggestionService.saveSuggestion(document, originalElementId, suggestionId, newSuggestionXML);

        //verify
        assertThat(result, is(updatedDocument));
        verify(elementService).getElement(document, "popup", suggestionId);
        verify(xmlContentProcessor).replaceElementByTagNameAndId(argThat(is(baseXmlBytes)), argThat(is(newSuggestionXML)), argThat(is("popup")),
                argThat(is(suggestionId)));
        verify(documentService).updateDocumentContent(docId, "login", updatedXmlBytes, "operation.suggestion.updated");
        verifyNoMoreInteractions( xmlContentProcessor, elementService, documentService);
    }

    @Test(expected = NullPointerException.class)
    public void test_saveSuggestion_SuggestionIDNotPassed() throws Exception {
        // setup
        byte[] baseXmlBytes = new byte[]{1, 2, 3};
        byte[] updatedXmlBytes = new byte[]{5, 6, 8};
        byte[] expectedXMLBytes = new byte[]{9, 10, 11};
        User user = ModelHelper.buildUser(45L, "login", "name");
        when(leosSecurityContext.getUser()).thenReturn(user);

        String originalElementId = "oid_1";

        String newSuggestionXML = "<popup id =\"suggestion_1\"><p original-id=\"oid_1\">new text <b original-=\"xy\">bold</b> ends</p></popup>";

        String docId = "doc-id";
        LeosDocument document = mock(LeosDocument.class);
        when(document.getLeosId()).thenReturn(docId);
        when(document.getContentStream()).thenReturn(new ByteArrayInputStream(baseXmlBytes));

        LeosDocument updatedDocument = mock(LeosDocument.class);
        when(updatedDocument.getLeosId()).thenReturn(docId);
        when(updatedDocument.getContentStream()).thenReturn(new ByteArrayInputStream(expectedXMLBytes));

        LeosDocument result = suggestionService.saveSuggestion(document, originalElementId, null, newSuggestionXML);
        verifyNoMoreInteractions(suggestionService, xmlContentProcessor, elementService, documentService);
    }

}
