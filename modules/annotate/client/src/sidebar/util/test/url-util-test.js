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

var urlUtil = require('../url-util');

describe('url-util', function () {
  describe('replaceURLParams()', function () {
    it('should replace params in URLs', function () {
      var replaced = urlUtil.replaceURLParams('http://foo.com/things/:id',
        {id: 'test'});
      assert.equal(replaced.url, 'http://foo.com/things/test');
    });

    it('should URL encode params in URLs', function () {
      var replaced = urlUtil.replaceURLParams('http://foo.com/things/:id',
        {id: 'foo=bar'});
      assert.equal(replaced.url, 'http://foo.com/things/foo%3Dbar');
    });

    it('should return unused params', function () {
      var replaced = urlUtil.replaceURLParams('http://foo.com/:id',
        {id: 'test', 'q': 'unused'});
      assert.deepEqual(replaced.params, {q: 'unused'});
    });
  });
});
