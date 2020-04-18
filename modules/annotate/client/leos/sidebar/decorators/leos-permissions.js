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

function leosPermissionsDecorator($provide) {
  $provide.decorator('permissions', ['$delegate', '$injector', '$rootScope', '$http', function permissionsDecorator($delegate, $injector, $rootScope, $http) {
    var bridge = $injector.get('bridge');
    var hostUserPermissions;

    function requestUserPermissions() {
      return new Promise( function(resolve, reject) {
        var timeout = setTimeout((() => reject('timeout')), 2000);
        bridge.call('requestUserPermissions', function (error, result) {
          clearTimeout(timeout);
          if (error) { return reject(error); } else { return resolve(result); }
        });
      });
    }

    requestUserPermissions().then(([permissions]) => {
      hostUserPermissions = permissions;
    }).catch(err => {
      hostUserPermissions = [];
    });

    $delegate.getUserPermissions = function() {
      if (!hostUserPermissions) {
        return [];
      }
      else {
        return hostUserPermissions;
      }
    };

    return $delegate;
  }]);
}

module.exports = leosPermissionsDecorator;