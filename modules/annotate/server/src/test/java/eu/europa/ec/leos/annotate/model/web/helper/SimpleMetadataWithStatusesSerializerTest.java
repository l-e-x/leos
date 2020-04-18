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
package eu.europa.ec.leos.annotate.model.web.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.europa.ec.leos.annotate.helper.SerialisationHelper;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

@RunWith(SpringRunner.class)
public class SimpleMetadataWithStatusesSerializerTest {

    // serialisation of an empty SimpleMetadataWithStatuses instance
    @Test
    public void serializeEmptyObject() throws JsonProcessingException {

        final SimpleMetadataWithStatuses smws = new SimpleMetadataWithStatuses();

        final String serialized = SerialisationHelper.serializeSimpleMetadataWithStatusesList(Arrays.asList(smws));
        final String expectedSerialized = "[%7B\"status\":[\"NORMAL\"]%7D]";

        Assert.assertEquals(expectedSerialized, serialized);
    }

    // serialisation of an SimpleMetadataWithStatuses instance containing statuses only
    @Test
    public void serializeStatusesOnly() throws JsonProcessingException {

        final SimpleMetadataWithStatuses smws = new SimpleMetadataWithStatuses();
        smws.setStatuses(Arrays.asList(AnnotationStatus.ACCEPTED, AnnotationStatus.REJECTED));

        final String serialized = SerialisationHelper.serializeSimpleMetadataWithStatusesList(Arrays.asList(smws));
        final String expectedSerialized = "[%7B\"status\":[\"ACCEPTED\",\"REJECTED\"]%7D]";

        Assert.assertEquals(expectedSerialized, serialized);
    }

    // serialisation of an SimpleMetadataWithStatuses instance containing metadata and no explicit statuses (i.e. default status only)
    @Test
    public void serializeMetadataOnly() throws JsonProcessingException {

        final SimpleMetadataWithStatuses smws = new SimpleMetadataWithStatuses();
        smws.setMetadata(new SimpleMetadata("key", "val"));

        final String serialized = SerialisationHelper.serializeSimpleMetadataWithStatusesList(Arrays.asList(smws));
        final String expectedSerialized = "[%7B\"key\":\"val\",\"status\":[\"NORMAL\"]%7D]";

        Assert.assertEquals(expectedSerialized, serialized);
    }

    // serialisation of an SimpleMetadataWithStatuses list containing several items with metadata and statuses
    @Test
    public void serializeFilledList() throws JsonProcessingException {

        final SimpleMetadata sm1 = new SimpleMetadata("ISCReference", "ISC/5/2019");
        sm1.put("respVersion", "AGRI-1");
        final SimpleMetadataWithStatuses smws1 = new SimpleMetadataWithStatuses(sm1, Arrays.asList(AnnotationStatus.DELETED));

        final SimpleMetadata sm2 = new SimpleMetadata("ISCReference", "ISC/6/2019");
        sm2.put("respVersion", "DIGIT-4");
        sm2.put("respId", "123-456-789b");
        final SimpleMetadataWithStatuses smws2 = new SimpleMetadataWithStatuses(sm2, Arrays.asList(AnnotationStatus.ALL));

        final String serialized = SerialisationHelper.serializeSimpleMetadataWithStatusesList(Arrays.asList(smws1, smws2));
        final String expectedSerialized = "[" 
            + "%7B\"ISCReference\":\"ISC/5/2019\",\"respVersion\":\"AGRI-1\",\"status\":[\"DELETED\"]%7D,"
            + "%7B\"ISCReference\":\"ISC/6/2019\",\"respId\":\"123-456-789b\",\"respVersion\":\"DIGIT-4\",\"status\":[\"ALL\"]%7D"
            + "]";

        Assert.assertEquals(expectedSerialized, serialized);
    }
}
