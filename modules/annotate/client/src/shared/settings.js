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
