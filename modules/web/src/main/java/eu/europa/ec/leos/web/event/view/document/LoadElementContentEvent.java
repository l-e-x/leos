/**
 * Copyright 2015 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.web.event.view.document;

public class LoadElementContentEvent {

	private String elementId;
	private String elementType;
	private String windowName;

	public LoadElementContentEvent(String windowName, String elementId, String elementType) {
		this.elementId = elementId;
		this.elementType = elementType;
		this.windowName = windowName;
	}
	
	public String getElementType() {
		return elementType;
	}
	
	public String getElementId() {
		return elementId;
	}
	
	public String getWindowName() {
		return windowName;
	}

}
