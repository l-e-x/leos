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
package eu.europa.ec.leos.annotate.services;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.Document;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationDocument;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationDocumentLink;
import eu.europa.ec.leos.annotate.repository.DocumentRepository;
import eu.europa.ec.leos.annotate.services.DocumentService;
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
@SpringBootTest
@ActiveProfiles("test")
public class DocumentServiceTest {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepos;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
    }

    @After
    public void cleanDatabaseAfterTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * Test that finding an invalid document does not produce a meaningful value 
     */
    @Test
    public void testFindNullDocument() {

        Assert.assertNull(documentService.findDocumentByUri(null));
    }

    /**
     * Test that finding a document works 
     */
    @Test
    public void testFindExistingDocument() throws URISyntaxException {

        String url = "http://www.doc.eu";
        URI uri = new URI(url);

        Document doc = new Document();
        doc.setUri(url);
        documentRepos.save(doc);

        Assert.assertEquals(doc, documentService.findDocumentByUri(uri));
    }

    /**
     * Test that finding a non-existing document does not work 
     */
    @Test
    public void testDontFindNonExistingDocument() throws URISyntaxException {

        String url = "http://www.doc.eu";
        URI anotherUri = new URI("www.this.is.another_uri.com");

        Document doc = new Document();
        doc.setUri(url);
        documentRepos.save(doc);

        Assert.assertNull(documentService.findDocumentByUri(anotherUri));
    }

    /**
     * Test creating a document by a given URI 
     */
    @Test
    public void testCreateDocumentByUri() throws URISyntaxException, CannotCreateDocumentException {

        String url = "http://www.doc.eu";
        URI uri = new URI(url);

        Document doc = documentService.createNewDocument(uri);
        Assert.assertNotNull(doc);
        Assert.assertEquals(uri.toString(), doc.getUri());

        Assert.assertEquals(doc, documentService.findDocumentByUri(uri));
    }

    /**
     * Test creating a document by a given invalid URI fails 
     */
    @Test
    @SuppressFBWarnings(value = "NP_LOAD_OF_KNOWN_NULL_VALUE", justification = "Intended; had to be done this way to call the correct function requiring a parameter of given type")
    public void testCreateDocumentByInvalidUri() throws URISyntaxException {

        URI uri = null;

        try {
            documentService.createNewDocument(uri);
            Assert.fail("Expected exception for creating invalid document not received");
        } catch (CannotCreateDocumentException ccde) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception");
        }

        Assert.assertEquals(0, documentRepos.count());
    }

    /**
     * Test that a document is not saved twice for same URI 
     */
    @Test
    public void testDontCreateDocumentsForSameUri() throws URISyntaxException, CannotCreateDocumentException {

        String url = "http://www.doc.eu";
        URI uri = new URI(url);

        Document firstDoc = documentService.createNewDocument(uri);
        Assert.assertNotNull(firstDoc);

        try {
            // try creating such a document once more!
            documentService.createNewDocument(uri);
            Assert.fail("Expected exception for creating doubled document not received");
        } catch (CannotCreateDocumentException ccde) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception");
        }

        // make sure document is contained only once
        Assert.assertEquals(1, documentRepos.count());
    }

    /**
     * Test document is not created by a null JSON-serialized object 
     */
    @Test
    @SuppressFBWarnings(value = "NP_LOAD_OF_KNOWN_NULL_VALUE", justification = "Intended; had to be done this way to call the correct function requiring a parameter of given type")
    public void testCreateDocumentByNullJson() {

        JsonAnnotationDocument jsDoc = null;

        try {
            documentService.createNewDocument(jsDoc);
            Assert.fail("Expected exception for creating invalid document not received");
        } catch (CannotCreateDocumentException ccde) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception");
        }

        Assert.assertEquals(0, documentRepos.count());
    }

    /**
     * Test document is not created by a JSON-serialized object without URI 
     */
    @Test
    public void testCreateDocumentByJsonWithoutUri() {

        JsonAnnotationDocument jsDoc = new JsonAnnotationDocument();
        jsDoc.setLink(null);

        try {
            documentService.createNewDocument(jsDoc);
            Assert.fail("Expected exception for creating invalid document not received");
        } catch (CannotCreateDocumentException ccde) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception");
        }

        Assert.assertEquals(0, documentRepos.count());

        // same again, but now set an empty link list
        List<JsonAnnotationDocumentLink> links = new ArrayList<JsonAnnotationDocumentLink>();
        jsDoc.setLink(links);

        try {
            documentService.createNewDocument(jsDoc);
            Assert.fail("Expected exception for creating invalid document not received");
        } catch (CannotCreateDocumentException ccde) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception");
        }

        Assert.assertEquals(0, documentRepos.count());
    }

    /**
     * Test document is created by a JSON-serialized object 
     * @throws CannotCreateDocumentException 
     */
    @Test
    public void testCreateDocumentByJson() throws URISyntaxException, CannotCreateDocumentException {

        URI uri = new URI("http://www.link.eu");
        URI anotherUri = new URI("http://this.is.another.uri");

        JsonAnnotationDocument jsDoc = new JsonAnnotationDocument();
        jsDoc.setLink(null);

        // same again, but now set an empty link list
        List<JsonAnnotationDocumentLink> links = new ArrayList<JsonAnnotationDocumentLink>();

        // insert two (!) links into the list - only first should be taken into account
        JsonAnnotationDocumentLink firstLink = new JsonAnnotationDocumentLink();
        firstLink.setHref(uri);
        links.add(firstLink);

        JsonAnnotationDocumentLink secondLink = new JsonAnnotationDocumentLink();
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

        String url = "http://www.doc.eu";
        URI uri = new URI(url);

        Document firstDoc = documentService.createNewDocument(uri);
        Assert.assertNotNull(firstDoc);

        documentService.deleteDocument(firstDoc);

        // make sure document was removed
        Assert.assertEquals(0, documentRepos.count());

        // trying to delete this document once more should not complain
        try {
            documentService.deleteDocument(firstDoc);
        } catch (Exception e) {
            Assert.fail("Received unexpected exception");
        }
    }

    /**
     * Test deletion of a document being null
     */
    @Test
    public void testDeleteNullDocument() {

        // trying to delete an invalid document should throw exception
        try {
            documentService.deleteDocument(null);
            Assert.fail("Expected exception for deleting invalid document not received");
        } catch (CannotDeleteDocumentException ccde) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception");
        }
    }
}
