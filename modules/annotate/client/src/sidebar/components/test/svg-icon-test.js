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

var angular = require('angular');

var util = require('../../directive/test/util');

describe('svgIcon', function () {
  before(function () {
    angular.module('app', [])
      .component('svgIcon', require('../svg-icon'))
      .config(($compileProvider) => $compileProvider.preAssignBindingsEnabled(true));
  });

  beforeEach(function () {
    angular.mock.module('app');
  });

  it("sets the element's content to the content of the SVG", function () {
    var el = util.createDirective(document, 'svgIcon', {name: 'refresh'});
    assert.ok(el[0].querySelector('svg'));
  });

  it('throws an error if the icon is unknown', function () {
    assert.throws(function () {
      util.createDirective(document, 'svgIcon', {name: 'unknown'});
    });
  });
});
