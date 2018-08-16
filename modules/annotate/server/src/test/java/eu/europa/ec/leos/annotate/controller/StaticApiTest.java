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
package eu.europa.ec.leos.annotate.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
public class StaticApiTest {

    /**
     * Test that the API endpoints returning static content actually return something
     */

    // -------------------------------------
    // Required services, repositories, mocks
    // -------------------------------------

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    // -------------------------------------
    // Preparation of mock objects
    // -------------------------------------
    @Before
    public void setupTests() {

        DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
        this.mockMvc = builder.build();
    }

    // -------------------------------------
    // Tests
    // -------------------------------------
    /**
     * successfully retrieve the API root, expected HTTP 200 and the some data
     */
    @Test
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Initialisation is done in function that is run before each test")
    public void testApiRoot() throws Exception {

        // send API root request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/api/");

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that there is something
        Assert.assertNotNull(responseString);
        Assert.assertFalse(responseString.isEmpty());
    }

    // -------------------------------------
    // Tests
    // -------------------------------------
    /**
     * successfully retrieve the API links, expected HTTP 200 and the some data
     */
    @Test
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Initialisation is done in function that is run before each test")
    public void testApiLinks() throws Exception {

        // send API root request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/api/links");

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that there is something
        Assert.assertNotNull(responseString);
        Assert.assertFalse(responseString.isEmpty());
    }
}
