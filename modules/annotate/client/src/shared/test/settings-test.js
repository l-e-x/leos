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

var settings = require('../settings');

var sandbox = sinon.sandbox.create();

describe('settings', function () {

  afterEach('reset the sandbox', function() {
    sandbox.restore();
  });

  describe('#jsonConfigsFrom', function() {
    var jsonConfigsFrom = settings.jsonConfigsFrom;

    function appendJSHypothesisConfig(document_, jsonString) {
      var el = document_.createElement('script');
      el.type = 'application/json';
      el.textContent = jsonString;
      el.classList.add('js-hypothesis-config');
      el.classList.add('js-settings-test');
      document_.body.appendChild(el);
    }

    afterEach('remove js-hypothesis-config tags', function() {
      var elements = document.querySelectorAll('.js-settings-test');
      for (var i=0; i < elements.length; i++) {
        elements[i].remove();
      }
    });

    context('when there are no JSON scripts', function() {
      it('returns {}', function() {
        assert.deepEqual(jsonConfigsFrom(document), {});
      });
    });

    context("when there's JSON scripts with no top-level objects", function() {
      beforeEach('add JSON scripts with no top-level objects', function() {
        appendJSHypothesisConfig(document, 'null');
        appendJSHypothesisConfig(document, '23');
        appendJSHypothesisConfig(document, 'true');
      });

      it('ignores them', function() {
        assert.deepEqual(jsonConfigsFrom(document), {});
      });
    });

    context("when there's a JSON script with a top-level array", function() {
      beforeEach('add a JSON script containing a top-level array', function() {
        appendJSHypothesisConfig(document, '["a", "b", "c"]');
      });

      it('returns the array, parsed into an object', function() {
        assert.deepEqual(
          jsonConfigsFrom(document),
          {0: 'a', 1: 'b', 2: 'c'}
        );
      });
    });

    context("when there's a JSON script with a top-level string", function() {
      beforeEach('add a JSON script with a top-level string', function() {
        appendJSHypothesisConfig(document, '"hi"');
      });

      it('returns the string, parsed into an object', function() {
        assert.deepEqual(jsonConfigsFrom(document), {0: 'h', 1: 'i'});
      });
    });

    context("when there's a JSON script containing invalid JSON", function() {
      beforeEach('stub console.warn()', function() {
        sandbox.stub(console, 'warn');
      });

      beforeEach('add a JSON script containing invalid JSON', function() {
        appendJSHypothesisConfig(document, 'this is not valid json');
      });

      it('logs a warning', function() {
        jsonConfigsFrom(document);

        assert.called(console.warn);
      });

      it('returns {}', function() {
        assert.deepEqual(jsonConfigsFrom(document), {});
      });

      it('still returns settings from other JSON scripts', function() {
        appendJSHypothesisConfig(document, '{"foo": "FOO", "bar": "BAR"}');

        assert.deepEqual(jsonConfigsFrom(document), {foo: 'FOO', bar: 'BAR'});
      });
    });

    context("when there's a JSON script with an empty object", function() {
      beforeEach('add a JSON script containing an empty object', function() {
        appendJSHypothesisConfig(document, '{}');
      });

      it('ignores it', function() {
        assert.deepEqual(jsonConfigsFrom(document), {});
      });
    });

    context("when there's a JSON script containing some settings", function() {
      beforeEach('add a JSON script containing some settings', function() {
        appendJSHypothesisConfig(document, '{"foo": "FOO", "bar": "BAR"}');
      });

      it('returns the settings', function() {
        assert.deepEqual(
          jsonConfigsFrom(document),
          {foo: 'FOO', bar: 'BAR'}
        );
      });
    });

    context('when there are JSON scripts with different settings', function() {
      beforeEach('add some JSON scripts with different settings', function() {
        appendJSHypothesisConfig(document, '{"foo": "FOO"}');
        appendJSHypothesisConfig(document, '{"bar": "BAR"}');
        appendJSHypothesisConfig(document, '{"gar": "GAR"}');
      });

      it('merges them all into one returned object', function() {
        assert.deepEqual(
          jsonConfigsFrom(document),
          {foo: 'FOO', bar: 'BAR', gar: 'GAR'}
        );
      });
    });

    context('when multiple JSON scripts contain the same setting', function() {
      beforeEach('add some JSON scripts with different settings', function() {
        appendJSHypothesisConfig(document, '{"foo": "first"}');
        appendJSHypothesisConfig(document, '{"foo": "second"}');
        appendJSHypothesisConfig(document, '{"foo": "third"}');
      });

      specify('settings from later in the page override ones from earlier', function() {
        assert.equal(jsonConfigsFrom(document).foo, 'third');
      });
    });
  });
});
