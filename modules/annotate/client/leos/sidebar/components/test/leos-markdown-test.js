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
var proxyquire = require('proxyquire');

var util = require('../../../../src/sidebar/directive/test/util');
var noCallThru = require('../../../../src/shared/test/util').noCallThru;

describe('leos-markdown', function () {
  function isHidden(element) {
    return element.classList.contains('ng-hide');
  }

  function inputElement(editor) {
    return editor[0].querySelector('.form-input');
  }

  function viewElement(editor) {
    return editor[0].querySelector('.markdown-body');
  }

  function toolbarButtons(editor) {
    return Array.from(editor[0].querySelectorAll('.markdown-tools-button'));
  }

  function getRenderedHTML(editor) {
    var contentElement = viewElement(editor);
    if (isHidden(contentElement)) {
      return 'rendered markdown is hidden';
    }
    return contentElement.innerHTML;
  }

  function mockFormattingCommand() {
    return {
      text: 'formatted text',
      selectionStart: 0,
      selectionEnd: 0,
    };
  }

  before(function () {
    angular.module('app', ['ngSanitize'])
      .component('leosMarkdown', proxyquire('../leos-markdown', noCallThru({
        angular: angular,
        katex: {
          renderToString: function (input) {
            return 'math:' + input.replace(/$$/g, '');
          },
        },
        'lodash.debounce': function (fn) {
          // Make input change debouncing synchronous in tests
          return function () {
            fn();
          };
        },
        '../../../../src/sidebar/components/render-markdown': noCallThru(function (markdown, $sanitize) {
          return $sanitize('rendered:' + markdown);
        }),
        '../../../../src/sidebar/components/markdown-commands': {
          convertSelectionToLink: mockFormattingCommand,
          toggleBlockStyle: mockFormattingCommand,
          toggleSpanStyle: mockFormattingCommand,
          LinkType: require('../../../../src/sidebar/markdown-commands').LinkType,
        },
        '../../../../src/sidebar/components/media-embedder': noCallThru({
          replaceLinksWithEmbeds: function (element) {
            // Tag the element as having been processed
            element.dataset.replacedLinksWithEmbeds = 'yes';
          },
        }),
      })));
  });

  beforeEach(function () {
    angular.mock.module('app');
  });

  describe('toolbar display', function () {
      it('should not show toolbar', function () {
        var editor = util.createDirective(document, 'leosMarkdown', {
          readOnly: false,
          text: 'Hello World',
        });
        assert.notEqual(viewElement(editor), null);
        assert.deepEqual(toolbarButtons(editor), []);
      });
    });
});