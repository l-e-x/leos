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

var configFuncSettingsFrom = require('../config-func-settings-from');

describe('annotator.config.configFuncSettingsFrom', function() {
  var sandbox = sinon.sandbox.create();

  afterEach('reset the sandbox', function() {
    sandbox.restore();
  });

  context("when there's no window.hypothesisConfig() function", function() {
    it('returns {}', function() {
      var fakeWindow = {};

      assert.deepEqual(configFuncSettingsFrom(fakeWindow), {});
    });
  });

  context("when window.hypothesisConfig() isn't a function", function() {
    beforeEach('stub console.warn()', function() {
      sandbox.stub(console, 'warn');
    });

    function fakeWindow() {
      return {hypothesisConfig: 42};
    }

    it('returns {}', function() {
      assert.deepEqual(configFuncSettingsFrom(fakeWindow()), {});
    });

    it('logs a warning', function() {
      configFuncSettingsFrom(fakeWindow());

      assert.calledOnce(console.warn);
      assert.isTrue(console.warn.firstCall.args[0].startsWith(
        'hypothesisConfig must be a function'
      ));
    });
  });

  context('when window.hypothesisConfig() is a function', function() {
    it('returns whatever window.hypothesisConfig() returns', function () {
      // It just blindly returns whatever hypothesisConfig() returns
      // (even if it's not an object).
      var fakeWindow = { hypothesisConfig: sinon.stub().returns(42) };

      assert.equal(configFuncSettingsFrom(fakeWindow), 42);
    });
  });
});
