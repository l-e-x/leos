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
 * @ngdoc directive
 * @name helpPanel
 * @description Displays product version and environment info
 */
// @ngInject
module.exports = {
  controllerAs: 'vm',
  // @ngInject
  controller: function ($scope, $window, store, serviceUrl) {
    this.userAgent = $window.navigator.userAgent;
    this.version = '__VERSION__';  // replaced by versionify
    this.dateTime = new Date();
    this.serviceUrl = serviceUrl;

    $scope.$watch(
      function () {
        return store.frames();
      },
      function (frames) {
        if (frames.length === 0) {
          return;
        }
        this.url = frames[0].uri;
        this.documentFingerprint = frames[0].metadata.documentFingerprint;
      }.bind(this)
    );
  },
  template: require('../templates/help-panel.html'),
  bindings: {
    auth: '<',
    onClose: '&',
  },
};
