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

var observable = require('../observable');

describe('observable', function () {
  describe('delay()', function () {
    var clock;

    beforeEach(function () {
      clock = sinon.useFakeTimers();
    });

    afterEach(function () {
      clock.restore();
    });

    it('defers events', function () {
      var received = [];
      var obs = observable.delay(50, observable.Observable.of('foo'));
      obs.forEach(function (v) {
        received.push(v);
      });
      assert.deepEqual(received, []);
      clock.tick(100);
      assert.deepEqual(received, ['foo']);
    });

    it('delivers events in sequence', function () {
      var received = [];
      var obs = observable.delay(10, observable.Observable.of(1,2));
      obs.forEach(function (v) {
        received.push(v);
      });
      clock.tick(20);
      assert.deepEqual(received, [1,2]);
    });
  });
});
