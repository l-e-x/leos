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

import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Tag;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.TagRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateAnnotationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class TagsServiceTest {

    private User user;
    
    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private TagsService tagsService;

    @Autowired
    private AnnotationService annotService;

    @Autowired
    private TagRepository tagRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
        TestDbHelper.insertDefaultGroup(groupRepos);
        user = new User("demo");
        userRepos.save(user);
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------
    /**
     * test the conversion of a list of strings to Tags objects
     */
    @Test
    public void testStringsToTags() {

        final Annotation annot = new Annotation();

        // generate a list of a random number of strings representing tags
        final java.util.Random rand = new java.util.Random();
        final int numberOfTags = rand.nextInt(100) + 1;

        final List<String> tagStrings = new ArrayList<String>();
        for (int i = 0; i < numberOfTags; i++) {
            tagStrings.add("tag" + i);
        }

        // let the list be generated to tag objects associated to a given annotation
        final List<Tag> tagList = tagsService.getTagList(tagStrings, annot);

        // verify assignment to annotation and that the tag is part of the initial list
        Assert.assertNotNull(tagList);
        Assert.assertEquals(numberOfTags, tagList.size());

        for (final Tag t : tagList) {
            Assert.assertEquals(annot, t.getAnnotation());
            Assert.assertTrue(tagStrings.contains(t.getName()));
        }

        // retrieve all tag names generated
        final List<String> generatedTagNames = new ArrayList<String>();
        tagList.stream().forEach(tag -> generatedTagNames.add(tag.getName()));

        // check if this list is identical to the original list
        Assert.assertTrue(generatedTagNames.removeAll(tagStrings));
        Assert.assertEquals(0, generatedTagNames.size());
    }

    /**
     * test that empty list of tag Strings does not produce any tags
     */
    @Test
    public void testEmptyStringListToTags() {

        final Annotation annot = new Annotation();

        // empty list of Strings should not produce any result
        final List<String> tagStrings = new ArrayList<String>();

        List<Tag> tagList = tagsService.getTagList(tagStrings, annot);
        Assert.assertNull(tagList);

        // null list should not produce any result either
        tagList = tagsService.getTagList(null, annot);
        Assert.assertNull(tagList);
    }

    /**
     * test that missing annotation does not produce any tags
     */
    @Test
    public void testMissingAnnotationDoesNotProduceTags() {

        // empty list of Strings should not produce any result
        final List<String> tagStrings = new ArrayList<String>();
        tagStrings.add("mytag");

        final List<Tag> tagList = tagsService.getTagList(tagStrings, null);
        Assert.assertNull(tagList);
    }

    /**
     * test removing tags
     */
    @Test
    public void testRemoveTags() throws CannotCreateAnnotationException {

        final String userlogin = "demo";

        // save an annotation with two tags
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(userlogin);

        final List<String> tagStrings = new ArrayList<String>();
        tagStrings.add("mytag");
        tagStrings.add("mysecondtag");

        jsAnnot.setTags(tagStrings);
        
        annotService.createAnnotation(jsAnnot, new UserInformation(user, Authorities.ISC)); // use the service in order to save effort...

        Assert.assertEquals(2, tagRepos.count());

        // remove tags one by one
        tagsService.removeTags(((List<Tag>) tagRepos.findAll()).subList(0, 1));
        Assert.assertEquals(1, tagRepos.count());

        tagsService.removeTags((List<Tag>) tagRepos.findAll());
        Assert.assertEquals(0, tagRepos.count());

        // removing empty list should not throw error
        tagsService.removeTags(null);
    }
    
    @Test
    public void testIsSuggestion() {
        
        final Annotation annot = new Annotation();
        final List<Tag> tagList = new ArrayList<Tag>();
        tagList.add(new Tag(Annotation.ANNOTATION_SUGGESTION, annot));
        
        Assert.assertTrue(tagsService.hasSuggestionTag(tagList));
    }
    
    @Test
    public void testIsNoSuggestion() {
        
        final Annotation annot = new Annotation();
        final List<Tag> tagList = new ArrayList<Tag>();
        tagList.add(new Tag("something", annot));
        
        Assert.assertFalse(tagsService.hasSuggestionTag(tagList));
    }
    
    @Test
    public void testIsSuggestion_NoTags() {
        
        Assert.assertFalse(tagsService.hasSuggestionTag(null));
    }
    
    @Test
    public void testIsSuggestion_EmptyTagList() {
        
        Assert.assertFalse(tagsService.hasSuggestionTag(new ArrayList<Tag>()));
    }
    
    @Test
    public void testIsHighlight() {
        
        final Annotation annot = new Annotation();
        final List<Tag> tagList = new ArrayList<Tag>();
        tagList.add(new Tag(Annotation.ANNOTATION_HIGHLIGHT, annot));
        
        Assert.assertTrue(tagsService.hasHighlightTag(tagList));
    }
    
    @Test
    public void testIsNoHighlight() {
        
        final Annotation annot = new Annotation();
        final List<Tag> tagList = new ArrayList<Tag>();
        tagList.add(new Tag("whatever", annot));
        
        Assert.assertFalse(tagsService.hasHighlightTag(tagList));
    }
    
    @Test
    public void testIsHighlight_NoTags() {
        
        Assert.assertFalse(tagsService.hasHighlightTag(null));
    }
    
    @Test
    public void testIsHighlight_EmptyTagList() {
        
        Assert.assertFalse(tagsService.hasHighlightTag(new ArrayList<Tag>()));
    }
}
