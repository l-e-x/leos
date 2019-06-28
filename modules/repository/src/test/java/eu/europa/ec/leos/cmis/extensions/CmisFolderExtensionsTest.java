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
package eu.europa.ec.leos.cmis.extensions;

import eu.europa.ec.leos.domain.cmis.LeosPackage;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CmisFolderExtensionsTest {

    private static final String FOLDER_ID = "FOLDER_ID";
    private static final String FOLDER_NAME = "FOLDER_NAME";
    private static final String FOLDER_PATH = "FOLDER_PATH";

    @Test
    public void test_toLeosPackage() {
        //setup
        Folder folder = mock(Folder.class);
        when(folder.getId()).thenReturn(FOLDER_ID);
        when(folder.getName()).thenReturn(FOLDER_NAME);
        when(folder.getPath()).thenReturn(FOLDER_PATH);

        //make call
        LeosPackage leosPackage = CmisFolderExtensions.toLeosPackage(folder);

        //verify
        assertThat(leosPackage, is(notNullValue()));
        assertThat(leosPackage.getId(), is(FOLDER_ID));
        assertThat(leosPackage.getName(), is(FOLDER_NAME));
        assertThat(leosPackage.getPath(), is(FOLDER_PATH));
    }
}