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

var frameSync = require('../../src/sidebar/services/frame-sync');

// @ngInject
function LeosFrameSync($injector, $rootScope, $window, Discovery, store, bridge) {
  $injector.invoke(frameSync.default, this, {$rootScope: $rootScope, $window: $window, Discovery: Discovery, store: store, bridge: bridge});
  let oldConnect = this.connect;
  this.connect = function() {
    oldConnect();
    // LEOS Change
    bridge.on('stateChangeHandler', function (state) {
      store.hostState = state;
    });
    bridge.on('reloadAnnotations', function (state) {
      $rootScope.$broadcast('reloadAnnotations');
    });
    // ----------
  }
};

module.exports = {
  default: LeosFrameSync,
  formatAnnot: frameSync.formatAnnot,
};