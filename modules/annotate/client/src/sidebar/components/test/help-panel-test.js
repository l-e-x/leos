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

var angular = require('angular');

describe('helpPanel', function () {
  var fakeStore;
  var $componentController;
  var $rootScope;

  beforeEach(function () {
    fakeStore = {
      frames: sinon.stub().returns([]),
    };

    angular.module('h', [])
      .component('helpPanel', require('../help-panel'));

    angular.mock.module('h', {
      store: fakeStore,
      serviceUrl: sinon.stub(),
    });

    angular.mock.inject(function (_$componentController_, _$rootScope_) {
      $componentController = _$componentController_;
      $rootScope = _$rootScope_;
    });
  });

  it('displays the URL and fingerprint of the first connected frame', function () {
    fakeStore.frames.returns([{
      uri: 'https://publisher.org/article.pdf',
      metadata: {
        documentFingerprint: '12345',
      },
    }]);

    var $scope = $rootScope.$new();
    var ctrl = $componentController('helpPanel', { $scope: $scope });
    $scope.$digest();

    assert.equal(ctrl.url, 'https://publisher.org/article.pdf');
    assert.equal(ctrl.documentFingerprint, '12345');
  });
});
