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
package eu.europa.ec.leos.usecases.document;

import org.junit.Test;

import eu.europa.ec.leos.usecases.document.AnnexNumberGenerator;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class AnnexNumberGeneratorTest {

	@Test
	public void teset_getAnnexNumber_For_ValidNumberRange() {

		String actaulResult = AnnexNumberGenerator.getAnnexNumber(14);
		String expectedResult = "Annex XIV";
		assertThat(expectedResult, is(actaulResult));
	}

	@Test
	public void teset_getAnnexNumber_For_Zero() {

		String actaulResult = AnnexNumberGenerator.getAnnexNumber(0);
		String expectedResult = "Annex";
		assertThat(expectedResult, is(actaulResult));

	}

	@Test
	public void teset_getAnnexNumber_For_InValidNumberRange() {

		String actaulResult = AnnexNumberGenerator.getAnnexNumber(4040);
		String expectedResult = "Annex 4040";
		assertThat(expectedResult, is(actaulResult));

	}

}
