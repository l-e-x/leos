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

var fakeStore = require('../../test/fake-redux-store');
var stateUtil = require('../state-util');

describe('state-util', function () {
  var store;

  beforeEach(function() {
    store = fakeStore({ val: 0 });
  });

  describe('awaitStateChange()', function () {
    function getValWhenGreaterThanTwo(store) {
      if (store.getState().val < 3) {
        return null;
      }
      return store.getState().val;
    }

    it('should return promise that resolves to a non-null value', function () {
      var expected = 5;

      store.setState({ val: 5 });

      return stateUtil.awaitStateChange(store, getValWhenGreaterThanTwo).then(function (actual) {
        assert.equal(actual, expected);
      });
    });

    it('should wait for awaitStateChange to return a non-null value', function () {
      var valPromise;
      var expected = 5;

      store.setState({ val: 2 });
      valPromise = stateUtil.awaitStateChange(store, getValWhenGreaterThanTwo);
      store.setState({ val: 5 });

      return valPromise.then(function (actual) {
        assert.equal(actual, expected);
      });
    });
  });
});
