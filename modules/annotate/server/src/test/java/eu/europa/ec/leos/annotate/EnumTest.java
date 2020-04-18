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

import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

@RunWith(SpringRunner.class)
public class EnumTest {

    // check that the retrieval of all statuses actually returns them all
    // (small help for consistency checks in case the enum is enlarged)
    @Test
    public void checkAllStatusEnums() {
        
        final List<AnnotationStatus> all = AnnotationStatus.getAllValues();
        Assert.assertEquals(all.size(), AnnotationStatus.values().length - 1); // contains all enum values except "ALL"
        Assert.assertTrue(all.contains(AnnotationStatus.NORMAL));
        Assert.assertTrue(all.contains(AnnotationStatus.DELETED));
        Assert.assertTrue(all.contains(AnnotationStatus.ACCEPTED));
        Assert.assertTrue(all.contains(AnnotationStatus.REJECTED));

        // compute value sum
        int sum = 0;
        for(int i = 0; i < all.size(); i++) {
            sum += all.get(i).getEnumValue();
        }
        
        Assert.assertEquals(AnnotationStatus.ALL.getEnumValue(), sum);
    }
    
    // check that the default item is still the expected one
    @Test
    public void checkDefaultStatus() {
        
        final List<AnnotationStatus> defaultList = AnnotationStatus.getDefaultStatus();
        Assert.assertEquals(1, defaultList.size());
        Assert.assertTrue(defaultList.contains(AnnotationStatus.NORMAL));
    }
}
