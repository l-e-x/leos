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

var configFrom = require('./config/index');
require('../shared/polyfills');


// Polyfills

// document.evaluate() implementation,
// required by IE 10, 11
//
// This sets `window.wgxpath`
if (!window.document.evaluate) {
  require('./vendor/wgxpath.install');
}
if (window.wgxpath) {
  window.wgxpath.install();
}

var $ = require('jquery');

// Applications
var Guest = require('./guest');
var Sidebar = require('./sidebar');
var PdfSidebar = require('./pdf-sidebar');
var LeosSidebar = require('./../../leos/annotator/leos-sidebar'); // LEOS Change
var config = configFrom(window);
var leos = document.querySelector(`${config.annotationContainer} ${config.leosDocumentRootNode}`);//TODO : have a better check to identify AKN LEOS Change

var pluginClasses = {
  // UI plugins
  BucketBar: require('./plugin/bucket-bar'),
  Toolbar: require('./plugin/toolbar'),

  // Document type plugins
  PDF: require('./plugin/pdf'),
  Document: leos
    ? require('./../../leos/annotator/plugin/leos-document')
    : require('./plugin/document'), //LEOS Change
  // Cross-frame communication
  CrossFrame: require('./plugin/cross-frame'),
};

var appLinkEl = document.querySelector('link[type="application/annotator+html"][rel="sidebar"]');

$.noConflict(true)(function () {

  var Klass = leos
    ? LeosSidebar // LEOS Change
    : Sidebar;

  if (window.PDFViewerApplication) {
    Klass = PdfSidebar; // LEOS Change
  }

  if (config.hasOwnProperty('constructor')) {
    Klass = config.constructor;
    delete config.constructor;
  }

  if (config.subFrameIdentifier) {
    // Make sure the PDF plugin is loaded if the subframe contains the PDF.js viewer.
    if (typeof window.PDFViewerApplication !== 'undefined') {
      config.PDF = {};
    }
    Klass = Guest;

    // Other modules use this to detect if this
    // frame context belongs to hypothesis.
    // Needs to be a global property that's set.
    window.__hypothesis_frame = true;
  }

  if(config.theme === 'clean') {
    delete pluginClasses.BucketBar;
  }

  config.pluginClasses = pluginClasses;

  var annotator = new Klass(document.body, config);
  appLinkEl.addEventListener('destroy', function () {
    appLinkEl.parentElement.removeChild(appLinkEl);
    annotator.destroy();
  });
});
