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
'use strict';

// `Object.assign()`-like helper. Used because this script needs to work
// in IE 10/11 without polyfills.
function assign(dest, src) {
  for (var k in src) {
    if (src.hasOwnProperty(k)) {
      dest[k] = src[k];
    }
  }
  return dest;
}

/**
 * Return a parsed `js-hypothesis-config` object from the document, or `{}`.
 *
 * Find all `<script class="js-hypothesis-config">` tags in the given document,
 * parse them as JSON, and return the parsed object.
 *
 * If there are no `js-hypothesis-config` tags in the document then return
 * `{}`.
 *
 * If there are multiple `js-hypothesis-config` tags in the document then merge
 * them into a single returned object (when multiple scripts contain the same
 * setting names, scripts further down in the document override those further
 * up).
 *
 * @param {Document|Element} document - The root element to search.
 */
function jsonConfigsFrom(document) {
  var config = {};
  var settingsElements =
    document.querySelectorAll('script.js-hypothesis-config');
  
  if (settingsElements.length) {
	  for (var i=0; i < settingsElements.length; i++) {
		  var settings;
		  try {
			  settings = JSON.parse(settingsElements[i].textContent);
		  } catch (err) {
			  console.warn('Could not parse settings from js-hypothesis-config tags', err);
			  settings = {};
		  }
		  assign(config, settings);
	  }
  } else {
	  // LEOS: For sidebar application the document is the iframe and then script.js-hypothesis-config
	  // is not on it. In this case configuration is taken from documentURI.
	  var url = new URL(document.documentURI);
	  var searchParams = new URLSearchParams(url.search);
	  if (searchParams.get('config') != null) {
		  config = JSON.parse(searchParams.get('config'));
		  assign(config, {'sidebarAppUrl' : document.baseURI + 'app.html'});
	  }
  }
  
  return config;
}

module.exports = {
  jsonConfigsFrom: jsonConfigsFrom,
};
