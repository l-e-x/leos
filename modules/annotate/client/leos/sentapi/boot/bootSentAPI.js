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

function injectStylesheet(doc, href) {
  var link = doc.createElement('link');
  link.rel = 'stylesheet';
  link.type = 'text/css';
  link.href = href;
  doc.head.appendChild(link);
}

function injectScript(doc, src) {
  var script = doc.createElement('script');
  script.type = 'text/javascript';
  script.src = src;

  // Set 'async' to false to maintain execution order of scripts.
  // See https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script
  script.async = false;
  doc.head.appendChild(script);
}

function injectAssets(doc, config, assets) {
  assets.forEach(function (path) {
    var url = config.assetRoot + '/' + config.manifest[path]; //LEOS changes
    if (url.match(/\.css/)) {
      injectStylesheet(doc, url);
    } else {
      injectScript(doc, url);
    }
  });
}

/**
 * Bootstrap the sidebar application which displays annotations.
 */
function bootSentAPIApp(doc, config) {
  injectAssets(doc, config, [
    // Vendor code and polyfills required by app.bundle.js
    'scripts/raven.bundle.js',
    'scripts/angular.bundle.js',
    'scripts/katex.bundle.js',
    'scripts/showdown.bundle.js',
    'scripts/polyfills.bundle.js',
    'scripts/unorm.bundle.js',

    // The sentapi app
    'scripts/sentapi.bundle.js',

    'styles/angular-csp.css',
    'styles/angular-toastr.css',
    'styles/icomoon.css',
    'styles/katex.min.css',
    'styles/sidebar.css',
    'styles/sentapi.css',
  ]);
}

function injectApp(doc) {
  var app = doc.createElement('annotate-sentapi-app');
  app.async = false;
  doc.body.appendChild(app);
}

function boot(document_, config) {
  bootSentAPIApp(document_, config);
  injectApp(document);
}

module.exports = boot;
