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
package eu.europa.ec.leos.annotate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResultWithSeparateReplies;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AnnotationSearchExtendedTest {

    /**
     * Tests on search functionality for annotations - extended sample data taken from our test database
     * (i.e. data set with which we observed unexpected results)
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private AnnotationRepository annotRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private AnnotationService annotService;

    // -------------------------------------
    // Cleanup of database content and prepare test data
    // -------------------------------------
    private static final String DOC_URL = "uri://LEOS/doc1";

    private static final String USER_LOGIN = "userLogin";

    private static final String ID_ANNOT_FIRSTUSER_1_PUB = "dDx4SEAjR-C4TR0J8s9IaQ";
    private static final String ID_ANNOT_FIRSTUSER_2_PUB = "E4-LjixbS0CwNEPu9EDfjA";
    private static final String ID_ANNOT_FIRSTUSER_3_PUB = "cUkva_1MSVaLXGIHOEVBHA";
    private static final String ID_ANNOT_FIRSTUSER_4_PUB = "imMyuSbeSoetO2-_cMfnrQ";
    private static final String ID_ANNOT_FIRSTUSER_5_PRIV = "gD8BHOs9R--PHDygxS4prg";
    private static final String ID_ANNOT_FIRSTUSER_6_PRIV = "SMEMXl4gSbW0j1Gj3SLQ3w";
    private static final String ID_ANNOT_FIRSTUSER_7_PRIV = "8Z16HBetT26H6NybYaI7Yg";
    private static final String ID_ANNOT_FIRSTUSER_8_PRIV = "IaozutxkRUq8Nthrz1EmvQ";
    private static final String ID_ANNOT_FIRSTUSER_9_PRIV = "AeAJ6jX1RjOv4OpOfFEZdw";
    private static final String ID_ANNOT_FIRSTUSER_10_PUB = "jVtHuWFsQiOGD8Vg0GMnVQ";
    private static final String ID_ANNOT_FIRSTUSER_11_PUB = "x8USFnmzTwaxBFFI02uKWA";
    private static final String ID_ANNOT_FIRSTUSER_12_PUB = "cB_aTYVCTfKkwKknBndUeQ";
    private static final String ID_ANNOT_FIRSTUSER_13_PUB = "CToFE33XQiOT0KdemkBeLA";
    private static final String ID_ANNOT_FIRSTUSER_14_PUB = "kQi4OwhBSFKUe9zu9NvTAA";
    private static final String ID_ANNOT_FIRSTUSER_15_PUB = "6LUUKoRoQWqcFTTwEAcRaQ";
    private static final String ID_ANNOT_FIRSTUSER_16_PUB = "yVhIPm4sT7S8udmfyEvLTg";
    private static final String ID_ANNOT_FIRSTUSER_17_PUB = "RB0WNYljQ2meOre2WFrzpw";
    private static final String ID_ANNOT_FIRSTUSER_18_PUB = "4rzazIXwQUK2kYkYGwonAw";
    private static final String ID_ANNOT_FIRSTUSER_19_PUB_REPLY = "XVz5ED6cTNOyLDZNLc-eOg";
    private static final String ID_ANNOT_FIRSTUSER_19_PUB_REPLY_DELETED = "DFUah4X2SHahcu6bjgQ8Aw";
    private static final String ID_ANNOT_FIRSTUSER_20_PUB = "VX12QSnGQuiHCTiLGfbikA";
    private static final String ID_ANNOT_FIRSTUSER_21_PUB_REPLY = "WFCffWcLQYuUYojZuBkWrg";
    private static final String ID_ANNOT_FIRSTUSER_22_PUB_REPLY = "6SzIQC-CS5GV9NkCCvnPUQ";
    private static final String ID_ANNOT_FIRSTUSER_23_PUB_REPLY = "_N6ZA5v0SC2vHz5BGIxWKA";
    private static final String ID_ANNOT_FIRSTUSER_24_PRIV = "FnAOJPctQzS7AdarWxmVqQ";
    private static final String ID_ANNOT_FIRSTUSER_25_PRIV = "cpXc7X_JSnC7WXAU_ZMPsQ";
    private static final String ID_ANNOT_SECONDUSER_26_PUB = "Zi6d7Q1fQd2-RZFUlmsdWg";
    private static final String ID_ANNOT_FIRSTUSER_27_PRIV_PAGENOTE = "rOLKWGlRQrmWgFPXA18_Ww";
    private static final String ID_ANNOT_FIRSTUSER_28_PUB_REPLY_PAGENOTE = "fpvfFb83RNewptNHxXgYiw";
    private static final String ID_ANNOT_SECONDUSER_29_PRIV_HIGHLIGHT = "qxwnazYgQ3-oYLzRaOCz0w";
    private static final String ID_ANNOT_FIRSTUSER_30_PUB_PAGENOTE = "SdAx2AeHQE2NBMjcIcV5DA";
    private static final String ID_ANNOT_FIRSTUSER_31_PUB = "2NTBWRxhTM2EkhzId_r1aA";
    private static final String ID_ANNOT_FIRSTUSER_32_PUB = "toDnbyCSRaSiW5aY6O9Jgw";
    private static final String ID_ANNOT_FIRSTUSER_33_PRIV = "H8yF15JFSxWcjhMCljMqOg";
    private static final String ID_ANNOT_THIRDUSER_34_PUB = "1NMs9Xc9Tmiui9rHDi7c6A";
    private static final String ID_ANNOT_THIRDUSER_35_PUB_REPLY = "OtWIOBKyRMeBm9oOrMzatA";
    private static final String ID_ANNOT_THIRDUSER_36_PUB = "TL_s-VviQeecIgEdStr3qg";

    @Before
    public void cleanDatabaseBeforeTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // insert user, assign to the default group
        User firstUser = new User(USER_LOGIN), secondUser = new User("secondUser"), thirdUser = new User("thirdUser");
        userRepos.save(Arrays.asList(firstUser, secondUser, thirdUser));
        userGroupRepos.save(new UserGroup(firstUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(secondUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(thirdUser.getId(), defaultGroup.getId()));

        // insert a document and metadata
        Document firstDoc = new Document(new URI(DOC_URL), "document's title");
        documentRepos.save(firstDoc);

        Metadata meta = new Metadata(firstDoc, defaultGroup, "thesystem");
        metadataRepos.save(meta);

        // insert annotations; following structure (annotations/replies)
        // 1
        // ...
        // 17
        // 18
        // └ deleted reply
        // └ 19
        // 20
        // └ 21
        // | └ 23
        // └ 22
        // 24
        // 25
        // 26
        // └ 35
        // 27
        // ...
        // 34
        // 36

        // dummy selector
        String dummySelector = "[{\"selector\":null,\"source\":\"" + firstDoc.getUri() + "\"}]";

        // first public annotation of first user
        Annotation firstAnnot = new Annotation();
        firstAnnot.setId(ID_ANNOT_FIRSTUSER_1_PUB);
        firstAnnot.setCreated(LocalDateTime.of(2017, 12, 22, 11, 40, 57));
        firstAnnot.setUpdated(LocalDateTime.of(2017, 12, 22, 11, 42, 59));
        firstAnnot.setDocument(firstDoc);
        firstAnnot.setGroup(defaultGroup);
        firstAnnot.setMetadata(meta);
        firstAnnot.setReferences("");
        firstAnnot.setShared(true);
        firstAnnot.setTargetSelectors(dummySelector);
        firstAnnot.setText("www");
        firstAnnot.setUser(firstUser);
        annotRepos.save(firstAnnot);

        // second public annotation of first user
        Annotation secondAnnot = new Annotation();
        secondAnnot.setId(ID_ANNOT_FIRSTUSER_2_PUB);
        secondAnnot.setCreated(LocalDateTime.of(2017, 12, 22, 12, 11, 28));
        secondAnnot.setUpdated(LocalDateTime.of(2017, 12, 22, 12, 11, 46));
        secondAnnot.setDocument(firstDoc);
        secondAnnot.setGroup(defaultGroup);
        secondAnnot.setMetadata(meta);
        secondAnnot.setReferences("");
        secondAnnot.setShared(true);
        secondAnnot.setTargetSelectors(dummySelector);
        secondAnnot.setText("rrr");
        secondAnnot.setUser(firstUser);
        annotRepos.save(secondAnnot);

        // third public annotation of first user
        Annotation thirdAnnot = new Annotation();
        thirdAnnot.setId(ID_ANNOT_FIRSTUSER_3_PUB);
        thirdAnnot.setCreated(LocalDateTime.of(2017, 12, 22, 12, 11, 29));
        thirdAnnot.setUpdated(LocalDateTime.of(2017, 12, 22, 12, 12, 33));
        thirdAnnot.setDocument(firstDoc);
        thirdAnnot.setGroup(defaultGroup);
        thirdAnnot.setMetadata(meta);
        thirdAnnot.setReferences("");
        thirdAnnot.setShared(true);
        thirdAnnot.setTargetSelectors(dummySelector);
        thirdAnnot.setText("rrr");
        thirdAnnot.setUser(firstUser);
        annotRepos.save(thirdAnnot);

        // fourth public annotation of first user
        Annotation fourthAnnot = new Annotation();
        fourthAnnot.setId(ID_ANNOT_FIRSTUSER_4_PUB);
        fourthAnnot.setCreated(LocalDateTime.of(2017, 12, 22, 13, 00, 57));
        fourthAnnot.setUpdated(LocalDateTime.of(2017, 12, 22, 13, 01, 49));
        fourthAnnot.setDocument(firstDoc);
        fourthAnnot.setGroup(defaultGroup);
        fourthAnnot.setMetadata(meta);
        fourthAnnot.setReferences("");
        fourthAnnot.setShared(true);
        fourthAnnot.setTargetSelectors(dummySelector);
        fourthAnnot.setText("ssssssssssssssssddddddddddddd");
        fourthAnnot.setUser(firstUser);
        annotRepos.save(fourthAnnot);

        // fifth annotation of first user: private!
        Annotation fifthAnnot = new Annotation();
        fifthAnnot.setId(ID_ANNOT_FIRSTUSER_5_PRIV);
        fifthAnnot.setCreated(LocalDateTime.of(2017, 12, 22, 13, 01, 56));
        fifthAnnot.setUpdated(LocalDateTime.of(2017, 12, 22, 13, 02, 15));
        fifthAnnot.setDocument(firstDoc);
        fifthAnnot.setGroup(defaultGroup);
        fifthAnnot.setMetadata(meta);
        fifthAnnot.setReferences("");
        fifthAnnot.setShared(false);
        fifthAnnot.setTargetSelectors(dummySelector);
        fifthAnnot.setText("publ");
        fifthAnnot.setUser(firstUser);
        annotRepos.save(fifthAnnot);

        // sixth annotation of first user: private!
        Annotation sixthAnnot = new Annotation();
        sixthAnnot.setId(ID_ANNOT_FIRSTUSER_6_PRIV);
        sixthAnnot.setCreated(LocalDateTime.of(2017, 12, 27, 8, 27, 55));
        sixthAnnot.setUpdated(LocalDateTime.of(2017, 12, 27, 8, 28, 38));
        sixthAnnot.setDocument(firstDoc);
        sixthAnnot.setGroup(defaultGroup);
        sixthAnnot.setMetadata(meta);
        sixthAnnot.setReferences("");
        sixthAnnot.setShared(false);
        sixthAnnot.setTargetSelectors(dummySelector);
        sixthAnnot.setText("qas");
        sixthAnnot.setUser(firstUser);
        annotRepos.save(sixthAnnot);

        // seventh annotation of first user: private!
        Annotation seventhAnnot = new Annotation();
        seventhAnnot.setId(ID_ANNOT_FIRSTUSER_7_PRIV);
        seventhAnnot.setCreated(LocalDateTime.of(2017, 12, 27, 8, 54, 40));
        seventhAnnot.setUpdated(LocalDateTime.of(2017, 12, 27, 8, 55, 17));
        seventhAnnot.setDocument(firstDoc);
        seventhAnnot.setGroup(defaultGroup);
        seventhAnnot.setMetadata(meta);
        seventhAnnot.setReferences("");
        seventhAnnot.setShared(false);
        seventhAnnot.setTargetSelectors(dummySelector);
        seventhAnnot.setText("abcde");
        seventhAnnot.setUser(firstUser);
        annotRepos.save(seventhAnnot);

        // eighth annotation of first user: private!
        Annotation eighthAnnot = new Annotation();
        eighthAnnot.setId(ID_ANNOT_FIRSTUSER_8_PRIV);
        eighthAnnot.setCreated(LocalDateTime.of(2017, 12, 27, 9, 11, 21));
        eighthAnnot.setUpdated(LocalDateTime.of(2017, 12, 27, 9, 11, 35));
        eighthAnnot.setDocument(firstDoc);
        eighthAnnot.setGroup(defaultGroup);
        eighthAnnot.setMetadata(meta);
        eighthAnnot.setReferences("");
        eighthAnnot.setShared(false);
        eighthAnnot.setTargetSelectors(dummySelector);
        eighthAnnot.setText("bcdef");
        eighthAnnot.setUser(firstUser);
        annotRepos.save(eighthAnnot);

        // ninth annotation of first user: private!
        Annotation ninthAnnot = new Annotation();
        ninthAnnot.setId(ID_ANNOT_FIRSTUSER_9_PRIV);
        ninthAnnot.setCreated(LocalDateTime.of(2017, 12, 27, 9, 12, 23));
        ninthAnnot.setUpdated(LocalDateTime.of(2017, 12, 27, 9, 13, 37));
        ninthAnnot.setDocument(firstDoc);
        ninthAnnot.setGroup(defaultGroup);
        ninthAnnot.setMetadata(meta);
        ninthAnnot.setReferences("");
        ninthAnnot.setShared(false);
        ninthAnnot.setTargetSelectors(dummySelector);
        ninthAnnot.setText("cdefg");
        ninthAnnot.setUser(firstUser);
        annotRepos.save(ninthAnnot);

        // tenth annotation of first user: public!
        Annotation tenthAnnot = new Annotation();
        tenthAnnot.setId(ID_ANNOT_FIRSTUSER_10_PUB);
        tenthAnnot.setCreated(LocalDateTime.of(2017, 12, 27, 9, 44, 39));
        tenthAnnot.setUpdated(LocalDateTime.of(2017, 12, 27, 9, 45, 06));
        tenthAnnot.setDocument(firstDoc);
        tenthAnnot.setGroup(defaultGroup);
        tenthAnnot.setMetadata(meta);
        tenthAnnot.setReferences("");
        tenthAnnot.setShared(true);
        tenthAnnot.setTargetSelectors(dummySelector);
        tenthAnnot.setText("tagtest");
        tenthAnnot.setUser(firstUser);
        annotRepos.save(tenthAnnot);

        // eleventh annotation of first user: public!
        Annotation eleventhAnnot = new Annotation();
        eleventhAnnot.setId(ID_ANNOT_FIRSTUSER_11_PUB);
        eleventhAnnot.setCreated(LocalDateTime.of(2017, 12, 27, 10, 52, 36));
        eleventhAnnot.setUpdated(LocalDateTime.of(2018, 01, 18, 16, 49, 20));
        eleventhAnnot.setDocument(firstDoc);
        eleventhAnnot.setGroup(defaultGroup);
        eleventhAnnot.setMetadata(meta);
        eleventhAnnot.setReferences("");
        eleventhAnnot.setShared(true);
        eleventhAnnot.setTargetSelectors(dummySelector);
        eleventhAnnot.setText("reg! changed");
        eleventhAnnot.setUser(firstUser);
        annotRepos.save(eleventhAnnot);

        // twelfth annotation of first user: public!
        Annotation twelfthAnnot = new Annotation();
        twelfthAnnot.setId(ID_ANNOT_FIRSTUSER_12_PUB);
        twelfthAnnot.setCreated(LocalDateTime.of(2017, 12, 27, 12, 28, 14));
        twelfthAnnot.setUpdated(LocalDateTime.of(2017, 12, 27, 12, 28, 52));
        twelfthAnnot.setDocument(firstDoc);
        twelfthAnnot.setGroup(defaultGroup);
        twelfthAnnot.setMetadata(meta);
        twelfthAnnot.setReferences("");
        twelfthAnnot.setShared(true);
        twelfthAnnot.setTargetSelectors(dummySelector);
        twelfthAnnot.setText("viel *Text*");
        twelfthAnnot.setUser(firstUser);
        annotRepos.save(twelfthAnnot);

        // thirteenth annotation of first user: public!
        Annotation thirteenthAnnot = new Annotation();
        thirteenthAnnot.setId(ID_ANNOT_FIRSTUSER_13_PUB);
        thirteenthAnnot.setCreated(LocalDateTime.of(2017, 12, 28, 9, 49, 43));
        thirteenthAnnot.setUpdated(LocalDateTime.of(2017, 12, 28, 9, 49, 51));
        thirteenthAnnot.setDocument(firstDoc);
        thirteenthAnnot.setMetadata(meta);
        thirteenthAnnot.setGroup(defaultGroup);
        thirteenthAnnot.setReferences("");
        thirteenthAnnot.setShared(true);
        thirteenthAnnot.setTargetSelectors(dummySelector);
        thirteenthAnnot.setText("new annot");
        thirteenthAnnot.setUser(firstUser);
        annotRepos.save(thirteenthAnnot);

        // fourteenth annotation of first user: public!
        Annotation fourteenthAnnot = new Annotation();
        fourteenthAnnot.setId(ID_ANNOT_FIRSTUSER_14_PUB);
        fourteenthAnnot.setCreated(LocalDateTime.of(2017, 12, 28, 10, 42, 07));
        fourteenthAnnot.setUpdated(LocalDateTime.of(2017, 12, 28, 10, 42, 16));
        fourteenthAnnot.setDocument(firstDoc);
        fourteenthAnnot.setGroup(defaultGroup);
        fourteenthAnnot.setMetadata(meta);
        fourteenthAnnot.setReferences("");
        fourteenthAnnot.setShared(true);
        fourteenthAnnot.setTargetSelectors(dummySelector);
        fourteenthAnnot.setText("win10");
        fourteenthAnnot.setUser(firstUser);
        annotRepos.save(fourteenthAnnot);

        // fifteenth annotation of first user: public!
        Annotation fifteenthAnnot = new Annotation();
        fifteenthAnnot.setId(ID_ANNOT_FIRSTUSER_15_PUB);
        fifteenthAnnot.setCreated(LocalDateTime.of(2017, 12, 28, 12, 38, 18));
        fifteenthAnnot.setUpdated(LocalDateTime.of(2017, 12, 28, 12, 40, 36));
        fifteenthAnnot.setDocument(firstDoc);
        fifteenthAnnot.setGroup(defaultGroup);
        fifteenthAnnot.setMetadata(meta);
        fifteenthAnnot.setReferences("");
        fifteenthAnnot.setShared(true);
        fifteenthAnnot.setTargetSelectors(dummySelector);
        fifteenthAnnot.setText("selectortest");
        fifteenthAnnot.setUser(firstUser);
        annotRepos.save(fifteenthAnnot);

        // sixteenth annotation of first user: public!
        Annotation sixteenthAnnot = new Annotation();
        sixteenthAnnot.setId(ID_ANNOT_FIRSTUSER_16_PUB);
        sixteenthAnnot.setCreated(LocalDateTime.of(2017, 12, 28, 15, 05, 21));
        sixteenthAnnot.setUpdated(LocalDateTime.of(2017, 12, 28, 15, 05, 41));
        sixteenthAnnot.setDocument(firstDoc);
        sixteenthAnnot.setGroup(defaultGroup);
        sixteenthAnnot.setMetadata(meta);
        sixteenthAnnot.setReferences("");
        sixteenthAnnot.setShared(true);
        sixteenthAnnot.setTargetSelectors(dummySelector);
        sixteenthAnnot.setText("text on text");
        sixteenthAnnot.setUser(firstUser);
        annotRepos.save(sixteenthAnnot);

        // seventeenth annotation of first user: public!
        Annotation seventeenthAnnot = new Annotation();
        seventeenthAnnot.setId(ID_ANNOT_FIRSTUSER_17_PUB);
        seventeenthAnnot.setCreated(LocalDateTime.of(2017, 12, 29, 10, 42, 20));
        seventeenthAnnot.setUpdated(LocalDateTime.of(2017, 12, 29, 10, 42, 40));
        seventeenthAnnot.setDocument(firstDoc);
        seventeenthAnnot.setGroup(defaultGroup);
        seventeenthAnnot.setMetadata(meta);
        seventeenthAnnot.setReferences("");
        seventeenthAnnot.setShared(true);
        seventeenthAnnot.setTargetSelectors(dummySelector);
        seventeenthAnnot.setText("new comment");
        seventeenthAnnot.setUser(firstUser);
        annotRepos.save(seventeenthAnnot);

        // eighteenth annotation of first user: public!
        Annotation eighteenthAnnot = new Annotation();
        eighteenthAnnot.setId(ID_ANNOT_FIRSTUSER_18_PUB);
        eighteenthAnnot.setCreated(LocalDateTime.of(2017, 12, 29, 10, 50, 42));
        eighteenthAnnot.setUpdated(LocalDateTime.of(2017, 12, 29, 10, 56, 28));
        eighteenthAnnot.setDocument(firstDoc);
        eighteenthAnnot.setGroup(defaultGroup);
        eighteenthAnnot.setMetadata(meta);
        eighteenthAnnot.setReferences("");
        eighteenthAnnot.setShared(true);
        eighteenthAnnot.setTargetSelectors(dummySelector);
        eighteenthAnnot.setText("parent");
        eighteenthAnnot.setUser(firstUser);
        annotRepos.save(eighteenthAnnot);

        // nineteenth annotation of first user: public reply (2nd degree)!
        Annotation nineteenthAnnot = new Annotation();
        nineteenthAnnot.setId(ID_ANNOT_FIRSTUSER_19_PUB_REPLY);
        nineteenthAnnot.setCreated(LocalDateTime.of(2017, 12, 29, 10, 59, 01));
        nineteenthAnnot.setUpdated(LocalDateTime.of(2017, 12, 29, 10, 59, 32));
        nineteenthAnnot.setDocument(firstDoc);
        nineteenthAnnot.setGroup(defaultGroup);
        nineteenthAnnot.setMetadata(meta);
        nineteenthAnnot.setReferences(Arrays.asList(ID_ANNOT_FIRSTUSER_18_PUB, ID_ANNOT_FIRSTUSER_19_PUB_REPLY_DELETED));
        nineteenthAnnot.setShared(true);
        nineteenthAnnot.setTargetSelectors(dummySelector);
        nineteenthAnnot.setText("child of child");
        nineteenthAnnot.setUser(firstUser);
        annotRepos.save(nineteenthAnnot);

        // twentieth annotation of first user: public!
        Annotation twentiethAnnot = new Annotation();
        twentiethAnnot.setId(ID_ANNOT_FIRSTUSER_20_PUB);
        twentiethAnnot.setCreated(LocalDateTime.of(2017, 12, 29, 13, 06, 54));
        twentiethAnnot.setUpdated(LocalDateTime.of(2017, 12, 29, 13, 13, 19));
        twentiethAnnot.setDocument(firstDoc);
        twentiethAnnot.setGroup(defaultGroup);
        twentiethAnnot.setMetadata(meta);
        twentiethAnnot.setReferences("");
        twentiethAnnot.setShared(true);
        twentiethAnnot.setTargetSelectors(dummySelector);
        twentiethAnnot.setText("new root comment");
        twentiethAnnot.setUser(firstUser);
        annotRepos.save(twentiethAnnot);

        // twentyfirst annotation of first user: public reply!
        Annotation twentyfirstAnnot = new Annotation();
        twentyfirstAnnot.setId(ID_ANNOT_FIRSTUSER_21_PUB_REPLY);
        twentyfirstAnnot.setCreated(LocalDateTime.of(2017, 12, 29, 13, 13, 49));
        twentyfirstAnnot.setUpdated(LocalDateTime.of(2017, 12, 29, 13, 14, 27));
        twentyfirstAnnot.setDocument(firstDoc);
        twentyfirstAnnot.setGroup(defaultGroup);
        twentyfirstAnnot.setMetadata(meta);
        twentyfirstAnnot.setReferences(ID_ANNOT_FIRSTUSER_20_PUB);
        twentyfirstAnnot.setShared(true);
        twentyfirstAnnot.setTargetSelectors(dummySelector);
        twentyfirstAnnot.setText("first reply to root");
        twentyfirstAnnot.setUser(firstUser);
        annotRepos.save(twentyfirstAnnot);

        // twentysecond annotation of first user: public reply!
        Annotation twentysecondAnnot = new Annotation();
        twentysecondAnnot.setId(ID_ANNOT_FIRSTUSER_22_PUB_REPLY);
        twentysecondAnnot.setCreated(LocalDateTime.of(2017, 12, 29, 13, 14, 48));
        twentysecondAnnot.setUpdated(LocalDateTime.of(2017, 12, 29, 13, 15, 9));
        twentysecondAnnot.setDocument(firstDoc);
        twentysecondAnnot.setGroup(defaultGroup);
        twentysecondAnnot.setMetadata(meta);
        twentysecondAnnot.setReferences(ID_ANNOT_FIRSTUSER_20_PUB);
        twentysecondAnnot.setShared(true);
        twentysecondAnnot.setTargetSelectors(dummySelector);
        twentysecondAnnot.setText("second reply to root");
        twentysecondAnnot.setUser(firstUser);
        annotRepos.save(twentysecondAnnot);

        // twentythird annotation of first user: public reply!
        Annotation twentythirdAnnot = new Annotation();
        twentythirdAnnot.setId(ID_ANNOT_FIRSTUSER_23_PUB_REPLY);
        twentythirdAnnot.setCreated(LocalDateTime.of(2017, 12, 29, 13, 15, 28));
        twentythirdAnnot.setUpdated(LocalDateTime.of(2017, 12, 29, 13, 16, 01));
        twentythirdAnnot.setDocument(firstDoc);
        twentythirdAnnot.setGroup(defaultGroup);
        twentythirdAnnot.setMetadata(meta);
        twentythirdAnnot.setReferences(Arrays.asList(ID_ANNOT_FIRSTUSER_20_PUB, ID_ANNOT_FIRSTUSER_21_PUB_REPLY));
        twentythirdAnnot.setShared(true);
        twentythirdAnnot.setTargetSelectors(dummySelector);
        twentythirdAnnot.setText("first reply *to *first reply");
        twentythirdAnnot.setUser(firstUser);
        annotRepos.save(twentythirdAnnot);

        // twentyfourth annotation of first user: private!
        Annotation twentyfourthAnnot = new Annotation();
        twentyfourthAnnot.setId(ID_ANNOT_FIRSTUSER_24_PRIV);
        twentyfourthAnnot.setCreated(LocalDateTime.of(2018, 01, 3, 8, 44, 48));
        twentyfourthAnnot.setUpdated(LocalDateTime.of(2018, 01, 3, 8, 45, 05));
        twentyfourthAnnot.setDocument(firstDoc);
        twentyfourthAnnot.setGroup(defaultGroup);
        twentyfourthAnnot.setMetadata(meta);
        twentyfourthAnnot.setReferences("");
        twentyfourthAnnot.setShared(false);
        twentyfourthAnnot.setTargetSelectors(dummySelector);
        twentyfourthAnnot.setText("hallo");
        twentyfourthAnnot.setUser(firstUser);
        annotRepos.save(twentyfourthAnnot);

        // twentyfifth annotation of first user: private!
        Annotation twentyfifthAnnot = new Annotation();
        twentyfifthAnnot.setId(ID_ANNOT_FIRSTUSER_25_PRIV);
        twentyfifthAnnot.setCreated(LocalDateTime.of(2018, 01, 9, 14, 9, 20));
        twentyfifthAnnot.setUpdated(LocalDateTime.of(2018, 01, 10, 12, 52, 28));
        twentyfifthAnnot.setDocument(firstDoc);
        twentyfifthAnnot.setGroup(defaultGroup);
        twentyfifthAnnot.setMetadata(meta);
        twentyfifthAnnot.setReferences("");
        twentyfifthAnnot.setShared(false);
        twentyfifthAnnot.setTargetSelectors(dummySelector);
        twentyfifthAnnot.setText("a new annotation");
        twentyfifthAnnot.setUser(firstUser);
        annotRepos.save(twentyfifthAnnot);

        // twentysixth annotation of second user: private!
        Annotation twentysixthAnnot = new Annotation();
        twentysixthAnnot.setId(ID_ANNOT_SECONDUSER_26_PUB);
        twentysixthAnnot.setCreated(LocalDateTime.of(2018, 01, 10, 12, 58, 00));
        twentysixthAnnot.setUpdated(LocalDateTime.of(2018, 01, 11, 10, 18, 53));
        twentysixthAnnot.setDocument(firstDoc);
        twentysixthAnnot.setGroup(defaultGroup);
        twentysixthAnnot.setMetadata(meta);
        twentysixthAnnot.setReferences("");
        twentysixthAnnot.setShared(true);
        twentysixthAnnot.setTargetSelectors(dummySelector);
        twentysixthAnnot.setText("my ann2aAA");
        twentysixthAnnot.setUser(secondUser);
        annotRepos.save(twentysixthAnnot);

        // twentyseventh annotation of first user: private page note!
        Annotation twentyseventhAnnot = new Annotation();
        twentyseventhAnnot.setId(ID_ANNOT_FIRSTUSER_27_PRIV_PAGENOTE);
        twentyseventhAnnot.setCreated(LocalDateTime.of(2018, 01, 11, 11, 30, 05));
        twentyseventhAnnot.setUpdated(LocalDateTime.of(2018, 01, 11, 11, 30, 59));
        twentyseventhAnnot.setDocument(firstDoc);
        twentyseventhAnnot.setGroup(defaultGroup);
        twentyseventhAnnot.setMetadata(meta);
        twentyseventhAnnot.setReferences("");
        twentyseventhAnnot.setShared(false);
        twentyseventhAnnot.setTargetSelectors(dummySelector);
        twentyseventhAnnot.setText("This is my _updated_ page note");
        twentyseventhAnnot.setUser(firstUser);
        annotRepos.save(twentyseventhAnnot);

        // twentyeighth annotation of first user: public reply to private page note!
        Annotation twentyeighthAnnot = new Annotation();
        twentyeighthAnnot.setId(ID_ANNOT_FIRSTUSER_28_PUB_REPLY_PAGENOTE);
        twentyeighthAnnot.setCreated(LocalDateTime.of(2018, 01, 11, 11, 31, 25));
        twentyeighthAnnot.setUpdated(LocalDateTime.of(2018, 01, 11, 11, 31, 50));
        twentyeighthAnnot.setDocument(firstDoc);
        twentyeighthAnnot.setGroup(defaultGroup);
        twentyeighthAnnot.setMetadata(meta);
        twentyeighthAnnot.setReferences(ID_ANNOT_FIRSTUSER_27_PRIV_PAGENOTE);
        twentyeighthAnnot.setShared(true);
        twentyeighthAnnot.setTargetSelectors(dummySelector);
        twentyeighthAnnot.setText("reply to page note (public)");
        twentyeighthAnnot.setUser(firstUser);
        annotRepos.save(twentyeighthAnnot);

        // twentyninth annotation of second user: private highlight!
        Annotation twentyninthAnnot = new Annotation();
        twentyninthAnnot.setId(ID_ANNOT_SECONDUSER_29_PRIV_HIGHLIGHT);
        twentyninthAnnot.setCreated(LocalDateTime.of(2018, 01, 11, 11, 32, 25));
        twentyninthAnnot.setUpdated(LocalDateTime.of(2018, 01, 11, 11, 32, 25));
        twentyninthAnnot.setDocument(firstDoc);
        twentyninthAnnot.setGroup(defaultGroup);
        twentyninthAnnot.setMetadata(meta);
        twentyninthAnnot.setReferences("");
        twentyninthAnnot.setShared(false);
        twentyninthAnnot.setTargetSelectors(dummySelector);
        twentyninthAnnot.setText(null);
        twentyninthAnnot.setUser(secondUser);
        annotRepos.save(twentyninthAnnot);

        // thirtyth annotation of first user: public page note!
        Annotation thirtythAnnot = new Annotation();
        thirtythAnnot.setId(ID_ANNOT_FIRSTUSER_30_PUB_PAGENOTE);
        thirtythAnnot.setCreated(LocalDateTime.of(2018, 01, 11, 14, 01, 34));
        thirtythAnnot.setUpdated(LocalDateTime.of(2018, 01, 11, 14, 01, 58));
        thirtythAnnot.setDocument(firstDoc);
        thirtythAnnot.setGroup(defaultGroup);
        thirtythAnnot.setMetadata(meta);
        thirtythAnnot.setReferences("");
        thirtythAnnot.setShared(true);
        thirtythAnnot.setTargetSelectors(dummySelector);
        thirtythAnnot.setText("new page note");
        thirtythAnnot.setUser(secondUser);
        annotRepos.save(thirtythAnnot);

        // thirtyfirst annotation of first user: public
        Annotation thirtyfirstAnnot = new Annotation();
        thirtyfirstAnnot.setId(ID_ANNOT_FIRSTUSER_31_PUB);
        thirtyfirstAnnot.setCreated(LocalDateTime.of(2018, 01, 11, 16, 05, 23));
        thirtyfirstAnnot.setUpdated(LocalDateTime.of(2018, 01, 11, 16, 05, 42));
        thirtyfirstAnnot.setDocument(firstDoc);
        thirtyfirstAnnot.setGroup(defaultGroup);
        thirtyfirstAnnot.setMetadata(meta);
        thirtyfirstAnnot.setReferences("");
        thirtyfirstAnnot.setShared(true);
        thirtyfirstAnnot.setTargetSelectors(dummySelector);
        thirtyfirstAnnot.setText("test new annot");
        thirtyfirstAnnot.setUser(secondUser);
        annotRepos.save(thirtyfirstAnnot);

        // thirtysecond annotation of first user: public
        Annotation thirtysecondAnnot = new Annotation();
        thirtysecondAnnot.setId(ID_ANNOT_FIRSTUSER_32_PUB);
        thirtysecondAnnot.setCreated(LocalDateTime.of(2018, 01, 24, 18, 50, 06));
        thirtysecondAnnot.setUpdated(LocalDateTime.of(2018, 01, 24, 18, 50, 11));
        thirtysecondAnnot.setDocument(firstDoc);
        thirtysecondAnnot.setGroup(defaultGroup);
        thirtysecondAnnot.setMetadata(meta);
        thirtysecondAnnot.setReferences("");
        thirtysecondAnnot.setShared(true);
        thirtysecondAnnot.setTargetSelectors(dummySelector);
        thirtysecondAnnot.setText("XZCdfsa");
        thirtysecondAnnot.setUser(secondUser);
        annotRepos.save(thirtysecondAnnot);

        // thirtythird annotation of first user: private
        Annotation thirtythirdAnnot = new Annotation();
        thirtythirdAnnot.setId(ID_ANNOT_FIRSTUSER_33_PRIV);
        thirtythirdAnnot.setCreated(LocalDateTime.of(2018, 01, 25, 18, 38, 03));
        thirtythirdAnnot.setUpdated(LocalDateTime.of(2018, 01, 25, 18, 38, 06));
        thirtythirdAnnot.setDocument(firstDoc);
        thirtythirdAnnot.setGroup(defaultGroup);
        thirtythirdAnnot.setMetadata(meta);
        thirtythirdAnnot.setReferences("");
        thirtythirdAnnot.setShared(false);
        thirtythirdAnnot.setTargetSelectors(dummySelector);
        thirtythirdAnnot.setText("new ann");
        thirtythirdAnnot.setUser(firstUser);
        annotRepos.save(thirtythirdAnnot);

        // thirtyfourth annotation of third user: public
        Annotation thirtyfourthAnnot = new Annotation();
        thirtyfourthAnnot.setId(ID_ANNOT_THIRDUSER_34_PUB);
        thirtyfourthAnnot.setCreated(LocalDateTime.of(2018, 01, 26, 9, 35, 19));
        thirtyfourthAnnot.setUpdated(LocalDateTime.of(2018, 01, 26, 9, 35, 23));
        thirtyfourthAnnot.setDocument(firstDoc);
        thirtyfourthAnnot.setGroup(defaultGroup);
        thirtyfourthAnnot.setMetadata(meta);
        thirtyfourthAnnot.setReferences("");
        thirtyfourthAnnot.setShared(true);
        thirtyfourthAnnot.setTargetSelectors(dummySelector);
        thirtyfourthAnnot.setText("darth2");
        thirtyfourthAnnot.setUser(thirdUser);
        annotRepos.save(thirtyfourthAnnot);

        // thirtyfifth annotation of third user: public reply
        Annotation thirtyfifthAnnot = new Annotation();
        thirtyfifthAnnot.setId(ID_ANNOT_THIRDUSER_35_PUB_REPLY);
        thirtyfifthAnnot.setCreated(LocalDateTime.of(2018, 01, 26, 10, 01, 36));
        thirtyfifthAnnot.setUpdated(LocalDateTime.of(2018, 01, 26, 10, 01, 41));
        thirtyfifthAnnot.setDocument(firstDoc);
        thirtyfifthAnnot.setGroup(defaultGroup);
        thirtyfifthAnnot.setMetadata(meta);
        thirtyfifthAnnot.setReferences(ID_ANNOT_SECONDUSER_26_PUB);
        thirtyfifthAnnot.setShared(true);
        thirtyfifthAnnot.setTargetSelectors(dummySelector);
        thirtyfifthAnnot.setText("reply darth");
        thirtyfifthAnnot.setUser(thirdUser);
        annotRepos.save(thirtyfifthAnnot);

        // thirtysixth annotation of third user: public
        Annotation thirtysixthAnnot = new Annotation();
        thirtysixthAnnot.setId(ID_ANNOT_THIRDUSER_36_PUB);
        thirtysixthAnnot.setCreated(LocalDateTime.of(2018, 01, 26, 10, 04, 53));
        thirtysixthAnnot.setUpdated(LocalDateTime.of(2018, 01, 26, 10, 05, 01));
        thirtysixthAnnot.setDocument(firstDoc);
        thirtysixthAnnot.setGroup(defaultGroup);
        thirtysixthAnnot.setMetadata(meta);
        thirtysixthAnnot.setReferences("");
        thirtysixthAnnot.setShared(true);
        thirtysixthAnnot.setTargetSelectors(dummySelector);
        thirtysixthAnnot.setText("updated!");
        thirtysixthAnnot.setUser(thirdUser);
        annotRepos.save(thirtysixthAnnot);

    }

    @After
    public void cleanDatabaseAfterTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * test: get first slice of 10 items, check correct received items and replies by their IDs
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testFirstBunchOfTenItems() {

        // retrieve first 10 annotations
        AnnotationSearchOptions options = new AnnotationSearchOptions(
                DOC_URL, "__world__", // URI, group
                true,                 // provide separate replies
                10, 0,                // limit, offset
                "asc", "created");    // order, sort column

        List<Annotation> annotations = annotService.searchAnnotations(options, USER_LOGIN);
        Assert.assertEquals(10, annotations.size());

        List<Annotation> replies = annotService.searchRepliesForAnnotations(annotations, options, USER_LOGIN);
        Assert.assertEquals(0, replies.size());

        JsonSearchResult initialResult = annotService.convertToJsonSearchResult(annotations, replies, options);
        Assert.assertTrue(initialResult instanceof JsonSearchResultWithSeparateReplies);
        JsonSearchResultWithSeparateReplies result = (JsonSearchResultWithSeparateReplies) initialResult;

        Assert.assertNotNull(result);
        Assert.assertEquals(10, result.getRows().size());   // 10 items expected
        Assert.assertEquals(0, result.getReplies().size()); // and 0 replies

        // check that the correct items (in correct order) were returned and check their respective number of replies
        assertItemAndReplies(result.getRows().get(0), ID_ANNOT_FIRSTUSER_1_PUB, null);
        assertItemAndReplies(result.getRows().get(1), ID_ANNOT_FIRSTUSER_2_PUB, null);
        assertItemAndReplies(result.getRows().get(2), ID_ANNOT_FIRSTUSER_3_PUB, null);
        assertItemAndReplies(result.getRows().get(3), ID_ANNOT_FIRSTUSER_4_PUB, null);
        assertItemAndReplies(result.getRows().get(4), ID_ANNOT_FIRSTUSER_5_PRIV, null);
        assertItemAndReplies(result.getRows().get(5), ID_ANNOT_FIRSTUSER_6_PRIV, null);
        assertItemAndReplies(result.getRows().get(6), ID_ANNOT_FIRSTUSER_7_PRIV, null);
        assertItemAndReplies(result.getRows().get(7), ID_ANNOT_FIRSTUSER_8_PRIV, null);
        assertItemAndReplies(result.getRows().get(8), ID_ANNOT_FIRSTUSER_9_PRIV, null);
        assertItemAndReplies(result.getRows().get(9), ID_ANNOT_FIRSTUSER_10_PUB, null);
    }

    /**
     * test: get second slice of 10 items, check correct received items and replies by their IDs
     */
    @Test
    public void testSecondBunchOfTenItems() {

        // retrieve second 10 annotations
        AnnotationSearchOptions options = new AnnotationSearchOptions(
                DOC_URL, "__world__", // URI, group
                true,                 // provide separate replies
                10, 10,               // limit, offset (10/10 -> second bunch of ten items)
                "asc", "created");    // order, sort column

        List<Annotation> items = annotService.searchAnnotations(options, USER_LOGIN);
        Assert.assertEquals(10, items.size());

        List<Annotation> replies = annotService.searchRepliesForAnnotations(items, options, USER_LOGIN);
        Assert.assertEquals(4, replies.size());

        JsonSearchResult initialResult = annotService.convertToJsonSearchResult(items, replies, options);
        Assert.assertTrue(initialResult instanceof JsonSearchResultWithSeparateReplies);

        JsonSearchResultWithSeparateReplies result = (JsonSearchResultWithSeparateReplies) initialResult;
        Assert.assertNotNull(result);
        Assert.assertEquals(10, result.getRows().size());   // 10 items expected
        Assert.assertEquals(4, result.getReplies().size()); // and 4 replies

        // check that the correct items (in correct order) were returned and check their respective number of replies
        assertItemAndReplies(result.getRows().get(0), ID_ANNOT_FIRSTUSER_11_PUB, null);
        assertItemAndReplies(result.getRows().get(1), ID_ANNOT_FIRSTUSER_12_PUB, null);
        assertItemAndReplies(result.getRows().get(2), ID_ANNOT_FIRSTUSER_13_PUB, null);
        assertItemAndReplies(result.getRows().get(3), ID_ANNOT_FIRSTUSER_14_PUB, null);
        assertItemAndReplies(result.getRows().get(4), ID_ANNOT_FIRSTUSER_15_PUB, null);
        assertItemAndReplies(result.getRows().get(5), ID_ANNOT_FIRSTUSER_16_PUB, null);
        assertItemAndReplies(result.getRows().get(6), ID_ANNOT_FIRSTUSER_17_PUB, null);
        assertItemAndReplies(result.getRows().get(7), ID_ANNOT_FIRSTUSER_18_PUB, null);
        // 19 skipped, is a reply
        assertItemAndReplies(result.getRows().get(8), ID_ANNOT_FIRSTUSER_20_PUB, null);
        // 21-23 skipped, are replies
        assertItemAndReplies(result.getRows().get(9), ID_ANNOT_FIRSTUSER_24_PRIV, null);

        // same for the replies
        assertItemAndReplies(result.getReplies().get(0), ID_ANNOT_FIRSTUSER_19_PUB_REPLY,
                Arrays.asList(ID_ANNOT_FIRSTUSER_18_PUB, ID_ANNOT_FIRSTUSER_19_PUB_REPLY_DELETED));
        assertItemAndReplies(result.getReplies().get(1), ID_ANNOT_FIRSTUSER_21_PUB_REPLY, Arrays.asList(ID_ANNOT_FIRSTUSER_20_PUB));
        assertItemAndReplies(result.getReplies().get(2), ID_ANNOT_FIRSTUSER_22_PUB_REPLY, Arrays.asList(ID_ANNOT_FIRSTUSER_20_PUB));
        assertItemAndReplies(result.getReplies().get(3), ID_ANNOT_FIRSTUSER_23_PUB_REPLY,
                Arrays.asList(ID_ANNOT_FIRSTUSER_20_PUB, ID_ANNOT_FIRSTUSER_21_PUB_REPLY));
    }

    /**
     * test: get third slice of 10 items, check correct received items and replies by their IDs
     */
    @Test
    public void testThirdBunchOfTenItems() {

        // retrieve third 10 annotations
        AnnotationSearchOptions options = new AnnotationSearchOptions(
                DOC_URL, "__world__", // URI, group
                true,                 // provide separate replies
                10, 20,               // limit, offset (10/20 -> third bunch of ten items)
                "asc", "created");    // order, sort column

        List<Annotation> items = annotService.searchAnnotations(options, USER_LOGIN);
        Assert.assertEquals(9, items.size());

        List<Annotation> replies = annotService.searchRepliesForAnnotations(items, options, USER_LOGIN);
        Assert.assertEquals(2, replies.size());

        JsonSearchResult initialResult = annotService.convertToJsonSearchResult(items, replies, options);
        Assert.assertTrue(initialResult instanceof JsonSearchResultWithSeparateReplies);

        JsonSearchResultWithSeparateReplies result = (JsonSearchResultWithSeparateReplies) initialResult;

        Assert.assertNotNull(result);
        Assert.assertEquals(9, result.getRows().size());    // 9 annotations expected
        Assert.assertEquals(2, result.getReplies().size()); // and 1 reply

        // check that the correct items (in correct order) were returned and check their respective number of replies
        assertItemAndReplies(result.getRows().get(0), ID_ANNOT_FIRSTUSER_25_PRIV, null);
        assertItemAndReplies(result.getRows().get(1), ID_ANNOT_SECONDUSER_26_PUB, null);
        assertItemAndReplies(result.getRows().get(2), ID_ANNOT_FIRSTUSER_27_PRIV_PAGENOTE, null);
        // 28 skipped, it is a reply to a page note
        // 29 skipped, it is a private highlight of another user
        assertItemAndReplies(result.getRows().get(3), ID_ANNOT_FIRSTUSER_30_PUB_PAGENOTE, null);
        assertItemAndReplies(result.getRows().get(4), ID_ANNOT_FIRSTUSER_31_PUB, null);
        assertItemAndReplies(result.getRows().get(5), ID_ANNOT_FIRSTUSER_32_PUB, null);
        assertItemAndReplies(result.getRows().get(6), ID_ANNOT_FIRSTUSER_33_PRIV, null);
        assertItemAndReplies(result.getRows().get(7), ID_ANNOT_THIRDUSER_34_PUB, null);
        // 35 skipped, is a reply
        assertItemAndReplies(result.getRows().get(8), ID_ANNOT_THIRDUSER_36_PUB, null);

        // same for the replies
        assertItemAndReplies(result.getReplies().get(0), ID_ANNOT_FIRSTUSER_28_PUB_REPLY_PAGENOTE, Arrays.asList(ID_ANNOT_FIRSTUSER_27_PRIV_PAGENOTE));
        assertItemAndReplies(result.getReplies().get(1), ID_ANNOT_THIRDUSER_35_PUB_REPLY, Arrays.asList(ID_ANNOT_SECONDUSER_26_PUB));
    }

    /**
     * test: get fourth slice of 10 items - there shouldn't be one
     */
    @Test
    public void testFourthBunchOfTenItems() {

        // retrieve third 10 annotations
        AnnotationSearchOptions options = new AnnotationSearchOptions(
                DOC_URL, "__world__", // URI, group
                true,                 // provide separate replies
                10, 30,               // limit, offset (10/30 -> fourth bunch of ten items)
                "asc", "created");    // order, sort column

        // no items found!
        List<Annotation> items = annotService.searchAnnotations(options, USER_LOGIN);
        Assert.assertEquals(0, items.size());

        List<Annotation> replies = annotService.searchRepliesForAnnotations(items, options, USER_LOGIN);
        Assert.assertEquals(0, replies.size());
    }

    private void assertItemAndReplies(JsonAnnotation annot, String itemId, List<String> replyParentIds) {

        Assert.assertNotNull(annot);
        Assert.assertEquals(itemId, annot.getId());
        if (replyParentIds == null) {
            Assert.assertNull(annot.getReferences());
        } else {
            Assert.assertArrayEquals(replyParentIds.toArray(), annot.getReferences().toArray());
        }
    }
}
