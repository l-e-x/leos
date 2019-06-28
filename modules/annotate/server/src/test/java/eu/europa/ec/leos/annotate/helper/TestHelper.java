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

import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.repository.AnnotationTestRepository;
import org.junit.Assert;

import java.time.LocalDateTime;

public final class TestHelper {

    public static final String AUTH_HEADER = "authorization";
    public static final String AUTH_BEARER = "Bearer ";
    
    private TestHelper() {
        // utility class -> private constructor
    }
    
    // check that an annotation specified by its ID has a given status 
    // and/or was recently modified by a given user 
    public static void assertHasStatus(
            final AnnotationTestRepository annotRepos, 
            final String annotId, final AnnotationStatus status, final Long userId) {
        
        final Annotation foundAnnot = annotRepos.findById(annotId);
        Assert.assertEquals("Status of annotation unexpected", status, foundAnnot.getStatus());
        Assert.assertEquals("User that updated status unexpected", userId, foundAnnot.getStatusUpdatedBy());
        
        if(userId != null) {
            Assert.assertNotNull(foundAnnot.getStatusUpdated());
            // set timestamp must be within the last about ten seconds
            Assert.assertTrue("'updated' timestamp not within 10 seconds before now", withinLastSeconds(foundAnnot.getStatusUpdated(), 10));
        }
    }
    
    public static boolean withinLastSeconds(final LocalDateTime dateToCheck, final long tolerance) {

        final LocalDateTime currentTime = LocalDateTime.now();
        return (dateToCheck.isBefore(currentTime) || dateToCheck.isEqual(currentTime)) 
                && dateToCheck.isAfter(currentTime.minusSeconds(tolerance));

    }
}
