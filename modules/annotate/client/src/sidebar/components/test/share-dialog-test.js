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

describe('shareDialog', function () {
  var fakeAnalytics;
  var fakeStore;

  beforeEach(function () {
    fakeAnalytics = {
      track: sinon.stub(),
      events: {},
    };
    fakeStore = { frames: sinon.stub().returns([]) };

    angular.module('h', [])
      .component('shareDialog', require('../share-dialog'))
      .value('analytics', fakeAnalytics)
      .value('store', fakeStore)
      .value('urlEncodeFilter', function (val) { return val; });
    angular.mock.module('h');
  });

  it('generates new via link', function () {
    var element = util.createDirective(document, 'shareDialog', {});
    fakeStore.frames.returns([{ uri: 'http://example.com' }]);
    element.scope.$digest();
    assert.equal(element.ctrl.viaPageLink, 'https://via.hypothes.is/http://example.com');
  });

  it('does not generate new via link if already on via', function () {
    var element = util.createDirective(document, 'shareDialog', {});
    fakeStore.frames.returns([{
      uri: 'https://via.hypothes.is/http://example.com',
    }]);
    element.scope.$digest();
    assert.equal(element.ctrl.viaPageLink, 'https://via.hypothes.is/http://example.com');
  });

  it('tracks the target being shared', function(){

    var element = util.createDirective(document, 'shareDialog');
    var clickShareIcon = function(iconName){
      element.find('.' + iconName).click();
    };

    clickShareIcon('h-icon-twitter');
    assert.equal(fakeAnalytics.track.args[0][1], 'twitter');
    clickShareIcon('h-icon-facebook');
    assert.equal(fakeAnalytics.track.args[1][1], 'facebook');
    clickShareIcon('h-icon-google-plus');
    assert.equal(fakeAnalytics.track.args[2][1], 'googlePlus');
    clickShareIcon('h-icon-mail');
    assert.equal(fakeAnalytics.track.args[3][1], 'email');
  });
});
