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
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Tag;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.TagsService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class SuggestionCheckTest {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationService annotService;

    @Autowired
    private TagsService tagsService;

    // -------------------------------------
    // Tests
    // -------------------------------------
    /**
     * test successful recognition of an annotation as a suggestion
     */
    @Test
    public void testSuggestionIsRecognized() {

        // create the annotation
        Annotation annot = new Annotation();
        annot.setTags(tagsService.getTagList(Arrays.asList("suggestion"), annot));

        // verify annotation is considered being a suggestion
        Assert.assertTrue(annotService.isSuggestion(annot));
    }

    /**
     * test that a comment annotation is not recognized as a suggestion
     */
    @Test
    public void testCommentIsNotRecognizedAsSuggestion() {

        // create the annotation
        Annotation annot = new Annotation();
        annot.setTags(tagsService.getTagList(Arrays.asList("comment"), annot));

        // verify annotation is not considered as a suggestion
        Assert.assertFalse(annotService.isSuggestion(annot));
    }

    /**
     * test that a highlight annotation is not recognized as a suggestion
     */
    @Test
    public void testHighlightIsNotRecognizedAsSuggestion() {

        // create the annotation
        Annotation annot = new Annotation();
        annot.setTags(tagsService.getTagList(Arrays.asList("highlight"), annot));

        // verify annotation is not considered as a suggestion
        Assert.assertFalse(annotService.isSuggestion(annot));
    }

    /**
     * test that an annotation without tags is not recognized as a suggestion
     */
    @Test
    public void testAnnotationWithoutTagsIsNotRecognizedAsSuggestion() {

        // create the annotation
        Annotation annot = new Annotation();
        annot.setTags(null);

        // verify annotation is not considered as a suggestion
        Assert.assertFalse(annotService.isSuggestion(annot));

        // set empty list of tags -> still no suggestion
        annot.setTags(new ArrayList<Tag>());
        Assert.assertFalse(annotService.isSuggestion(annot));
    }

    /**
     * test that an undefined annotation is not recognized as a suggestion (exception)
     */
    @Test
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Intended for test")
    public void testUndefinedAnnotationIsNotRecognizedAsSuggestion() {

        try {
            // should throw an exception
            annotService.isSuggestion(null);
            Assert.fail("Expected exception for checking on undefined Annotation not thrown!");
        } catch (Exception e) {
            // ok
        }
    }
}
