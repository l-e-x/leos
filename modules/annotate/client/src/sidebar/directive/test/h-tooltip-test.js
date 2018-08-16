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

var util = require('./util');

function testComponent() {
  return {
    controller: function () {},
    restrict: 'E',
    template: '<div aria-label="Share" h-tooltip>Label</div>',
  };
}

describe('h-tooltip', function () {
  var targetEl;
  var tooltipEl;

  before(function () {
    angular.module('app', [])
      .directive('hTooltip', require('../h-tooltip'))
      .directive('test', testComponent);
  });

  beforeEach(function () {
    angular.mock.module('app');
    var testEl = util.createDirective(document, 'test', {});
    targetEl = testEl[0].querySelector('div');
    tooltipEl = document.querySelector('.tooltip');
  });

  afterEach(function () {
    var testEl = document.querySelector('test');
    testEl.parentNode.removeChild(testEl);
  });

  it('appears when the target is hovered', function () {
    util.sendEvent(targetEl, 'mouseover');
    assert.equal(tooltipEl.style.visibility, '');
  });

  it('sets the label from the target\'s "aria-label" attribute', function () {
    util.sendEvent(targetEl, 'mouseover');
    assert.equal(tooltipEl.textContent, 'Share');
  });

  it('sets the direction from the target\'s "tooltip-direction" attribute', function () {
    targetEl.setAttribute('tooltip-direction', 'up');
    util.sendEvent(targetEl, 'mouseover');
    assert.deepEqual(Array.from(tooltipEl.classList), ['tooltip','tooltip--up']);

    targetEl.setAttribute('tooltip-direction', 'down');
    util.sendEvent(targetEl, 'mouseover');
    assert.deepEqual(Array.from(tooltipEl.classList), ['tooltip','tooltip--down']);
  });

  it('disappears when the target is unhovered', function () {
    util.sendEvent(targetEl, 'mouseout');
    assert.equal(tooltipEl.style.visibility, 'hidden');
  });

  it('disappears when the target is destroyed', function () {
    util.sendEvent(targetEl, 'mouseover');
    angular.element(targetEl).scope().$broadcast('$destroy');
    assert.equal(tooltipEl.style.visibility, 'hidden');
  });
});
