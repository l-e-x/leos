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

var retryUtil = require('../../../src/sidebar/util/retry');
var events = require('../../../src/sidebar/events');

function leosOauthAuthDecorator($provide) {
  $provide.decorator('auth', ['$delegate', '$injector', '$rootScope', '$http', function oauthAuthDecorator($delegate, $injector, $rootScope, $http) {
    var bridge = $injector.get('bridge');
    var localStorage = $injector.get('localStorage');
    var settings = $injector.get('settings');
    var frameConnected = false;
    var hostToken;

    function storageKey() {
      var apiDomain = new URL(settings.apiUrl).hostname;
      apiDomain = apiDomain.replace(/\./g, '%2E');
      return `hypothesis.oauth.${apiDomain}.token`;
    }

    function saveToken(token) {
      localStorage.setObject(storageKey(), token);
    }

    function waitForFrameConnection() {
      return retryUtil.retryPromiseOperation(function () {
        return new Promise(function(resolve, reject) {
          if (!frameConnected) {
            $rootScope.$on(events.FRAME_CONNECTED, function(event, error) {
              frameConnected = true;
              resolve(true);
            });
          } else {
            resolve(true);
          }
        });
      }, {
        retries: 5,
        minTimeout: 500,
        maxTimeout: 2000
      });
    }

    function requestHostToken() {
      return new Promise( function(resolve, reject) {
          var promiseTimeout = setTimeout(() => reject('timeout'), 500);
          bridge.call('requestSecurityToken', function (error, result) {
            clearTimeout(promiseTimeout);
          if (error) {
            return reject(error);
          } else {
            return resolve(result);
          }
        })
      });
    }

    function getHostToken() {
      return waitForFrameConnection().then(result => {
        return requestHostToken();
      }).catch(err => {
        return Promise.reject(err);
      });
    }

    var oldTokenGetter = $delegate.tokenGetter;
    $delegate.tokenGetter = function() {
      if (hostToken && Date.now() <= hostToken.expiresAt) {
        return Promise.resolve(hostToken.accessToken);
      }
      return Promise.all([getHostToken(), $delegate.getOauthClient()]).then(function([[grantToken], client]) {
        if (!grantToken) {
          throw "token is null";
        }
        else {
          return client.exchangeGrantToken(grantToken).then(function (resultToken) {
            hostToken = resultToken;
            saveToken(hostToken);
            return hostToken.accessToken;
          }).catch(err => {
            throw "Invalid token";
          });
        }
      }).catch(err => {
        return oldTokenGetter();
      });
    }
    return $delegate;
  }]);
}

module.exports = leosOauthAuthDecorator;