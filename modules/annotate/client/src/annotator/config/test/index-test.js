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

var proxyquire = require('proxyquire');
var util = require('../../../shared/test/util');

var fakeSettingsFrom = sinon.stub();

var configFrom = proxyquire('../index', util.noCallThru({
  './settings': fakeSettingsFrom,
}));

describe('annotator.config.index', function() {

  beforeEach('reset fakeSettingsFrom', function() {
    fakeSettingsFrom.reset();
    fakeSettingsFrom.returns({
      hostPageSetting: sinon.stub(),
    });
  });

  it('gets the configuration settings', function() {
    configFrom('WINDOW');

    assert.calledOnce(fakeSettingsFrom);
    assert.calledWithExactly(fakeSettingsFrom, 'WINDOW');
  });

  [
    'sidebarAppUrl',
    'query',
    'annotations',
    'showHighlights',
  ].forEach(function(settingName) {
    it('returns the ' + settingName + ' setting', function() {
      fakeSettingsFrom()[settingName] = 'SETTING_VALUE';

      var config = configFrom('WINDOW');

      assert.equal(config[settingName], 'SETTING_VALUE');
    });
  });

  context("when there's no application/annotator+html <link>", function() {
    beforeEach('remove the application/annotator+html <link>', function() {
      Object.defineProperty(
        fakeSettingsFrom(),
        'sidebarAppUrl',
        {
          get: sinon.stub().throws(new Error("there's no link")),
        }
      );
    });

    it('throws an error', function() {
      assert.throws(
        function() { configFrom('WINDOW'); },
        "there's no link"
      );
    });
  });

  [
    'assetRoot',
    'subFrameIdentifier',
    'openSidebar',
  ].forEach(function(settingName) {
    it('reads ' + settingName + ' from the host page, even when in a browser extension', function() {
      configFrom('WINDOW');

      assert.calledWithExactly(
        fakeSettingsFrom().hostPageSetting,
        settingName, {allowInBrowserExt: true}
      );
    });
  });

  [
    'branding',
    'services',
  ].forEach(function(settingName) {
    it('reads ' + settingName + ' from the host page only when in an embedded client', function() {
      configFrom('WINDOW');

      assert.calledWithExactly(fakeSettingsFrom().hostPageSetting, settingName);
    });
  });

  [
    'assetRoot',
    'openSidebar',
    'branding',
    'services',
  ].forEach(function(settingName) {
    it('returns the ' + settingName + ' value from the host page', function() {
      var settings = {
        'assetRoot': 'chrome-extension://1234/client/',
        'openSidebar': 'OPEN_SIDEBAR_SETTING',
        'branding': 'BRANDING_SETTING',
        'services': 'SERVICES_SETTING',
      };
      fakeSettingsFrom().hostPageSetting = function(settingName) {
        return settings[settingName];
      };

      var settingValue = configFrom('WINDOW')[settingName];

      assert.equal(settingValue, settings[settingName]);
    });
  });
});
