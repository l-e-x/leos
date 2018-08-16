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

var analyticsService = require('../analytics');

var createEventObj = function(override){
  return {
    category: override.category,
    label: override.label,
    metricValue: override.metricValue,
  };
};

describe('analytics', function () {

  var $analyticsStub;
  var $windowStub;
  var eventTrackStub;

  beforeEach(function () {
    $analyticsStub = {
      eventTrack: sinon.stub(),
    };

    eventTrackStub = $analyticsStub.eventTrack;

    $windowStub = {
      location: {
        href: '',
        protocol: 'https:',
      },
      document: {
        referrer: '',
      },
    };
  });

  describe('applying global category based on environment contexts', function () {

    it('sets the category to match the appType setting value', function(){
      var validTypes = ['chrome-extension', 'embed', 'bookmarklet', 'via'];
      validTypes.forEach(function(appType, index){
        analyticsService($analyticsStub, $windowStub, {appType: appType}).track('event' + index);
        assert.deepEqual(eventTrackStub.args[index], ['event' + index, createEventObj({category: appType})]);
      });
    });

    it('sets category as embed if no other matches can be made', function () {
      analyticsService($analyticsStub, $windowStub).track('eventA');
      assert.deepEqual(eventTrackStub.args[0], ['eventA', createEventObj({category: 'embed'})]);
    });

    it('sets category as via if url matches the via uri pattern', function () {
      $windowStub.document.referrer = 'https://via.hypothes.is/';
      analyticsService($analyticsStub, $windowStub).track('eventA');
      assert.deepEqual(eventTrackStub.args[0], ['eventA', createEventObj({category: 'via'})]);

      // match staging as well
      $windowStub.document.referrer = 'https://qa-via.hypothes.is/';
      analyticsService($analyticsStub, $windowStub).track('eventB');
      assert.deepEqual(eventTrackStub.args[1], ['eventB', createEventObj({category: 'via'})]);
    });

    it('sets category as chrome-extension if protocol matches chrome-extension:', function () {
      $windowStub.location.protocol = 'chrome-extension:';
      analyticsService($analyticsStub, $windowStub).track('eventA');
      assert.deepEqual(eventTrackStub.args[0], ['eventA', createEventObj({category: 'chrome-extension'})]);
    });

  });

  it('allows custom labels to be sent for an event', function () {
    analyticsService($analyticsStub, $windowStub, {appType: 'embed'}).track('eventA', 'labelA');
    assert.deepEqual(eventTrackStub.args[0], ['eventA', createEventObj({category: 'embed', label: 'labelA'})]);
  });

  it('allows custom metricValues to be sent for an event', function () {
    analyticsService($analyticsStub, $windowStub, {appType: 'embed'}).track('eventA', null, 242.2);
    assert.deepEqual(eventTrackStub.args[0], ['eventA', createEventObj({category: 'embed', metricValue: 242.2})]);
  });

  it('allows custom metricValues and labels to be sent for an event', function () {
    analyticsService($analyticsStub, $windowStub, {appType: 'embed'}).track('eventA', 'labelabc', 242.2);
    assert.deepEqual(eventTrackStub.args[0], ['eventA', createEventObj({category: 'embed', label: 'labelabc', metricValue: 242.2})]);
  });

});
