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

var istanbul = require('browserify-istanbul');

module.exports = function(config) {
  config.set({

    // base path that will be used to resolve all patterns (eg. files, exclude)
    basePath: './',

    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: [
      'browserify',
      'mocha',
      'chai',
      'sinon',
    ],

    // list of files / patterns to load in the browser
    files: [
      // Polyfills for PhantomJS
      './shared/polyfills.js',

      // Test setup
      './sidebar/test/bootstrap.js',

      // Empty HTML file to assist with some tests
      { pattern: './annotator/test/empty.html', watched: false },

      // Karma watching is disabled for these files because they are
      // bundled with karma-browserify which handles watching itself via
      // watchify

      // Unit tests
      { pattern: '**/*-test.coffee', watched: false, included: true, served: true },
      { pattern: '../leos/**/*-test.coffee', watched: false, included: true, served: true }, // LEOS Change include leos specific tests
      { pattern: '**/test/*-test.js', watched: false, included: true, served: true },
      { pattern: '../leos/**/test/*-test.js', watched: false, included: true, served: true }, // LEOS Change include leos specific tests

      // Integration tests
      { pattern: '**/integration/*-test.js', watched: false, included: true, served: true },
    ],

    // list of files to exclude
    exclude: [
    ],

    // preprocess matching files before serving them to the browser
    // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
    preprocessors: {
      './shared/polyfills.js': ['browserify'],
      './sidebar/test/bootstrap.js': ['browserify'],
      '**/*-test.js': ['browserify'],
      '**/*-test.coffee': ['browserify'],
      '../leos/**/*-test.js': ['browserify'],                   // LEOS Change include leos specific tests
      '../leos/**/*-test.coffee': ['browserify'],               // LEOS Change include leos specific tests
      '**/*-it.js': ['browserify'],
    },

    browserify: {
      debug: true,
      extensions: ['.coffee'],
      configure: function (bundle) {
        bundle.plugin('proxyquire-universal');
      },

      transform: [
        'coffeeify',
        istanbul({
          ignore: [
            // Third party code
            '**/node_modules/**', '**/vendor/*',
            // Non JS modules
            '**/*.html', '**/*.svg',
          ],

          // There is an outstanding bug with karma-coverage and istanbul
          // in regards to doing source mapping and transpiling CoffeeScript.
          // The least bad work around is to replace the instrumenter with
          // isparta and it will handle doing the re mapping for us.
          // This issue follows the issue and attempts to fix it:
          // https://github.com/karma-runner/karma-coverage/issues/157
          instrumenter: require('isparta'),
        }),
        'babelify',
      ],
    },

    mochaReporter: {
      // Display a helpful diff when comparing complex objects
      // See https://www.npmjs.com/package/karma-mocha-reporter#showdiff
      showDiff: true,
      // Only show the total test counts and details for failed tests
      output: 'minimal',
    },

    coverageReporter: {
      dir: '../coverage/',
      reporters: [
        {type:'html'},
        {type:'json', subdir: './'},
      ],
    },

    // Use https://www.npmjs.com/package/karma-mocha-reporter
    // for more helpful rendering of test failures
    reporters: ['mocha', 'coverage'],

    // web server port
    port: 9876,

    // enable / disable colors in the output (reporters and logs)
    colors: true,

    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,

    browserConsoleLogOptions: {
      level: 'log',
      format: '%b %T: %m',
      terminal: true,
    },

    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,

    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['PhantomJS'],
    browserNoActivityTimeout: 20000, // Travis is slow...

    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: false,

    // Log slow tests so we can fix them before they timeout
    reportSlowerThan: 500,
  });
};
