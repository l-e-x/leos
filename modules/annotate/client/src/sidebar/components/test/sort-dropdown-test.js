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

describe('sortDropdown', function () {
  before(function () {
    angular.module('app', [])
      .component('sortDropdown', require('../sort-dropdown'));
  });

  beforeEach(function () {
    angular.mock.module('app');
  });

  it('should update the sort key on click', function () {
    var changeSpy = sinon.spy();
    var elem = util.createDirective(document, 'sortDropdown', {
      sortKeysAvailable: ['Newest', 'Oldest'],
      sortKey: 'Newest',
      onChangeSortKey: {
        args: ['sortKey'],
        callback: changeSpy,
      },
    });
    var links = elem.find('li');
    angular.element(links[0]).click();
    assert.calledWith(changeSpy, 'Newest');
  });
});
