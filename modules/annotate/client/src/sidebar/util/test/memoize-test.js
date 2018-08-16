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

var memoize = require('../memoize');

describe('memoize', function () {
  var count = 0;
  var memoized;

  function square(arg) {
    ++count;
    return arg * arg;
  }

  beforeEach(function () {
    count = 0;
    memoized = memoize(square);
  });

  it('computes the result of the function', function () {
    assert.equal(memoized(12), 144);
  });

  it('does not recompute if the input is unchanged', function () {
    memoized(42);
    memoized(42);
    assert.equal(count, 1);
  });

  it('recomputes if the input changes', function () {
    memoized(42);
    memoized(39);
    assert.equal(count, 2);
  });
});
