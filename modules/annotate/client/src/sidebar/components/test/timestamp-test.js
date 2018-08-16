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
var escapeStringRegexp = require('escape-string-regexp');

var util = require('../../directive/test/util');

describe('timestamp', function () {
  var clock;
  var fakeTime;

  before(function () {
    angular.module('app',[])
      .component('timestamp', require('../timestamp'));
  });

  beforeEach(function () {
    clock = sinon.useFakeTimers();
    fakeTime = {
      toFuzzyString: sinon.stub().returns('a while ago'),
      decayingInterval: function () {},
    };

    angular.mock.module('app', {
      time: fakeTime,
    });
  });

  afterEach(function() {
    clock.restore();
  });

  describe('#relativeTimestamp', function() {
    it('displays a relative time string', function() {
      var element = util.createDirective(document, 'timestamp', {
        timestamp: '2016-06-10T10:04:04.939Z',
      });
      assert.equal(element.ctrl.relativeTimestamp, 'a while ago');
    });

    it('is updated when the timestamp changes', function () {
      var element = util.createDirective(document, 'timestamp', {
        timestamp: '1776-07-04T10:04:04.939Z',
      });
      element.scope.timestamp = '1863-11-19T12:00:00.939Z';
      fakeTime.toFuzzyString.returns('four score and seven years ago');
      element.scope.$digest();
      assert.equal(element.ctrl.relativeTimestamp, 'four score and seven years ago');
    });

    it('is updated after time passes', function() {
      fakeTime.decayingInterval = function (date, callback) {
        setTimeout(callback, 10);
      };
      var element = util.createDirective(document, 'timestamp', {
        timestamp: '2016-06-10T10:04:04.939Z',
      });
      fakeTime.toFuzzyString.returns('60 jiffies');
      element.scope.$digest();
      clock.tick(1000);
      assert.equal(element.ctrl.relativeTimestamp, '60 jiffies');
    });

    it('is no longer updated after the component is destroyed', function() {
      var cancelRefresh = sinon.stub();
      fakeTime.decayingInterval = function () {
        return cancelRefresh;
      };
      var element = util.createDirective(document, 'timestamp', {
        timestamp: '2016-06-10T10:04:04.939Z',
      });
      element.ctrl.$onDestroy();
      assert.called(cancelRefresh);
    });
  });

  describe('#absoluteTimestamp', function () {
    it('displays the current time', function () {
      var expectedDate = new Date('2016-06-10T10:04:04.939Z');
      var element = util.createDirective(document, 'timestamp', {
        timestamp: expectedDate.toISOString(),
      });

      // The exact format of the result will depend on the current locale,
      // but check that at least the current year and time are present
      assert.match(element.ctrl.absoluteTimestamp, new RegExp('.*2016.*' +
        escapeStringRegexp(expectedDate.toLocaleTimeString())));
    });
  });
});
