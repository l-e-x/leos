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

describe('searchStatusBar', function () {
  before(function () {
    angular.module('app', [])
      .component('searchStatusBar', require('../search-status-bar'));
  });

  beforeEach(function () {
    angular.mock.module('app');
  });

  context('when there is a filter', function () {
    it('should display the filter count', function () {
      var elem = util.createDirective(document, 'searchStatusBar', {
        filterActive: true,
        filterMatchCount: 5,
      });
      assert.include(elem[0].textContent, '5 search results');
    });
  });

  context('when there is a selection', function () {
    it('should display the "Show all annotations (2)" message when there are 2 annotations', function () {
      var msg = 'Show all annotations';
      var msgCount = '(2)';
      var elem = util.createDirective(document, 'searchStatusBar', {
        selectionCount: 1,
        totalAnnotations: 2,
        selectedTab: 'annotation',
      });
      var clearBtn = elem[0].querySelector('button');
      assert.include(clearBtn.textContent, msg);
      assert.include(clearBtn.textContent, msgCount);
    });

    it('should display the "Show all notes (3)" message when there are 3 notes', function () {
      var msg = 'Show all notes';
      var msgCount = '(3)';
      var elem = util.createDirective(document, 'searchStatusBar', {
        selectionCount: 1,
        totalNotes: 3,
        selectedTab: 'note',
      });
      var clearBtn = elem[0].querySelector('button');
      assert.include(clearBtn.textContent, msg);
      assert.include(clearBtn.textContent, msgCount);
    });
  });
});
