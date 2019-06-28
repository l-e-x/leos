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

var serviceConfig = require('../service-config');

describe('serviceConfig', function () {
  it('returns null if services is not an array', function () {
    var settings = {
      services: 'someString',
    };

    assert.isNull(serviceConfig(settings));
  });

  it('returns null if the settings object has no services', function () {
    var settings = {
      services: [],
    };

    assert.isNull(serviceConfig(settings));
  });

  it('returns the first service in the settings object', function () {
    var settings = {
      services: [{
        key: 'val',
      }],
    };

    assert.deepEqual(settings.services[0], serviceConfig(settings));
  });
});
