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

var events = require('../../events');
var util = require('../../directive/test/util');

describe('newNoteBtn', function () {
  var $rootScope;
  var sandbox = sinon.sandbox.create();
  var fakeStore = {
    frames: sinon.stub().returns([{ id: null, uri: 'www.example.org'}, { id: '1', uri: 'www.example.org'}]),
  };

  before(function () {
    angular.module('app', [])
      .component('selectionTabs', require('../selection-tabs'))
      .component('newNoteBtn', require('../new-note-btn'));
  });

  beforeEach(function () {
    var fakeFeatures = {
      flagEnabled: sinon.stub().returns(true),
    };
    var fakeSettings = { theme: 'clean' };

    angular.mock.module('app', {
      store: fakeStore,
      features: fakeFeatures,
      settings: fakeSettings,
    });

    angular.mock.inject(function (_$componentController_, _$rootScope_) {
      $rootScope = _$rootScope_;
    });
  });

  afterEach(function() {
    sandbox.restore();
  });

  it('should broadcast BEFORE_ANNOTATION_CREATED event when the new note button is clicked', function () {
    var annot = {
      target: [],
      uri: 'www.example.org',
    };
    var elem = util.createDirective(document, 'newNoteBtn', {
      store: fakeStore,
    });
    sandbox.spy($rootScope, '$broadcast');
    elem.ctrl.onNewNoteBtnClick();
    assert.calledWith($rootScope.$broadcast, events.BEFORE_ANNOTATION_CREATED, annot);
  });
});
