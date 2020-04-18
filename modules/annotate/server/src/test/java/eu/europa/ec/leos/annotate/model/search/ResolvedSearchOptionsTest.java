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
package eu.europa.ec.leos.annotate.model.search;

import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.helper.JsonConverter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
public class ResolvedSearchOptionsTest {

    /**
     * tests for the most important methods of the {@link ResolvedSearchOptions} class
     */

    // -------------------------------------
    // Tests
    // -------------------------------------

    // test that metadata conversion works even when quotes around property names are missing
    @Test
    public void testMetadataChunkedCorrectlyWithoutQuotes() {

        final String metaAsJson = "[{ firstProp: 1, secondProp: \"abc\"}]";
        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setMetadataWithStatusesList(metaAsJson);

        final List<SimpleMetadataWithStatuses> metaList = rso.getMetadataWithStatusesList();
        Assert.assertNotNull(metaList);
        Assert.assertEquals(1, metaList.size());

        final SimpleMetadata metas = metaList.get(0).getMetadata();
        Assert.assertEquals(2, metas.size());
        Assert.assertEquals("1", metas.get("firstProp"));
        Assert.assertEquals("abc", metas.get("secondProp"));
    }

    // test that statuses cannot be recognized if quotes are missing; default value expected
    @Test
    public void testStatusWithoutQuotes_bringsDefault() {

        final String metaAsJson = "[{status:[ACCEPTED]}]";
        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setMetadataWithStatusesList(metaAsJson);

        final List<SimpleMetadataWithStatuses> metaList = rso.getMetadataWithStatusesList();
        Assert.assertNotNull(metaList);
        Assert.assertEquals(1, metaList.size());

        final SimpleMetadata metas = metaList.get(0).getMetadata();
        Assert.assertEquals(0, metas.size());

        // could not be deserialized due to missing quotes -> default values only
        final List<AnnotationStatus> stats = metaList.get(0).getStatuses();
        Assert.assertEquals(1, stats.size());
        Assert.assertEquals(AnnotationStatus.NORMAL, stats.get(0));
    }

    // test that metadata conversion works even when quotes around property names are used
    @Test
    public void testMetadataChunkedCorrectlyWithQuotes() {

        final String metaAsJson = "[{ \"firstProp\": \"1\", \"secondProp\": \"abc\"}]";
        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setMetadataWithStatusesList(metaAsJson);

        final List<SimpleMetadataWithStatuses> metaList = rso.getMetadataWithStatusesList();
        Assert.assertNotNull(metaList);
        Assert.assertEquals(1, metaList.size());

        final SimpleMetadata metas = metaList.get(0).getMetadata();
        Assert.assertEquals(2, metas.size());
        Assert.assertEquals("1", metas.get("firstProp"));
        Assert.assertEquals("abc", metas.get("secondProp"));
    }

    // test that metadata conversion works for a list of maps
    @Test
    public void testMetadataList() {

        final String metaAsJson = "[{ \"firstPropFirstList\": \"1\", \"secondPropFirstList\": \"abc\"}," +
                "{ \"firstPropSecondList\": \"ab8\", \"secondPropSecondList\": \"def\", \"end\": \"here\"}]";
        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setMetadataWithStatusesList(metaAsJson);

        final List<SimpleMetadataWithStatuses> metaList = rso.getMetadataWithStatusesList();
        Assert.assertNotNull(metaList);
        Assert.assertEquals(2, metaList.size());

        final SimpleMetadata metaMap1 = metaList.get(0).getMetadata();
        Assert.assertEquals(2, metaMap1.size());
        Assert.assertEquals("1", metaMap1.get("firstPropFirstList"));
        Assert.assertEquals("abc", metaMap1.get("secondPropFirstList"));

        final SimpleMetadata metaMap2 = metaList.get(1).getMetadata();
        Assert.assertEquals(3, metaMap2.size());
        Assert.assertEquals("ab8", metaMap2.get("firstPropSecondList"));
        Assert.assertEquals("def", metaMap2.get("secondPropSecondList"));
        Assert.assertEquals("here", metaMap2.get("end"));
    }

    // test that metadata conversion fails when received content is not JSON-conformant
    @Test
    public void testMetadataConversionFails_missingQuote() {

        final String metaAsJson = "[{ firstProp: 1, secondProp: abc}]"; // quotes around non-numeric content missing!
        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setMetadataWithStatusesList(metaAsJson);

        // calling the function on the level of the {@link ResolvedSearchOptions} gives a dummy
        final List<SimpleMetadataWithStatuses> metaList = rso.getMetadataWithStatusesList();
        Assert.assertTrue(isDummyMetadataWithStatusesList(metaList));

        // same hold for the pure deserialisation
        final List<SimpleMetadataWithStatuses> deserial = JsonConverter.convertJsonToSimpleMetadataWithStatusesList(metaAsJson);
        Assert.assertTrue(isDummyMetadataWithStatusesList(deserial));
    }

    private boolean isDummyMetadataWithStatusesList(final List<SimpleMetadataWithStatuses> smwsList) {
        Assert.assertNotNull(smwsList);
        Assert.assertEquals(1, smwsList.size());
        Assert.assertEquals(0, smwsList.get(0).getMetadata().size());
        Assert.assertEquals(1, smwsList.get(0).getStatuses().size());
        Assert.assertTrue(smwsList.get(0).getStatuses().contains(AnnotationStatus.NORMAL));

        return true;
    }

    // test that metadata conversion fails when received content is not a list of maps
    @Test
    public void testMetadataConversionFails_noList() {

        final String metaAsJson = "{ firstProp: \"1\", secondProp: \"abc\"}"; // missing surrounding [ ]
        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setMetadataWithStatusesList(metaAsJson);

        // calling the function on the level of the {@link ResolvedSearchOptions} gives a dummy
        final List<SimpleMetadataWithStatuses> metaList = rso.getMetadataWithStatusesList();
        Assert.assertTrue(isDummyMetadataWithStatusesList(metaList));

        // but the pure deserialisation cannot deal with it
        final List<SimpleMetadataWithStatuses> deserial = JsonConverter.convertJsonToSimpleMetadataWithStatusesList(metaAsJson);
        Assert.assertNotNull(deserial);
        Assert.assertEquals(0, deserial.size());
    }

    // test that NORMAL annotations without any specific metadata are wanted when creating a new object
    @Test
    public void testStatusSetForNewInstance() {

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();

        final List<SimpleMetadataWithStatuses> metaStats = rso.getMetadataWithStatusesList();
        Assert.assertEquals(1, metaStats.size());

        // metadata entry available, but empty
        final SimpleMetadataWithStatuses entry = metaStats.get(0);
        Assert.assertEquals(0, entry.getMetadata().size());

        // default status set
        Assert.assertEquals(1, entry.getStatuses().size());
        Assert.assertEquals(AnnotationStatus.NORMAL, entry.getStatuses().get(0));
    }

    // test that NORMAL annotations remain even when setting statuses empty
    @Test
    public void testSetStatusEmpty() {

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();

        // act: fill with items
        final List<SimpleMetadataWithStatuses> smws = new ArrayList<SimpleMetadataWithStatuses>();
        final SimpleMetadata simpMeta = new SimpleMetadata("key", "someval");
        smws.add(new SimpleMetadataWithStatuses(simpMeta, Arrays.asList(AnnotationStatus.ACCEPTED)));
        rso.setMetadataWithStatusesList(smws);

        // verify
        Assert.assertEquals(1, rso.getMetadataWithStatusesList().size());
        Assert.assertEquals(AnnotationStatus.ACCEPTED, rso.getMetadataWithStatusesList().get(0).getStatuses().get(0));
        Assert.assertEquals("someval", rso.getMetadataWithStatusesList().get(0).getMetadata().get("key"));

        // act again
        rso.setMetadataWithStatusesList("");

        // verify again: only default entry available
        Assert.assertEquals(1, rso.getMetadataWithStatusesList().size());
        Assert.assertEquals(AnnotationStatus.NORMAL, rso.getMetadataWithStatusesList().get(0).getStatuses().get(0));
        Assert.assertEquals(0, rso.getMetadataWithStatusesList().get(0).getMetadata().size());
    }

    // test that annotation status "ALL" is set correctly
    @Test
    public void testSetAllStatus() {

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();

        // act
        rso.setMetadataWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(null, Arrays.asList(AnnotationStatus.ALL))));

        // verify: all status values set
        Assert.assertEquals(1, rso.getMetadataWithStatusesList().size());
        Assert.assertThat(rso.getMetadataWithStatusesList().get(0).getStatuses(), 
                org.hamcrest.Matchers.containsInAnyOrder(AnnotationStatus.NORMAL, AnnotationStatus.ACCEPTED, AnnotationStatus.REJECTED, AnnotationStatus.DELETED));
    }

    // test that a default status is set when no status is given
    @Test
    public void testDefaultStatusAdded() {
        
        final ResolvedSearchOptions rso = new ResolvedSearchOptions();

        // act
        final SimpleMetadataWithStatuses smws = new SimpleMetadataWithStatuses();
        smws.setMetadata(new SimpleMetadata());
        smws.setStatuses(null);// no status set!
                
        rso.setMetadataWithStatusesList(Arrays.asList(smws));
        
        // verify: the setter should have checked and filled the status
        final List<SimpleMetadataWithStatuses> list = rso.getMetadataWithStatusesList();
        Assert.assertEquals(1, list.size());
        final List<AnnotationStatus> stats = list.get(0).getStatuses();
        Assert.assertEquals(1, stats.size());
        Assert.assertEquals(AnnotationStatus.getDefaultStatus(), stats);
    }
}
