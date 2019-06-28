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

import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.helper.JsonConverter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.hamcrest.Matchers.*;

@RunWith(SpringRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class JsonConverterTest {

    @Test
    public void deserializeOneStatus() {

        final String input = "[\"DELETED\"]";

        final List<AnnotationStatus> result = JsonConverter.convertJsonToAnnotationStatusList(input);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertThat(result, contains(AnnotationStatus.DELETED));
    }

    @Test
    public void deserializeAllStatus() {

        final String input = "[\"ALL\"]";

        final List<AnnotationStatus> result = JsonConverter.convertJsonToAnnotationStatusList(input);
        Assert.assertNotNull(result);
        Assert.assertEquals(4, result.size());
        Assert.assertThat(result, containsInAnyOrder(AnnotationStatus.NORMAL, AnnotationStatus.ACCEPTED, AnnotationStatus.REJECTED, AnnotationStatus.DELETED));
    }

    @Test
    public void deserializeAllStatusesIndividually() {

        final String input = "[\"NORMAL\", \"ACCEPTED\", \"REJECTED\", \"DELETED\"]";

        final List<AnnotationStatus> result = JsonConverter.convertJsonToAnnotationStatusList(input);
        Assert.assertNotNull(result);
        Assert.assertEquals(4, result.size());
        Assert.assertThat(result, containsInAnyOrder(AnnotationStatus.NORMAL, AnnotationStatus.ACCEPTED, AnnotationStatus.REJECTED, AnnotationStatus.DELETED));
    }

    // check that the "ALL" status takes precedence
    @Test
    public void deserializeAllAndOtherStatus() {

        final String input = "[\"NORMAL\", \"ALL\"]";

        final List<AnnotationStatus> result = JsonConverter.convertJsonToAnnotationStatusList(input);
        Assert.assertNotNull(result);
        Assert.assertEquals(4, result.size());
        Assert.assertThat(result, containsInAnyOrder(AnnotationStatus.NORMAL, AnnotationStatus.ACCEPTED, AnnotationStatus.REJECTED, AnnotationStatus.DELETED));
    }

    // empty list of statuses returns the default status (= NORMAL)
    @Test
    public void deserializeEmptyStatusList() {

        final String input = "[]";

        final List<AnnotationStatus> result = JsonConverter.convertJsonToAnnotationStatusList(input);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertThat(result, contains(AnnotationStatus.NORMAL));
    }

    // empty status returns the default status (= NORMAL)
    @Test
    public void deserializeEmptyStatus() {

        final String input = "";

        final List<AnnotationStatus> result = JsonConverter.convertJsonToAnnotationStatusList(input);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertThat(result, contains(AnnotationStatus.NORMAL));
    }

    // deserialisation of two {@link SimpleMetadataWithStatuses} items
    @Test
    public void deserializeListOfSimpleMetadataWithStatuses() {

        final String SOURCE_JSON = "[{\"ISC\":\"ISC/1/2\", \"status\":[\"NORMAL\"]},{\"ISC2\":\"ISC/3/4\", \"status\":[\"DELETED\",\"ACCEPTED\"]}]";

        final List<SimpleMetadataWithStatuses> list = JsonConverter.convertJsonToSimpleMetadataWithStatusesList(SOURCE_JSON);

        Assert.assertEquals(2, list.size());

        // verify statuses
        List<AnnotationStatus> stats = list.get(0).getStatuses();
        Assert.assertEquals(1, stats.size());
        Assert.assertEquals(AnnotationStatus.NORMAL, stats.get(0));

        stats = list.get(1).getStatuses();
        Assert.assertEquals(2, stats.size());
        Assert.assertThat(stats, containsInAnyOrder(AnnotationStatus.ACCEPTED, AnnotationStatus.DELETED));

        // check metadata
        SimpleMetadata meta = list.get(0).getMetadata();
        Assert.assertNotNull(meta);
        Assert.assertEquals(1, meta.size());
        Assert.assertEquals("ISC/1/2", meta.get("ISC"));

        meta = list.get(1).getMetadata();
        Assert.assertNotNull(meta);
        Assert.assertEquals(1, meta.size());
        Assert.assertEquals("ISC/3/4", meta.get("ISC2"));
    }

    // deserialisation of a {@link SimpleMetadataWithStatuses}, property names don't have quotes
    @Test
    public void deserializeListOfSimpleMetadataWithStatuses_withoutQuotes() {

        final String SOURCE_JSON = "[{ISC2:\"ISC/3/4\", Version:\"vers1\", status:[\"REJECTED\",\"ACCEPTED\"]}]";

        final List<SimpleMetadataWithStatuses> list = JsonConverter.convertJsonToSimpleMetadataWithStatusesList(SOURCE_JSON);

        Assert.assertEquals(1, list.size());

        // verify statuses
        final List<AnnotationStatus> stats = list.get(0).getStatuses();
        Assert.assertEquals(2, stats.size());
        Assert.assertThat(stats, containsInAnyOrder(AnnotationStatus.ACCEPTED, AnnotationStatus.REJECTED));

        // check metadata
        final SimpleMetadata meta = list.get(0).getMetadata();
        Assert.assertNotNull(meta);
        Assert.assertEquals(2, meta.size());
        Assert.assertEquals("ISC/3/4", meta.get("ISC2"));
        Assert.assertEquals("vers1", meta.get("Version"));
    }

    // deserialisation of a {@link SimpleMetadataWithStatuses}, "ALL" status is given
    @Test
    public void deserializeListOfSimpleMetadataWithStatuses_allStatus() {

        final String SOURCE_JSON = "[{Version:\"vers1\", status:[\"ALL\"]}]";

        final List<SimpleMetadataWithStatuses> list = JsonConverter.convertJsonToSimpleMetadataWithStatusesList(SOURCE_JSON);

        Assert.assertEquals(1, list.size());

        // verify statuses
        final List<AnnotationStatus> stats = list.get(0).getStatuses();
        Assert.assertEquals(4, stats.size());
        Assert.assertThat(stats, containsInAnyOrder(AnnotationStatus.NORMAL, AnnotationStatus.ACCEPTED, AnnotationStatus.REJECTED, AnnotationStatus.DELETED));

        // check metadata
        final SimpleMetadata meta = list.get(0).getMetadata();
        Assert.assertNotNull(meta);
        Assert.assertEquals(1, meta.size());
        Assert.assertEquals("vers1", meta.get("Version"));
    }

    // deserialisation of a {@link SimpleMetadataWithStatuses} not containing statuses should add default status
    @Test
    public void deserializeListOfSimpleMetadataWithStatuses_noStatus() {

        final String SOURCE_JSON = "[{\"ISC\":\"ISC/1/2\"}]";

        final List<SimpleMetadataWithStatuses> list = JsonConverter.convertJsonToSimpleMetadataWithStatusesList(SOURCE_JSON);

        Assert.assertEquals(1, list.size());

        // verify statuses: default status should have been added
        final List<AnnotationStatus> stats = list.get(0).getStatuses();
        Assert.assertEquals(1, stats.size());
        Assert.assertEquals(AnnotationStatus.NORMAL, stats.get(0));

        // check metadata
        final SimpleMetadata meta = list.get(0).getMetadata();
        Assert.assertNotNull(meta);
        Assert.assertEquals(1, meta.size());
        Assert.assertEquals("ISC/1/2", meta.get("ISC"));
    }

    // deserialisation of a {@link SimpleMetadataWithStatuses} not containing metadata should not add some
    @Test
    public void deserializeListOfSimpleMetadataWithStatuses_noMetadata() {

        final String SOURCE_JSON = "[{\"status\":[\"DELETED\",\"ACCEPTED\"]}]";

        final List<SimpleMetadataWithStatuses> list = JsonConverter.convertJsonToSimpleMetadataWithStatusesList(SOURCE_JSON);

        Assert.assertEquals(1, list.size());

        // verify statuses: default status should have been added
        final List<AnnotationStatus> stats = list.get(0).getStatuses();
        Assert.assertEquals(2, stats.size());
        Assert.assertThat(stats, contains(AnnotationStatus.DELETED, AnnotationStatus.ACCEPTED));

        // check metadata
        final SimpleMetadata meta = list.get(0).getMetadata();
        Assert.assertNotNull(meta);
        Assert.assertEquals(0, meta.size());
    }

    // deserialisation of an empty {@link SimpleMetadataWithStatuses} -> no metadata, but default status expected
    @Test
    public void deserializeListOfSimpleMetadataWithStatuses_empty() {

        final String SOURCE_JSON = "[{}]";

        final List<SimpleMetadataWithStatuses> list = JsonConverter.convertJsonToSimpleMetadataWithStatusesList(SOURCE_JSON);

        Assert.assertEquals(1, list.size());

        // verify statuses: default status should have been added
        final List<AnnotationStatus> stats = list.get(0).getStatuses();
        Assert.assertEquals(1, stats.size());
        Assert.assertThat(stats, contains(AnnotationStatus.NORMAL));

        // check metadata
        final SimpleMetadata meta = list.get(0).getMetadata();
        Assert.assertNotNull(meta);
        Assert.assertEquals(0, meta.size());
    }
}
