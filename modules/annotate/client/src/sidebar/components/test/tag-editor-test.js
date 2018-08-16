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

describe('tagEditor', function () {
  var fakeTags;

  before(function () {
    angular.module('app',[])
      .component('tagEditor', require('../tag-editor'));
  });

  beforeEach(function () {
    fakeTags = {
      filter: sinon.stub(),
      store: sinon.stub(),
    };

    angular.mock.module('app', {
      tags: fakeTags,
    });
  });

  it('converts tags to the form expected by ng-tags-input', function () {
    var element = util.createDirective(document, 'tag-editor', {
      tags: ['foo', 'bar'],
    });
    assert.deepEqual(element.ctrl.tagList, [{text: 'foo'}, {text: 'bar'}]);
  });

  describe('when tags are changed', function () {
    var element;
    var onEditTags;

    beforeEach(function () {
      onEditTags = sinon.stub();
      element = util.createDirective(document, 'tag-editor', {
        onEditTags: {args: ['tags'], callback: onEditTags},
        tags: ['foo'],
      });
      element.ctrl.onTagsChanged();
    });

    it('calls onEditTags handler', function () {
      assert.calledWith(onEditTags, sinon.match(['foo']));
    });

    it('saves tags to the store', function () {
      assert.calledWith(fakeTags.store, sinon.match([{text: 'foo'}]));
    });
  });

  describe('#autocomplete', function () {
    it('suggests tags using the `tags` service', function () {
      var element = util.createDirective(document, 'tag-editor', {tags: []});
      element.ctrl.autocomplete('query');
      assert.calledWith(fakeTags.filter, 'query');
    });
  });
});
