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
package eu.europa.ec.leos.annotate.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.helper.SerialisationHelper;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.helper.TestHelper;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchCount;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.TagsService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class CountAnnotationsTest {

    private static final String ACCESS_TOKEN = "demoaccesstoken";
    private static final String REFRESH_TOKEN = "helloRefresh";
    private static final String ISCREF = "ISCRef";
    private static final String RESP_VERSION = "respVersion";
    private static final String ISCREF1 = "2018/1";
    private static final String ISCREF2 = "2018/2";
    private static final String ISCREF3 = "2018/3";
    private static final String META_PATTERN = "{\"key\":\"val\"}";

    private static final String API_COUNT = "/api/count";

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private TokenRepository tokenRepos;

    @Autowired
    private TagsService tagsService;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    private User theUser;

    private final BiFunction<String, String, String> metaReq = (String key, String val) -> META_PATTERN.replace("key", key).replace("val", val);

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        TestDbHelper.cleanupRepositories(this);
        TestDbHelper.insertDefaultGroup(groupRepos);

        theUser = new User("demo");
        userRepos.save(theUser);

        final DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
        this.mockMvc = builder.build();
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * successfully retrieve the correct number of annotations, expected HTTP 200 and returned annotation count
     * checks that correct type of annotations are returned
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testCountAnnotations_checkAnnotationType() throws Exception {

        tokenRepos.save(new Token(theUser, Authorities.ISC, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5),
                REFRESH_TOKEN, LocalDateTime.now().plusMinutes(5)));

        final URI uri = new URI("http://leos/48");
        final Document document = new Document(uri, "");
        documentRepos.save(document);

        final Group group = new Group("DGAGRI", true);
        groupRepos.save(group);
        userGroupRepos.save(new UserGroup(theUser.getId(), group.getId()));

        final Metadata meta = new Metadata(document, group, Authorities.ISC);
        meta.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(meta);

        // create a public comment -> found!
        final Annotation annotPubComment = getAnnotation(meta, "theidPubComm", "public comment", true, Annotation.ANNOTATION_COMMENT, 
                AnnotationStatus.NORMAL);
        annotRepos.save(annotPubComment);

        // create a private comment -> ignored
        final Annotation annotPrivComment = getAnnotation(meta, "theidPrivComm", "private comment", false, Annotation.ANNOTATION_COMMENT,
                AnnotationStatus.NORMAL);
        annotRepos.save(annotPrivComment);

        // create a public page note -> found!
        final Annotation annotPubPageNote = getAnnotation(meta, "theidPubPageNote", "public page note", true, "", AnnotationStatus.NORMAL);
        annotRepos.save(annotPubPageNote);

        // create a private page note -> ignored
        final Annotation annotPrivPageNote = getAnnotation(meta, "theidPrivPageNote", "private page note", false, Annotation.ANNOTATION_PAGENOTE,
                AnnotationStatus.NORMAL);
        annotRepos.save(annotPrivPageNote);

        // create a public suggestion -> found!
        final Annotation annotPubSugg = getAnnotation(meta, "theidPubSugg", "public suggestion", true, Annotation.ANNOTATION_SUGGESTION,
                AnnotationStatus.NORMAL);
        annotRepos.save(annotPubSugg);

        // create a private suggestion -> ignored
        final Annotation annotPrivSugg = getAnnotation(meta, "theidPrivSugg", "public suggestion", false, Annotation.ANNOTATION_SUGGESTION,
                AnnotationStatus.NORMAL);
        annotRepos.save(annotPrivSugg);

        // create a public highlight -> ignored since highlights are ignored in general
        final Annotation annotPubHigh = getAnnotation(meta, "theidPubHigh", "public highlight", true, Annotation.ANNOTATION_HIGHLIGHT, 
                AnnotationStatus.NORMAL);
        annotRepos.save(annotPubHigh);

        // create a private highlight -> ignored since highlights are ignored in general
        final Annotation annotPrivHigh = getAnnotation(meta, "theidPrivHigh", "public highlight", false, Annotation.ANNOTATION_HIGHLIGHT,
                AnnotationStatus.NORMAL);
        annotRepos.save(annotPrivHigh);

        // create a reply to the public comment -> ignored since only top-level items are counted (i.e. no replies)
        final Annotation annotReplyPubComment = getAnnotation(meta, "theidPubCommReply", "public comment reply", true, Annotation.ANNOTATION_COMMENT,
                AnnotationStatus.NORMAL);
        annotReplyPubComment.setReferences(annotPubComment.getId());
        annotRepos.save(annotReplyPubComment);

        // create a public comment that was already deleted -> ignored since only NORMAL items are counted
        final Annotation annotPubCommentDeleted = getAnnotation(meta, "theidPubCommDel", "public comment, deleted", true, Annotation.ANNOTATION_COMMENT,
                AnnotationStatus.DELETED);
        annotRepos.save(annotPubCommentDeleted);

        // create a public suggestion that was already accepted -> ignored since only NORMAL items are counted
        final Annotation annotPubSuggAccepted = getAnnotation(meta, "theidPubSuggAcc", "public suggestion, accepted", true, Annotation.ANNOTATION_SUGGESTION,
                AnnotationStatus.ACCEPTED);
        annotRepos.save(annotPubSuggAccepted);

        // create a public suggestion that was already rejected -> ignored since only NORMAL items are counted
        final Annotation annotPubSuggRejected = getAnnotation(meta, "theidPubSuggRej", "public suggestion, rejected", true, Annotation.ANNOTATION_SUGGESTION,
                AnnotationStatus.REJECTED);
        annotRepos.save(annotPubSuggRejected);

        // note: encoding curly brackets (metadata) prevents that MockMvc wants to resolve the content as a variable
        String url = appendUri(API_COUNT, uri);
        url = appendGroup(url, group) + "&metadatasets=[%7B\"systemId\":\"ISC\"%7D]";

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(url)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonSearchCount jsResponse = SerialisationHelper.deserializeJsonSearchCount(responseString);
        Assert.assertNotNull(jsResponse);

        // IDs of found annotations should be "theidPubComm", "theidPubPageNote", "theidPubSugg"
        // -> thus three items
        Assert.assertEquals(3, jsResponse.getCount());
    }

    /**
     * successfully retrieve the correct number of annotations, expected HTTP 200 and returned annotation count
     * checks that annotations having expected metadata are returned
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testCountAnnotations_checkMetadata() throws Exception {

        final String ISCREF = "ISCRef";
        final String ISCREF_IGNORED = "2018/1";
        final String ISCREF_FOUND = "2018/4";

        tokenRepos.save(new Token(theUser, Authorities.ISC, ACCESS_TOKEN,
                LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN,
                LocalDateTime.now().plusMinutes(5)));

        final URI uri = new URI("http://leos/4");
        final Document document = new Document(uri, "");
        documentRepos.save(document);

        final Group group = new Group("AGRI", true);
        groupRepos.save(group);
        userGroupRepos.save(new UserGroup(theUser.getId(), group.getId()));

        // save two metadata items - only one is queried
        final Metadata metaFirst = new Metadata(document, group, Authorities.ISC);
        final SimpleMetadata metaFirstProps = new SimpleMetadata();
        metaFirstProps.put(ISCREF, ISCREF_IGNORED);
        metaFirst.setKeyValuePropertyFromSimpleMetadata(metaFirstProps);
        metaFirst.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(metaFirst);

        final Metadata metaSecond = new Metadata(document, group, Authorities.ISC);
        final SimpleMetadata metaSecondProps = new SimpleMetadata();
        metaSecondProps.put(ISCREF, ISCREF_FOUND);
        metaSecond.setKeyValuePropertyFromSimpleMetadata(metaSecondProps);
        metaSecond.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(metaSecond);

        // create three public comments with first metadata -> ignored
        final Annotation annotPubComment = getSimpleAnnotation(metaFirst, "theidPubComm");
        annotRepos.save(annotPubComment);

        final Annotation annotPubComment2 = getSimpleAnnotation(metaFirst, "theidPubComm2");
        annotRepos.save(annotPubComment2);

        final Annotation annotPubComment3 = getSimpleAnnotation(metaFirst, "theidPubComm3");
        annotRepos.save(annotPubComment3);

        // create two public comments with second metadata -> found!
        final Annotation annotPubCommentFound = getSimpleAnnotation(metaSecond, "theidPubCommFound");
        annotRepos.save(annotPubCommentFound);

        final Annotation annotPubCommentFound2 = getSimpleAnnotation(metaSecond, "theidPubCommFound2");
        annotRepos.save(annotPubCommentFound2);

        // note: encoding curly brackets (metadata) prevents that MockMvc wants to resolve the content as a variable
        String url = appendUri(API_COUNT, uri);
        url = appendGroup(url, group) + "&metadatasets=[%7B\"" + ISCREF + "\":\"" + ISCREF_FOUND +
                "\"%7D]";

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(url)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonSearchCount jsResponse = SerialisationHelper.deserializeJsonSearchCount(responseString);
        Assert.assertNotNull(jsResponse);

        // two items should be found
        Assert.assertEquals(2, jsResponse.getCount());
    }

    @Test
    public void queryMeta_noMeta() {

        // query without metadata -> receive all annotations
        Assert.assertEquals(3, runCountAnnotations("[]"));
    }

    @Test
    public void queryMeta_IscRef1() {

        // query for reference 1 -> receive one annotation
        Assert.assertEquals(1, runCountAnnotations("[" + metaReq.apply(ISCREF, ISCREF1) + "]"));
    }

    @Test
    public void queryMeta_IscRef2() {

        // query for reference 2 -> receive one annotation
        Assert.assertEquals(1, runCountAnnotations("[" + metaReq.apply(ISCREF, ISCREF2) + "]"));
    }

    @Test
    public void queryMeta_IscRef3() {

        // query for reference 3 -> receive one annotation
        Assert.assertEquals(1, runCountAnnotations("[" + metaReq.apply(ISCREF, ISCREF3) + "]"));
    }

    @Test
    public void queryMeta_Version1() {

        // query for version 1 -> receive two annotations
        Assert.assertEquals(2, runCountAnnotations("[" + metaReq.apply(RESP_VERSION, "1") + "]"));
    }

    @Test
    public void queryMeta_Version2() {

        // query for version 2 -> receive two annotations
        Assert.assertEquals(1, runCountAnnotations("[" + metaReq.apply(RESP_VERSION, "2") + "]"));
    }

    @Test
    public void queryMeta_IscRef1Vers1() {

        final String metaString = metaReq.apply(ISCREF, ISCREF1) + "," + metaReq.apply(RESP_VERSION, "1");

        // query for reference 1 version 1 -> receive two annotations (two have version 1)
        Assert.assertEquals(2, runCountAnnotations("[" + metaString + "]"));
    }

    @Test
    public void queryMeta_Vers1Vers2() {

        final String metaString = metaReq.apply(RESP_VERSION, "1") + "," + metaReq.apply(RESP_VERSION, "2");

        // query for version 1 or 2 -> receive all annotations
        Assert.assertEquals(3, runCountAnnotations("[" + metaString + "]"));
    }

    @Test
    public void queryMeta_Vers3Vers5() {

        final String metaString = metaReq.apply(RESP_VERSION, "3") + "," + metaReq.apply(RESP_VERSION, "5");

        // query for version 3 or 5 -> receive no annotations
        Assert.assertEquals(0, runCountAnnotations("[" + metaString + "]"));
    }

    @Test
    public void queryMeta_Vers1To100() {

        final StringBuilder metaString = new StringBuilder();
        final int MAX_SETS = 100;

        for (int i = 1; i <= MAX_SETS; i++) {
            metaString.append(metaReq.apply(RESP_VERSION, Integer.toString(i)));
            if (i < MAX_SETS) {
                metaString.append(',');
            }
        }

        // query for versions 1 to 100 -> receive three annotations
        Assert.assertEquals(3, runCountAnnotations("[" + metaString.toString() + "]"));
    }

    @Test
    public void queryMeta_Vers1TenTimes() {

        final StringBuilder metaString = new StringBuilder();
        final int MAX_SETS = 10;

        for (int i = 1; i <= MAX_SETS; i++) {
            metaString.append(metaReq.apply(RESP_VERSION, "1"));
            if (i < MAX_SETS) {
                metaString.append(',');
            }
        }

        // query for versions 1 (ten times) -> receive two annotations
        Assert.assertEquals(2, runCountAnnotations("[" + metaString.toString() + "]"));
    }

    private long runCountAnnotations(final String metadata) {

        try {
            final BiConsumer<SimpleMetadata, String> putVersion = (simpleMeta, vers) -> simpleMeta.put(RESP_VERSION, vers);

            tokenRepos.save(new Token(theUser, Authorities.ISC, ACCESS_TOKEN,
                    LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN,
                    LocalDateTime.now().plusMinutes(5)));

            final URI uri = new URI("http://leos/4");
            final Document document = new Document(uri, "");
            documentRepos.save(document);

            final Group group = new Group("AGRI", true);
            groupRepos.save(group);
            userGroupRepos.save(new UserGroup(theUser.getId(), group.getId()));

            // save three metadata items
            final Metadata metaFirst = new Metadata(document, group, Authorities.ISC);
            final SimpleMetadata metaFirstProps = new SimpleMetadata(ISCREF, ISCREF1);
            putVersion.accept(metaFirstProps, "1");
            metaFirst.setKeyValuePropertyFromSimpleMetadata(metaFirstProps);
            metaFirst.setResponseStatus(Metadata.ResponseStatus.SENT);
            metadataRepos.save(metaFirst);

            final Metadata metaSecond = new Metadata(document, group, Authorities.ISC);
            final SimpleMetadata metaSecondProps = new SimpleMetadata(ISCREF, ISCREF2);
            putVersion.accept(metaSecondProps, "1");
            metaSecond.setKeyValuePropertyFromSimpleMetadata(metaSecondProps);
            metaSecond.setResponseStatus(Metadata.ResponseStatus.SENT);
            metadataRepos.save(metaSecond);

            final Metadata metaThree = new Metadata(document, group, Authorities.ISC);
            final SimpleMetadata metaThreeProps = new SimpleMetadata(ISCREF, ISCREF3);
            putVersion.accept(metaThreeProps, "2");
            metaThree.setKeyValuePropertyFromSimpleMetadata(metaThreeProps);
            metaThree.setResponseStatus(Metadata.ResponseStatus.SENT);
            metadataRepos.save(metaThree);

            // create three public comments each having one metadata assigned
            final Annotation annot1 = getSimpleAnnotation(metaFirst, "comm1");
            annotRepos.save(annot1);

            final Annotation annot2 = getSimpleAnnotation(metaSecond, "comm2");
            annotRepos.save(annot2);

            final Annotation annot3 = getSimpleAnnotation(metaThree, "comm3");
            annotRepos.save(annot3);

            // note: encoding curly brackets (metadata) prevents that MockMvc wants to resolve the content as a variable
            String url = appendUri(API_COUNT, uri);
            url = appendGroup(url, group) + "&metadatasets=" + metadata.replace("{", "%7B").replaceAll("}", "%7D");

            final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(url)
                    .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

            final ResultActions result = this.mockMvc.perform(builder);

            // expected: Http 200
            result.andExpect(MockMvcResultMatchers.status().isOk());

            final MvcResult resultContent = result.andReturn();
            final String responseString = resultContent.getResponse().getContentAsString();

            // ID must have been set
            final JsonSearchCount jsResponse = SerialisationHelper.deserializeJsonSearchCount(responseString);
            Assert.assertNotNull(jsResponse);

            return jsResponse.getCount();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        return -1;
    }

    /**
     * call annotation counting with an EdiT user, expected HTTP 400 and error response
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    @Test
    public void testCountAnnotations_FailsForLeosUser() throws Exception {

        // save token for an EdiT user -> API call will be refused
        tokenRepos.save(
                new Token(theUser, Authorities.EdiT, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN, LocalDateTime.now().plusMinutes(5)));

        final URI uri = new URI("http://leos/4");
        final Document document = new Document(uri, "");
        documentRepos.save(document);
        final Group group = new Group("AGRI", true);
        groupRepos.save(group);
        userGroupRepos.save(new UserGroup(theUser.getId(), group.getId()));
        final Metadata meta = new Metadata(document, group, Authorities.ISC);
        metadataRepos.save(meta);

        // create a public comment -> can be found!
        final Annotation annotPubComment = getAnnotation(meta, "theidPubComm", "public comment", true, Annotation.ANNOTATION_COMMENT, AnnotationStatus.NORMAL);
        annotRepos.save(annotPubComment);

        String url = appendUri(API_COUNT, uri);
        url = appendGroup(url, group) + "&metadatasets=";
        url += "[%7B\"systemId\":\"LEOS\"%7D]"; // encoding curly brackets prevents attempts to expand JSON content from environment variables

        // launch request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(url)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!StringUtils.isEmpty(jsResponse.getReason()));
    }

    private Annotation getAnnotation(final Metadata meta, final String annotId,
            final String annotText, final boolean shared,
            final String tag, final AnnotationStatus status) {

        final Annotation annot = new Annotation();
        annot.setMetadata(meta);
        annot.setText(annotText);
        annot.setId(annotId);
        annot.setShared(shared);
        annot.setUser(theUser);
        annot.setCreated(LocalDateTime.now());
        annot.setUpdated(LocalDateTime.now());
        annot.setStatus(status);
        annot.setTargetSelectors("somewhere");
        if (!StringUtils.isEmpty(tag)) {
            annot.setTags(tagsService.getTagList(Arrays.asList(tag), annot));
        }

        return annot;
    }

    private Annotation getSimpleAnnotation(final Metadata meta, final String annotId) {

        return getAnnotation(meta, annotId, "public comment", true, Annotation.ANNOTATION_COMMENT,
                AnnotationStatus.NORMAL);
    }

    private String appendGroup(final String url, final Group group) {
        return url + "&group=" + group.getName();
    }

    private String appendUri(final String url, final URI uri) {
        return url + "?uri=" + uri.toString();
    }
}
