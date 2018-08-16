/*
 * Copyright 2018 European Commission
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

var getApiUrl = require('../get-api-url');

describe('sidebar.getApiUrl', function () {
  context('when there is a service object in settings', function () {
    it('returns apiUrl from the service object', function () {
      var settings = {
        apiUrl: 'someApiUrl',
        services: [{
          apiUrl: 'someOtherApiUrl',
        }],
      };
      assert.equal(getApiUrl(settings), settings.services[0].apiUrl);
    });
  });

  context('when there is no service object in settings', function () {
    it('returns apiUrl from the settings object', function () {
      var settings = {
        apiUrl: 'someApiUrl',
      };
      assert.equal(getApiUrl(settings), settings.apiUrl);
    });
  });

  context('when there is a service object in settings but does not contain an apiUrl key', function () {
    it('throws error', function () {
      var settings = {
        apiUrl: 'someApiUrl',
        services: [{}],
      };
      assert.throws(
        function() { getApiUrl(settings); },
        Error,
        'Service should contain an apiUrl value');
    });
  });
});
