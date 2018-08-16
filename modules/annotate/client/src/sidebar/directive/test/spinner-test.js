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

var module = angular.mock.module;
var inject = angular.mock.inject;

describe('spinner', function () {
  var $animate = null;
  var $element = null;
  var sandbox = null;

  before(function () {
    angular.module('h', []).directive('spinner', require('../spinner'));
  });

  beforeEach(module('h'));

  beforeEach(inject(function (_$animate_, $compile, $rootScope) {
    sandbox = sinon.sandbox.create();

    $animate = _$animate_;
    sandbox.spy($animate, 'enabled');

    $element = angular.element('<span class="spinner"></span>');
    $compile($element)($rootScope.$new());
  }));

  afterEach(function () {
    sandbox.restore();
  });

  it('disables ngAnimate animations for itself', function () {
    assert.calledWith($animate.enabled, false, sinon.match($element));
  });
});
