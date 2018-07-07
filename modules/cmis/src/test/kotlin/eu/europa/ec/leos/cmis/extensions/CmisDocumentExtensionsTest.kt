/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.cmis.extensions

import eu.europa.ec.leos.cmis.mapping.CmisProperties
import eu.europa.ec.leos.domain.common.LeosAuthority
import mu.KLogging
import org.apache.chemistry.opencmis.client.api.Document
import org.apache.chemistry.opencmis.client.api.Property
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.`when`

internal class CmisDocumentExtensionsTest {

    companion object : KLogging()

    @Test
    fun test_getCollaborators_MapIsCorrect() {
        //setup
        val property: Property<Any> = Mockito.mock(Property::class.java) as Property<Any>
        `when`(property.values).thenReturn(listOf("testUser1::OWNER", "testUser2::OWNER", "testUser3::CONTRIBUTOR"))

        val cmisDocument = Mockito.mock<Document>(Document::class.java)
        `when`(cmisDocument.getProperty<Any>(ArgumentMatchers.eq(CmisProperties.COLLABORATORS.id))).thenReturn(property)

        //make call
        val resultUsers: Map<String, LeosAuthority> = cmisDocument.collaborators

        //verify
        Assert.assertThat(resultUsers.size, Matchers.`is`(3))
        Assert.assertThat(resultUsers["testUser1"], Matchers.equalTo(LeosAuthority.OWNER))
        Assert.assertThat(resultUsers["testUser2"], Matchers.equalTo(LeosAuthority.OWNER))
        Assert.assertThat(resultUsers["testUser3"], Matchers.equalTo(LeosAuthority.CONTRIBUTOR))
    }

    @Test
    fun test_getCollaborators_IfIncorrectValuesAreIgnored() {
        //setup
        val property: Property<Any> = Mockito.mock(Property::class.java) as Property<Any>
        `when`(property.values).thenReturn(listOf("testUser1::OWNER", "testUser2::INCORRECT"))

        val cmisDocument = Mockito.mock<Document>(Document::class.java)
        `when`(cmisDocument.getProperty<Any>(ArgumentMatchers.eq(CmisProperties.COLLABORATORS.id))).thenReturn(property)

        //make call
        val resultUsers: Map<String, LeosAuthority> = cmisDocument.collaborators

        //verify
        Assert.assertThat(resultUsers.size, Matchers.`is`(1))
        Assert.assertThat(resultUsers["testUser1"], Matchers.equalTo(LeosAuthority.OWNER))
    }

    @Test
    fun test_getCollaborators_IfIncorrectFormatIgnored() {
        //setup
        val property: Property<Any> = Mockito.mock(Property::class.java) as Property<Any>
        `when`(property.values).thenReturn(listOf("testUser1::OWNER", "XYZ"))

        val cmisDocument = Mockito.mock<Document>(Document::class.java)
        `when`(cmisDocument.getProperty<Any>(ArgumentMatchers.eq(CmisProperties.COLLABORATORS.id))).thenReturn(property)

        //make call
        val resultUsers: Map<String, LeosAuthority> = cmisDocument.collaborators

        //verify
        Assert.assertThat(resultUsers.size, Matchers.`is`(1))
        Assert.assertThat(resultUsers["testUser1"], Matchers.equalTo(LeosAuthority.OWNER))
    }
}
