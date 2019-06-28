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

var isBrowserExtension = require('../is-browser-extension');

describe('annotator.config.isBrowserExtension', function() {
  [
    {
      url: 'chrome-extension://abcxyz',
      returns: true,
    },
    {
      url: 'moz-extension://abcxyz',
      returns: true,
    },
    {
      url: 'ms-browser-extension://abcxyz',
      returns: true,
    },
    {
      url: 'http://partner.org',
      returns: false,
    },
    {
      url: 'https://partner.org',
      returns: false,
    },
    // It considers anything not http(s) to be a browser extension.
    {
      url: 'ftp://partner.org',
      returns: true,
    },
  ].forEach(function(test) {
    it('returns ' + test.returns + ' for ' + test.url, function() {
      assert.equal(isBrowserExtension(test.url), test.returns);
    });
  });
});
