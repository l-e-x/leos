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

/**
 * Defines a set of vendor bundles which are
 * libraries of 3rd-party code referenced by
 * one or more bundles of the Hypothesis client/frontend.
 */

module.exports = {
  bundles: {
    jquery: ['jquery'],
    //polyfills: [require.resolve('../../src/shared/polyfills')],// LEOS:not working for windows build
    angular: [
      'angular',
      'angular-route',
      'angular-sanitize',
      'ng-tags-input',
      'angular-toastr',
      'angulartics/src/angulartics',
      'angulartics/src/angulartics-ga',
    ],
    katex: ['katex'],
    showdown: ['showdown'],
    unorm: ['unorm'],
    raven: ['raven-js'],
  },

  // List of modules to exclude from parsing for require() statements.
  //
  // Modules may be excluded from parsing for two reasons:
  //
  // 1. The module is large (eg. jQuery) and contains no require statements,
  //    so skipping parsing speeds up the build process.
  // 2. The module is itself a compiled Browserify bundle containing
  //    internal require() statements, which should not be processed
  //    when including the bundle in another project.
  noParseModules: [
    'jquery',
  ],
};
