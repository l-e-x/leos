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

var addAnalytics = require('../../src/sidebar/ga');
var disableOpenerForExternalLinks = require('../../src/sidebar/util/disable-opener-for-external-links');
var apiUrls = require('../../src/sidebar/get-api-url');
var serviceConfig = require('../../src/sidebar/service-config');
var crossOriginRPC = require('../../src/sidebar/cross-origin-rpc.js');
require('../../src/shared/polyfills');

var raven;

// Read settings rendered into sidebar app HTML by service/extension.
var settings = require('../../src/shared/settings').jsonConfigsFrom(document);

if (settings.raven) {
  // Initialize Raven. This is required at the top of this file
  // so that it happens early in the app's startup flow
  raven = require('../../src/sidebar/raven');
  raven.init(settings.raven);
}

var hostPageConfig = require('../../src/sidebar/host-config');
Object.assign(settings, hostPageConfig(window));

settings.apiUrl = apiUrls.getApiUrl(settings);

// Disable Angular features that are not compatible with CSP.
//
// See https://docs.angularjs.org/api/ng/directive/ngCsp
//
// The `ng-csp` attribute must be set on some HTML element in the document
// _before_ Angular is require'd for the first time.
document.body.setAttribute('ng-csp', '');

// Prevent tab-jacking.
disableOpenerForExternalLinks(document.body);

var angular = require('angular');

// autofill-event relies on the existence of window.angular so
// it must be require'd after angular is first require'd
require('autofill-event');

// Setup Angular integration for Raven
if (settings.raven) {
  raven.angularModule(angular);
} else {
  angular.module('ngRaven', []);
}

if(settings.googleAnalytics){
  addAnalytics(settings.googleAnalytics);
}

// @ngInject
function setupHttp($http, api) {
  //$http.defaults.headers.common['X-Client-Id'] = api.clientId;
}

// @ngInject
function configureLocation($locationProvider) {
  return $locationProvider.html5Mode({
    enabled: true,
    requireBase: false
  });  // Use HTML5 history
}

// @ngInject
function configureToastr(toastrConfig) {
  angular.extend(toastrConfig, {
    preventOpenDuplicates: true,
  });
}

// @ngInject
function configureCompile($compileProvider) {
  // Make component bindings available in controller constructor. When
  // pre-assigned bindings is off, as it is by default in Angular >= 1.6.0,
  // bindings are only available during and after the controller's `$onInit`
  // method.
  //
  // This migration helper is being removed in Angular 1.7.0. To see which
  // components need updating, look for uses of `preAssignBindingsEnabled` in
  // tests.
  $compileProvider.preAssignBindingsEnabled(true);
}

function processAppOpts() {
  if (settings.liveReloadServer) {
    require('../../src/sidebar/live-reload-client').connect(settings.liveReloadServer);
  }
}

module.exports = angular.module('sentapi', [
  // Angular addons which export the Angular module name
  // via module.exports
  require('angular-route'),
  require('angular-sanitize'),
  require('angular-toastr'),

  // Angular addons which do not export the Angular module
  // name via module.exports
  ['angulartics', require('angulartics')][0],
  ['angulartics.google.analytics', require('angulartics/src/angulartics-ga')][0],
  ['ngTagsInput', require('ng-tags-input')][0],
  ['ui.bootstrap', require('../../src/sidebar/vendor/ui-bootstrap-custom-tpls-0.13.4')][0],

  // Local addons
  'ngRaven', 
])

  // The root component for the application
  .component('annotateSentapiApp', require('./components/annotate-sentapi-app'))

  // UI components

  .service('unicode', require('../../src/sidebar/services/unicode'))
  .service('api', require('./services/api'))
  
  // Redux store
  .service('store', require('../../src/sidebar/store'))

  // Utilities
  .value('Discovery', require('../../src/shared/discovery'))
  .value('ExcerptOverflowMonitor', require('../../src/sidebar/util/excerpt-overflow-monitor'))
  .value('VirtualThreadList', require('../../src/sidebar/virtual-thread-list'))
  .value('random', require('../../src/sidebar/util/random'))
  .value('raven', require('../../src/sidebar/raven'))
  .value('serviceConfig', serviceConfig)
  .value('settings', settings)
  .value('time', require('../../src/sidebar/util/time'))
  .value('urlEncodeFilter', require('../../src/sidebar/filter/url').encode)

  .config(configureCompile)
  .config(configureLocation)
  .config(configureToastr)
  .value('serviceConfig', serviceConfig)
  .value('settings', settings)
  .run(setupHttp)
  .run(crossOriginRPC.server.start);

processAppOpts();

// Work around a check in Angular's $sniffer service that causes it to
// incorrectly determine that Firefox extensions are Chrome Packaged Apps which
// do not support the HTML 5 History API. This results Angular redirecting the
// browser on startup and thus the app fails to load.
// See https://github.com/angular/angular.js/blob/a03b75c6a812fcc2f616fc05c0f1710e03fca8e9/src/ng/sniffer.js#L30
if (window.chrome && !window.chrome.app) {
  window.chrome.app = {
    dummyAddedByHypothesisClient: true,
  };
}

var appEl = document.querySelector('annotate-sentapi-app');
angular.bootstrap(appEl, ['sentapi'], {strictDi: true});
