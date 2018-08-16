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

var util = require('../../directive/test/util');

describe('searchInput', function () {
  var fakeHttp;

  before(function () {
    angular.module('app', [])
      .component('searchInput', require('../search-input'));
  });

  beforeEach(function () {
    fakeHttp = {pendingRequests: []};
    angular.mock.module('app', {
      $http: fakeHttp,
    });
  });

  it('displays the search query', function () {
    var el = util.createDirective(document, 'searchInput', {
      query: 'foo',
    });
    var input = el.find('input')[0];
    assert.equal(input.value, 'foo');
  });

  it('invokes #onSearch() when the query changes', function () {
    var onSearch = sinon.stub();
    var el = util.createDirective(document, 'searchInput', {
      query: 'foo',
      onSearch: {
        args: ['$query'],
        callback: onSearch,
      },
    });
    var input = el.find('input')[0];
    var form = el.find('form');
    input.value = 'new-query';
    form.submit();
    assert.calledWith(onSearch, 'new-query');
  });

  describe('loading indicator', function () {
    it('is hidden when there are no network requests in flight', function () {
      var el = util.createDirective(document, 'search-input', {});
      var spinner = el[0].querySelector('.spinner');
      assert.equal(util.isHidden(spinner), true);
    });

    it('is visible when there are network requests in flight', function () {
      var el = util.createDirective(document, 'search-input', {});
      var spinner = el[0].querySelector('.spinner');
      fakeHttp.pendingRequests.push([{}]);
      el.scope.$digest();
      assert.equal(util.isHidden(spinner), false);
    });
  });
});
