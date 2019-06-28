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

var hostPageConfig = require('../host-config');

function fakeWindow(config) {
  return {
    location: {
      search: '?config=' + JSON.stringify(config),
    },
  };
}

describe('hostPageConfig', function () {
  it('parses config from location string and returns whitelisted params', function () {
    var window_ = fakeWindow({
      annotations: '1234',
      appType: 'bookmarklet',
      openSidebar: true,
      showHighlights: true,
      services: [{
        authority: 'hypothes.is',
      }],
    });

    assert.deepEqual(hostPageConfig(window_), {
      annotations: '1234',
      appType: 'bookmarklet',
      openSidebar: true,
      showHighlights: true,
      services: [{
        authority: 'hypothes.is',
      }],
    });
  });

  it('ignores non-whitelisted config params', function () {
    var window_ = fakeWindow({
      apiUrl: 'https://not-the-hypothesis/api/',
    });

    assert.deepEqual(hostPageConfig(window_), {});
  });

  it('ignores `null` values in config', function () {
    var window_ = fakeWindow({
      openSidebar: null,
    });

    assert.deepEqual(hostPageConfig(window_), {});
  });
});
