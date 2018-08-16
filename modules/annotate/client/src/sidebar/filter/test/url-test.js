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

var url = require('../url');

describe('url.encode', function () {
  it('urlencodes its input', function () {
    var expect = 'http%3A%2F%2Ffoo.com%2Fhello%20there.pdf';
    var result = url.encode('http://foo.com/hello there.pdf');

    assert.equal(result, expect);
  });

  it('returns the empty string for null values', function () {
    assert.equal(url.encode(null), '');
    assert.equal(url.encode(undefined), '');
  });
});
