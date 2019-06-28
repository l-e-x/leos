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
package eu.europa.ec.leos.services.mandate;

import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.domain.vo.DocumentVO;

public interface PostProcessingMandateService {

	// Constants declaration
	static final String BILL = "bill";
	static final String DOC = "doc";

	static final String BODY = "body";
	static final String PREAMBLE = "preamble";
	static final String ARTICLE = "article";
	static final String CITATIONS = "citations";
	static final String CITATION = "citation";
	static final String RECITALS = "recitals";
	static final String RECITAL = "recital";

	static final String EC = "ec";
	static final String LEOS_ORIGIN_ATTR = "leos:origin";
	static final String LEOS_DELETABLE_ATTR = "leos:deletable";
	static final String LEOS_EDITABLE_ATTR = "leos:editable";

	// Interface methods
	Result processMandate(DocumentVO documentVO);
}
