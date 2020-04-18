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
package eu.europa.ec.leos.annotate.model;

import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * simple tests on the {@link SimpleMetadataWithStatuses} object
 */
@RunWith(SpringRunner.class)
public class SimpleMetadataWithStatusTest {

    @Test
    public void testNewInstanceIsNotEmpty() {

        final SimpleMetadataWithStatuses smws = new SimpleMetadataWithStatuses();

        // verify
        Assert.assertNotNull(smws.getMetadata());
        Assert.assertNotNull(smws.getStatuses());
        Assert.assertEquals(1, smws.getStatuses().size());
        Assert.assertEquals(AnnotationStatus.getDefaultStatus(), smws.getStatuses());
        Assert.assertTrue(smws.isEmptyDefaultEntry());
    }
    
    @Test
    public void testNewInstanceIsNotEmpty2() {

        final SimpleMetadataWithStatuses smws = new SimpleMetadataWithStatuses(null, null); // check with other constructor

        // verify
        Assert.assertNotNull(smws.getMetadata());
        Assert.assertNotNull(smws.getStatuses());
        Assert.assertEquals(1, smws.getStatuses().size());
        Assert.assertEquals(AnnotationStatus.getDefaultStatus(), smws.getStatuses());
        Assert.assertTrue(smws.isEmptyDefaultEntry());
    }
    
}
