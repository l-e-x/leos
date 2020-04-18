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
package eu.europa.ec.leos.annotate.services;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.Document;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationDocument;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationDocumentLink;
import eu.europa.ec.leos.annotate.repository.DocumentRepository;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateDocumentException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotDeleteDocumentException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
@SuppressWarnings({"PMD.TooManyMethods", "PMD.EmptyCatchBlock"})
public class DocumentServiceTest {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepos;

    private final static String UNEXP_EXC = "Received unexpected exception";
    private final static String MISSING_EXC = "Expected exception for creating invalid document not received";
    
    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * Test that finding an invalid document does not produce a meaningful value 
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    public void testFindNullDocument() {

        final URI uri = null;
        Assert.assertNull(documentService.findDocumentByUri(uri));
    }

    /**
     * Test that finding a document works 
     */
    @Test
    public void testFindExistingDocument() throws URISyntaxException {

        final String url = "http://www.doc.eu";
        final URI uri = new URI(url);

        final Document doc = new Document();
        doc.setUri(url);
        documentRepos.save(doc);

        Assert.assertEquals(doc, documentService.findDocumentByUri(uri));
    }

    /**
     * Test that finding a non-existing document does not work 
     */
    @Test
    public void testDontFindNonExistingDocument() throws URISyntaxException {

        final String url = "http://www.doc2.eu";
        final URI anotherUri = new URI("www.this.is.another_uri.com");

        final Document doc = new Document();
        doc.setUri(url);
        documentRepos.save(doc);

        Assert.assertNull(documentService.findDocumentByUri(anotherUri));
    }

    /**
     * Test creating a document by a given URI 
     */
    @Test
    public void testCreateDocumentByUri() throws URISyntaxException, CannotCreateDocumentException {

        final String url = "http://www.doc3.eu";
        final URI uri = new URI(url);

        final Document doc = documentService.createNewDocument(uri);
        Assert.assertNotNull(doc);
        Assert.assertEquals(uri.toString(), doc.getUri());

        Assert.assertEquals(doc, documentService.findDocumentByUri(uri));
    }

    /**
     * Test creating a document by a given invalid URI fails 
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    public void testCreateDocumentByInvalidUri() throws URISyntaxException {

        final URI uri = null;

        try {
            documentService.createNewDocument(uri);
            Assert.fail(MISSING_EXC);
        } catch (CannotCreateDocumentException ccde) {
            // OK
        } catch (Exception e) {
            Assert.fail(UNEXP_EXC);
        }

        Assert.assertEquals(0, documentRepos.count());
    }

    /**
     * Test that a document is not saved twice for same URI 
     */
    @Test
    public void testDontCreateDocumentsForSameUri() throws URISyntaxException, CannotCreateDocumentException {

        final String url = "http://www.doc4.eu";
        final URI uri = new URI(url);

        final Document firstDoc = documentService.createNewDocument(uri);
        Assert.assertNotNull(firstDoc);

        try {
            // try creating such a document once more!
            documentService.createNewDocument(uri);
            Assert.fail("Expected exception for creating doubled document not received");
        } catch (CannotCreateDocumentException ccde) {
            // OK
        } catch (Exception e) {
            Assert.fail(UNEXP_EXC);
        }

        // make sure document is contained only once
        Assert.assertEquals(1, documentRepos.count());
    }

    /**
     * Test document is not created by a null JSON-serialized object 
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    public void testCreateDocumentByNullJson() {

        final JsonAnnotationDocument jsDoc = null;

        try {
            documentService.createNewDocument(jsDoc);
            Assert.fail(MISSING_EXC);
        } catch (CannotCreateDocumentException ccde) {
            // OK
        } catch (Exception e) {
            Assert.fail(UNEXP_EXC);
        }

        Assert.assertEquals(0, documentRepos.count());
    }

    /**
     * Test document is not created by a JSON-serialized object without URI 
     */
    @Test
    public void testCreateDocumentByJsonWithoutUri() {

        final JsonAnnotationDocument jsDoc = new JsonAnnotationDocument();
        jsDoc.setLink(null);

        try {
            documentService.createNewDocument(jsDoc);
            Assert.fail(MISSING_EXC);
        } catch (CannotCreateDocumentException ccde) {
            // OK
        } catch (Exception e) {
            Assert.fail(UNEXP_EXC);
        }

        Assert.assertEquals(0, documentRepos.count());

        // same again, but now set an empty link list
        final List<JsonAnnotationDocumentLink> links = new ArrayList<JsonAnnotationDocumentLink>();
        jsDoc.setLink(links);

        try {
            documentService.createNewDocument(jsDoc);
            Assert.fail(MISSING_EXC);
        } catch (CannotCreateDocumentException ccde) {
            // OK
        } catch (Exception e) {
            Assert.fail(UNEXP_EXC);
        }

        Assert.assertEquals(0, documentRepos.count());
    }

    /**
     * Test document is created by a JSON-serialized object 
     * @throws CannotCreateDocumentException 
     */
    @Test
    public void testCreateDocumentByJson() throws URISyntaxException, CannotCreateDocumentException {

        final URI uri = new URI("http://www.link.eu");
        final URI anotherUri = new URI("http://this.is.another.uri");

        final JsonAnnotationDocument jsDoc = new JsonAnnotationDocument();
        jsDoc.setLink(null);

        // same again, but now set an empty link list
        final List<JsonAnnotationDocumentLink> links = new ArrayList<JsonAnnotationDocumentLink>();

        // insert two (!) links into the list - only first should be taken into account
        final JsonAnnotationDocumentLink firstLink = new JsonAnnotationDocumentLink();
        firstLink.setHref(uri);
        links.add(firstLink);

        final JsonAnnotationDocumentLink secondLink = new JsonAnnotationDocumentLink();
        secondLink.setHref(anotherUri);
        links.add(secondLink);
        jsDoc.setLink(links);

        // persist the document
        documentService.createNewDocument(jsDoc);

        // was saved with first link
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertNotNull(documentRepos.findByUri(uri.toString())); // contained
        Assert.assertNull(documentRepos.findByUri(anotherUri.toString())); // not contained
    }

    /**
     * Test successful deletion of a document
     */
    @Test
    public void testDeleteDocument() throws URISyntaxException, CannotCreateDocumentException, CannotDeleteDocumentException {

        final String url = "http://www.thedoc.eu";
        final URI uri = new URI(url);

        final Document firstDoc = documentService.createNewDocument(uri);
        Assert.assertNotNull(firstDoc);

        documentService.deleteDocument(firstDoc);

        // make sure document was removed
        Assert.assertEquals(0, documentRepos.count());

        // trying to delete this document once more should not complain
        try {
            documentService.deleteDocument(firstDoc);
        } catch (Exception e) {
            Assert.fail(UNEXP_EXC);
        }
    }

    /**
     * Test deletion of a document being null
     */
    @Test(expected = CannotDeleteDocumentException.class)
    public void testDeleteNullDocument() throws CannotDeleteDocumentException {

        // trying to delete an invalid document should throw exception
        documentService.deleteDocument(null);
    }
}
