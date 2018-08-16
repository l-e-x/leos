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

var angular = require('angular');

var unroll = require('../../../shared/test/util').unroll;
var util = require('./util');

function testComponent() {
  return {
    bindToController: true,
    controllerAs: 'vm',
    controller: function () {
      this.tapCount = 0;
      this.tap = function () {
        ++this.tapCount;
      };
    },
    restrict: 'E',
    template: '<div h-on-touch="vm.tap()">Tap me</div>',
  };
}

describe('hOnTouch', function () {
  var testEl;

  before(function () {
    angular.module('app', [])
      .directive('hOnTouch', require('../h-on-touch'))
      .directive('test', testComponent);
  });

  beforeEach(function () {
    angular.mock.module('app');
    testEl = util.createDirective(document, 'test', {});
  });

  unroll('calls the handler when activated with a "#event" event', function (testCase) {
    util.sendEvent(testEl[0].querySelector('div'), testCase.event);
    assert.equal(testEl.ctrl.tapCount, 1);
  },[{
    event: 'touchstart',
  },{
    event: 'mousedown',
  },{
    event: 'click',
  }]);
});
