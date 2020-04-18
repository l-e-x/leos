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
package eu.europa.ec.leos.annotate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchCountOptions;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationDocumentLink;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotDeleteAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotDeleteSentAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.MissingPermissionException;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class AnnotationCountTest {

    /**
     * This class contains tests for counting the number of annotations for ISC users/annotations
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationService annotService;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private MetadataRepository metadataRepos;
    
    // -------------------------------------
    // Help variables
    // -------------------------------------
    private final static String LOGIN = "demo";
    private final static String ISCREF = "ISCReference";
    private final static String ISCREFVAL = "ISC/2019/007";
    private final static String RESPVERS = "responseVersion";
    private final static String RESPVERS1 = "1";
    private final static String RESPVERS2 = "2";
    private final static String SEPARATOR = ":\"";

    private URI dummyUri;
    private User theUser;

    // -------------------------------------
    // Cleanup of database content before running new test
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() throws URISyntaxException {

        TestDbHelper.cleanupRepositories(this);
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        theUser = userRepos.save(new User(LOGIN));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        dummyUri = new URI("http://some.url");
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * retrieving number of annotation is issued by a LEOS user
     * -> will be refused with exception
     */
    @Test(expected = MissingPermissionException.class)
    public void testCountAnnotationNotForLeosUser() throws Exception {

        final UserInformation userInfo = new UserInformation(LOGIN, Authorities.EdiT);
        annotService.getAnnotationsCount(new AnnotationSearchCountOptions(), userInfo);
    }

    /**
     * retrieving number of annotation, but for an unknown document
     * -> error result
     */
    @Test
    public void testCountAnnotationForUnknownDocument() throws Exception {

        final UserInformation userInfo = new UserInformation(LOGIN, Authorities.ISC);

        final AnnotationSearchCountOptions options = new AnnotationSearchCountOptions();
        options.setUri(new URI("http://some.thing"));

        Assert.assertEquals(-1, annotService.getAnnotationsCount(options, userInfo));
    }

    /**
     * retrieving number of annotation, but for an unknown group
     * -> error result
     */
    @Test
    public void testCountAnnotationForUnknownGroup() throws Exception {

        final UserInformation userInfo = new UserInformation(LOGIN, Authorities.ISC);

        documentRepos.save(new Document(dummyUri, "a"));

        final AnnotationSearchCountOptions options = new AnnotationSearchCountOptions();
        options.setUri(dummyUri);
        options.setGroup("unknownGroup");

        Assert.assertEquals(-1, annotService.getAnnotationsCount(options, userInfo));
    }

    /**
     * retrieving number of annotation, but for an unknown user
     * -> error result
     */
    @Test
    public void testCountAnnotationForUnknownUser() throws Exception {

        final UserInformation userInfo = new UserInformation(LOGIN, Authorities.ISC);
        userInfo.setUser(null); // make user "unknown" -> will be searched for

        documentRepos.save(new Document(dummyUri, "a"));

        final Group group = new Group("mygroup", true);
        groupRepos.save(group);

        final AnnotationSearchCountOptions options = new AnnotationSearchCountOptions();
        options.setUri(dummyUri);
        options.setGroup(group.getName());

        Assert.assertEquals(-1, annotService.getAnnotationsCount(options, userInfo));
    }

    /**
     * retrieving number of annotations, but for LEOS annotations (via metadata)
     * -> will be refused with exception
     */
    @Test(expected = MissingPermissionException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testCountAnnotationForLeosAnnotations() throws Exception {

        final UserInformation userInfo = new UserInformation(LOGIN, Authorities.ISC);

        documentRepos.save(new Document(dummyUri, "a"));

        final Group group = new Group("thegroup", true);
        groupRepos.save(group);
        userGroupRepos.save(new UserGroup(theUser.getId(), group.getId())); // assign user

        final String metadataMapJson = "[{\"systemId\":\"LEOS\"}]";

        final AnnotationSearchCountOptions options = new AnnotationSearchCountOptions();
        options.setUri(dummyUri);
        options.setGroup(group.getName());
        options.setMetadatasets(metadataMapJson);

        // asking for LEOS annotations will provoke exception
        annotService.getAnnotationsCount(options, userInfo);
    }

    /**
     * retrieving number of annotation, but there exists no matching metadata
     * -> result should be 0
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testCountAnnotationNoMatchingMetadata() throws Exception {

        final UserInformation userInfo = new UserInformation(LOGIN, Authorities.ISC);

        documentRepos.save(new Document(dummyUri, "a"));

        final Group group = new Group("thegroup", true);
        groupRepos.save(group);
        userGroupRepos.save(new UserGroup(theUser.getId(), group.getId())); // assign user

        final AnnotationSearchCountOptions options = new AnnotationSearchCountOptions();
        options.setUri(dummyUri);
        options.setGroup(group.getName());
        options.setMetadatasets("");

        // should not find any items
        Assert.assertEquals(0, annotService.getAnnotationsCount(options, userInfo));
    }

    /**
     * retrieving number of annotations: ask for none
     * -> all annotations should be counted
     */
    @Test
    public void testCountAnnotation_noMetadataRequested() throws Exception {

        // ask without any metadata - should find all three annotations as there are no restrictions
        Assert.assertEquals(3, runCountAnnotationSeveralMatchingMetadata("[]"));
    }

    /**
     * retrieving number of annotations: ask for empty list
     * -> all annotations should be counted
     */
    @Test
    public void testCountAnnotation_noMetadataRequestedViaEmptyList() throws Exception {

        // ask without any metadata - should find all three annotations as there are no restrictions
        Assert.assertEquals(3, runCountAnnotationSeveralMatchingMetadata("[{}]"));
    }

    /**
     * retrieving number of annotations: ask for several empty lists
     * -> all annotations should be counted
     */
    @Test
    public void testCountAnnotation_noMetadataRequestedViaEmptyLists() throws Exception {

        // ask without any metadata - should find all three annotations as there are no restrictions
        Assert.assertEquals(3, runCountAnnotationSeveralMatchingMetadata("[{},{},{}]"));
    }

    /**
     * retrieving number of annotations: ask for ISC reference
     * -> all annotations should be counted
     */
    @Test
    public void testCountAnnotation_IscReference() throws Exception {

        // ask for ISC Reference - should find all annotations
        Assert.assertEquals(3, runCountAnnotationSeveralMatchingMetadata("[{" + ISCREF + SEPARATOR + ISCREFVAL + "\"}]"));
    }
    
    /**
     * retrieving number of annotations: ask for ISC reference and explicitly NORMAL status
     * -> all annotations should be counted
     */
    @Test
    public void testCountAnnotation_IscReference_Normal() throws Exception {

        // ask for ISC Reference - should find all annotations
        Assert.assertEquals(3, runCountAnnotationSeveralMatchingMetadata("[{\"" + ISCREF + "\":\"" + ISCREFVAL + "\", \"status\":[\"NORMAL\"]}]"));
    }

    /**
     * retrieving number of annotations: ask for ISC reference and explicitly DELETED status
     * -> one annotation should be counted (the deleted page note)
     */
    @Test
    public void testCountAnnotation_IscReference_Deleted() throws Exception {

        // ask for ISC Reference - should find one annotation
        Assert.assertEquals(1, runCountAnnotationSeveralMatchingMetadata("[{" + ISCREF + SEPARATOR + ISCREFVAL + "\", \"status\":[\"DELETED\"]}]"));
    }
    
    /**
     * retrieving number of annotations: ask for ISC reference and ACCEPTED or REJECTED status
     * -> no matches
     */
    @Test
    public void testCountAnnotation_IscReference_AcceptedRejected() throws Exception {

        // ask for ISC Reference - should not find any annotation
        Assert.assertEquals(0, runCountAnnotationSeveralMatchingMetadata("[{\"" + ISCREF + "\":\"" + ISCREFVAL + "\", \"status\":[\"ACCEPTED\",\"REJECTED\"]}]"));
    }
    
    /**
     * retrieving number of annotations: ask for response version 1
     * -> one annotation should be counted
     */
    @Test
    public void testCountAnnotation_responseVersion1() throws Exception {

        // ask for response version 1 - should only find one annotation
        Assert.assertEquals(1, runCountAnnotationSeveralMatchingMetadata("[{" + RESPVERS + ":" + RESPVERS1 + "}]"));
    }

    /**
     * retrieving number of annotations: ask for response version 1 and DELETED status
     * -> one annotation should be counted
     */
    @Test
    public void testCountAnnotation_responseVersion1_DELETED() throws Exception {

        // ask for response version 1 - should only find one annotation
        Assert.assertEquals(1, runCountAnnotationSeveralMatchingMetadata("[{" + RESPVERS + ":" + RESPVERS1 + ", status:[\"DELETED\"]}]"));
    }
    
    /**
     * retrieving number of annotations: ask for response version 1 and ALL statuses
     * -> one annotation should be counted
     */
    @Test
    public void testCountAnnotation_responseVersion1_AllStatuses() throws Exception {

        // ask for response version 1 - should find two annotations (one alive, one deleted)
        Assert.assertEquals(2, runCountAnnotationSeveralMatchingMetadata("[{" + RESPVERS + ":" + RESPVERS1 + ", status:[\"ALL\"]}]"));
    }
    
    /**
     * retrieving number of annotations: ask for response version 2
     * -> one annotation should be counted
     */
    @Test
    public void testCountAnnotation_responseVersion2() throws Exception {

        // ask for response version 2 - should only find one annotation
        Assert.assertEquals(1, runCountAnnotationSeveralMatchingMetadata("[{" + RESPVERS + ":" + RESPVERS2 + "}]"));
    }

    /**
     * retrieving number of annotations: ask for unknown response version
     * -> no annotation should be counted
     */
    @Test
    public void testCountAnnotation_responseVersionUnknown() throws Exception {

        // ask for an unknown response version - should not find any matches
        Assert.assertEquals(0, runCountAnnotationSeveralMatchingMetadata("[{" + RESPVERS + ":999}]"));
    }

    /**
     * retrieving number of annotations: ask for response versions 1 or 2
     * -> two annotations should be counted
     */
    @Test
    public void testCountAnnotation_responseVersions1Or2() throws Exception {

        // ask for response versions 1 or 2 - should find two annotations
        Assert.assertEquals(2, runCountAnnotationSeveralMatchingMetadata("[{" + RESPVERS + ":" + RESPVERS1 + "},{" + RESPVERS + ":" + RESPVERS2 + "}]"));
    }
    
    /**
     * retrieving number of annotations: ask for response versions 1 or 2 and ALL statuses
     * -> three annotations should be counted (two annotations, one deleted page note)
     */
    @Test
    public void testCountAnnotation_responseVersions1Or2_AllStatuses() throws Exception {

        // ask for response versions 1 or 2 in all statuses - should find three items
        Assert.assertEquals(3, runCountAnnotationSeveralMatchingMetadata(
                "[{" + RESPVERS + SEPARATOR + RESPVERS1 + "\", status:[\"ALL\"]},{" + RESPVERS + SEPARATOR + RESPVERS2 + "\", status:[\"NORMAL\",\"DELETED\",\"ACCEPTED\",\"REJECTED\"]}]"));
    }

    /**
     * retrieving number of annotations: ask for response versions 2 or 999
     * -> one annotation should be counted
     */
    @Test
    public void testCountAnnotation_responseVersions2Or999() throws Exception {

        // ask for response versions 2 and 999 - should only find one annotation (having response version 2)
        Assert.assertEquals(1, runCountAnnotationSeveralMatchingMetadata("[{" + RESPVERS + ":" + RESPVERS2 + "},{" + RESPVERS + ":999}]"));
    }

    /**
     * retrieving number of annotations: ask for response versions 1 to 1000
     * -> two annotations should be counted (having response versions 1 or 2)
     */
    @Test
    public void testCountAnnotation_response1000Versions() throws Exception {

        final int MAX_SETS = 1000;

        // ask for response versions 1 to 1000 - should only find two annotations (having response version 1 or 2)
        final StringBuilder sbMeta = new StringBuilder();
        sbMeta.append('[');
        for (int i = 1; i <= MAX_SETS; i++) {
            sbMeta.append('{').append(RESPVERS).append(':').append(i).append('}');
            if (i < MAX_SETS) {
                sbMeta.append(',');
            }
        }
        sbMeta.append(']');
        Assert.assertEquals(2, runCountAnnotationSeveralMatchingMetadata(sbMeta.toString()));
    }

    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    private long runCountAnnotationSeveralMatchingMetadata(final String metadataWithStatus)
            throws CannotCreateAnnotationException, MissingPermissionException, CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        final String hypoAccount = "acct:" + theUser.getLogin() + "@" + Authorities.ISC;
        final UserInformation userInfo = new UserInformation(LOGIN, Authorities.ISC);
        userInfo.setCurrentToken(new Token(userRepos.findByLogin(LOGIN), userInfo.getAuthority(), "a", LocalDateTime.now().plusMinutes(1),
            "r", LocalDateTime.now().plusMinutes(1)));

        final Group group = new Group("thegroup", true);
        groupRepos.save(group);
        userGroupRepos.save(new UserGroup(theUser.getId(), group.getId())); // assign user

        // prepare metadata: twice with two entries, once with a single entry
        final SimpleMetadata metaIscRef = new SimpleMetadata(ISCREF, ISCREFVAL);

        final SimpleMetadata metaIscRefVers1 = new SimpleMetadata(metaIscRef);
        metaIscRefVers1.put(Metadata.PROP_RESPONSE_VERSION, RESPVERS1);

        final SimpleMetadata metaIscRefVers2 = new SimpleMetadata(metaIscRef);
        metaIscRefVers2.put(Metadata.PROP_RESPONSE_VERSION, RESPVERS2);

        final JsonAnnotation annotRefVers1 = TestData.getTestAnnotationObject(hypoAccount);
        annotRefVers1.getDocument().setLink(Arrays.asList(new JsonAnnotationDocumentLink(dummyUri)));
        annotRefVers1.getDocument().setMetadata(metaIscRefVers1);
        annotRefVers1.setUri(dummyUri);
        annotRefVers1.setGroup(group.getName());
        annotRefVers1.setTags(Arrays.asList(Annotation.ANNOTATION_COMMENT));
        annotService.createAnnotation(annotRefVers1, userInfo);

        final JsonAnnotation annotRefVers2 = TestData.getTestAnnotationObject(hypoAccount);
        annotRefVers2.getDocument().setLink(Arrays.asList(new JsonAnnotationDocumentLink(dummyUri)));
        annotRefVers2.getDocument().setMetadata(metaIscRefVers2);
        annotRefVers2.setUri(dummyUri);
        annotRefVers2.setGroup(group.getName());
        annotRefVers2.setTags(Arrays.asList(Annotation.ANNOTATION_COMMENT));
        annotService.createAnnotation(annotRefVers2, userInfo);

        final JsonAnnotation annotRef = TestData.getTestAnnotationObject(hypoAccount);
        annotRef.getDocument().setLink(Arrays.asList(new JsonAnnotationDocumentLink(dummyUri)));
        annotRef.getDocument().setMetadata(metaIscRef);
        annotRef.setUri(dummyUri);
        annotRef.setGroup(group.getName());
        annotRef.setTags(Arrays.asList(Annotation.ANNOTATION_COMMENT));
        annotService.createAnnotation(annotRef, userInfo);
        
        // create a page note and delete it directly
        final JsonAnnotation pageNoteDelRefVers1 = TestData.getTestAnnotationObject(hypoAccount);
        pageNoteDelRefVers1.getDocument().setLink(Arrays.asList(new JsonAnnotationDocumentLink(dummyUri)));
        pageNoteDelRefVers1.getDocument().setMetadata(metaIscRefVers1);
        pageNoteDelRefVers1.setUri(dummyUri);
        pageNoteDelRefVers1.setGroup(group.getName());
        pageNoteDelRefVers1.setTags(null); // = page note
        annotService.createAnnotation(pageNoteDelRefVers1, userInfo);
        annotService.deleteAnnotationById(pageNoteDelRefVers1.getId(), userInfo);

        // set all saved metadata to have responseStatus SENT
        final List<Metadata> metas = (List<Metadata>) metadataRepos.findAll();
        for(final Metadata meta : metas) {
            meta.setResponseStatus(Metadata.ResponseStatus.SENT);
            metadataRepos.save(meta);
        }
        
        // prepare options and launch the counting
        final AnnotationSearchCountOptions options = new AnnotationSearchCountOptions();
        options.setUri(dummyUri);
        options.setGroup(group.getName());
        options.setMetadatasets(metadataWithStatus);

        return annotService.getAnnotationsCount(options, userInfo);
    }
}
