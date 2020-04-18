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
package eu.europa.ec.leos.annotate.helper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.web.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TestData {

    /**
     * Class containing functions for providing test data
     */

    private TestData() {
        // private constructor to have class be considered an utility class
    }
    
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_EXCEPTION")
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public static JsonAnnotation getTestAnnotationObject(final String username) {

        URI uri = null;
        try {
            uri = new URI("https://a.com");
        } catch (URISyntaxException e) {
            // will not occur, URI is valid
        }
        
        final String authority = username.substring(username.indexOf('@') + 1);

        final JsonAnnotation jsAnnot = new JsonAnnotation();
        jsAnnot.setCreated(LocalDateTime.now());
        jsAnnot.setGroup("__world__");
        jsAnnot.setUser(username);
        jsAnnot.setText("my annotation");
        jsAnnot.setUri(uri);
        jsAnnot.setReferences(null); // new top-level annotation has no references

        final JsonAnnotationDocument jsAnnotDoc = new JsonAnnotationDocument();
        final JsonAnnotationDocumentLink docLink = new JsonAnnotationDocumentLink();
        docLink.setHref(uri);

        final List<JsonAnnotationDocumentLink> docLinks = new ArrayList<JsonAnnotationDocumentLink>();
        docLinks.add(docLink);
        jsAnnotDoc.setLink(docLinks);
        
        final SimpleMetadata metadata = new SimpleMetadata();
        metadata.put("systemId", authority);
        jsAnnotDoc.setMetadata(metadata);
        jsAnnot.setDocument(jsAnnotDoc);

        // permissions
        final JsonAnnotationPermissions permissions = new JsonAnnotationPermissions();
        permissions.setAdmin(Arrays.asList(username));
        permissions.setDelete(Arrays.asList(username));
        permissions.setUpdate(Arrays.asList(username));
        permissions.setRead(Arrays.asList("group:__world__")); // public
        jsAnnot.setPermissions(permissions);

        final JsonAnnotationTargets targets = new JsonAnnotationTargets("[{\"selector\":null,\"source\":\"" + uri.toString() + "\"}]");
        targets.setSource(uri);
        final List<JsonAnnotationTargets> targetsList = new ArrayList<JsonAnnotationTargets>();
        targetsList.add(targets);
        jsAnnot.setTarget(targetsList);

        final List<String> tags = new ArrayList<String>();
        tags.add("firsttag");
        tags.add("secondtag");
        jsAnnot.setTags(tags);

        return jsAnnot;
    }

    // create a non-shared annotation
    public static JsonAnnotation getTestPrivateAnnotationObject(final String username) {
        
        final JsonAnnotation annot = getTestAnnotationObject(username);
        annot.getPermissions().setRead(Arrays.asList(username));
        return annot;
    }
    
    public static JsonAnnotation getTestReplyToAnnotation(final String username, final URI uri, final JsonAnnotation parent) {
        
        return getTestReplyToAnnotation(username, uri, Arrays.asList(parent.getId()));
    }
    
    public static JsonAnnotation getTestReplyToAnnotation(final String username, final URI uri, final List<String> references) {

        final JsonAnnotation jsAnnot = new JsonAnnotation();
        jsAnnot.setCreated(LocalDateTime.now());
        jsAnnot.setGroup("__world__");
        jsAnnot.setUser(username);
        jsAnnot.setText("my reply");
        jsAnnot.setUri(uri);

        // set the references (=IDs of ancestor annotations; this distinguishes top-level annotations from replies)
        jsAnnot.setReferences(references);

        // in a reply, the target only contains the source URI, no selectors
        final JsonAnnotationTargets targets = new JsonAnnotationTargets(null);
        targets.setSource(uri);

        /*final JsonAnnotationDocumentLink docLink = new JsonAnnotationDocumentLink();
        docLink.setHref(uri);

        final List<JsonAnnotationDocumentLink> docLinks = new ArrayList<JsonAnnotationDocumentLink>();
        docLinks.add(docLink);*/
        
        // note: replies do not contain the document and thus no metadata!
        
        // note: permissions are not added here, not necessary for the purpose of the tests so far

        final List<String> tags = new ArrayList<String>();
        tags.add("myreplytag");
        jsAnnot.setTags(tags);

        return jsAnnot;
    }
    
    // return a simple suggestion
    public static JsonAnnotation getTestSuggestionObject(final String username) {
        
        final JsonAnnotation sugg = getTestAnnotationObject(username);
        sugg.setTags(Arrays.asList(Annotation.ANNOTATION_SUGGESTION));
        return sugg;
    }
}
