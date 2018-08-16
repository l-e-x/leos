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

var fixtures = require('./annotation-fixtures');
var uiConstants = require('../ui-constants');
var tabs = require('../tabs');
var unroll = require('../../shared/test/util').unroll;

describe('tabs', function () {
  describe('tabForAnnotation', function () {
    unroll('shows annotation in correct tab', function (testCase) {
      var ann = testCase.ann;
      var expectedTab = testCase.expectedTab;
      assert.equal(tabs.tabForAnnotation(ann), expectedTab);
    }, [{
      ann: fixtures.defaultAnnotation(),
      expectedTab: uiConstants.TAB_ANNOTATIONS,
    },{
      ann: fixtures.oldPageNote(),
      expectedTab: uiConstants.TAB_NOTES,
    },{
      ann: Object.assign(fixtures.defaultAnnotation(), {$orphan: true}),
      expectedTab: uiConstants.TAB_ORPHANS,
    }]);
  });

  describe('shouldShowInTab', function () {
    unroll('returns true if the annotation should be shown', function (testCase) {
      var ann = fixtures.defaultAnnotation();
      ann.$anchorTimeout = testCase.anchorTimeout;
      ann.$orphan = testCase.orphan;

      assert.equal(tabs.shouldShowInTab(ann, uiConstants.TAB_ANNOTATIONS), testCase.expectedTab === uiConstants.TAB_ANNOTATIONS);
      assert.equal(tabs.shouldShowInTab(ann, uiConstants.TAB_ORPHANS), testCase.expectedTab === uiConstants.TAB_ORPHANS);
    }, [{
      // Anchoring in progress.
      anchorTimeout: false,
      orphan: undefined,
      expectedTab: null,
    },{
      // Anchoring succeeded.
      anchorTimeout: false,
      orphan: false,
      expectedTab: uiConstants.TAB_ANNOTATIONS,
    },{
      // Anchoring failed.
      anchorTimeout: false,
      orphan: true,
      expectedTab: uiConstants.TAB_ORPHANS,
    },{
      // Anchoring timed out.
      anchorTimeout: true,
      orphan: undefined,
      expectedTab: uiConstants.TAB_ANNOTATIONS,
    },{
      // Anchoring initially timed out but eventually
      // failed.
      anchorTimeout: true,
      orphan: true,
      expectedTab: uiConstants.TAB_ORPHANS,
    }]);
  });

  describe('counts', function () {
    var annotation = Object.assign(fixtures.defaultAnnotation(), {$orphan:false});
    var orphan = Object.assign(fixtures.defaultAnnotation(), {$orphan:true});

    it('counts Annotations and Orphans separately', function () {
      assert.deepEqual(tabs.counts([annotation, orphan], true), {
        anchoring: 0,
        annotations: 1,
        notes: 0,
        orphans: 1,
      });
    });
  });
});
