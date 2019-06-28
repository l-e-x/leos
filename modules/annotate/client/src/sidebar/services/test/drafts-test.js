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

var draftsService = require('../drafts');

var fixtures = {
  draftWithText: {
    isPrivate: false,
    text: 'some text',
    tags: [],
  },
  draftWithTags: {
    isPrivate: false,
    text: '',
    tags: ['atag'],
  },
  emptyDraft: {
    isPrivate: false,
    text: '',
    tags: [],
  },
};

describe('drafts', function () {
  var drafts;

  beforeEach(function () {
    drafts = draftsService();
  });

  describe('#getIfNotEmpty', function () {
    it('returns the draft if it has tags', function () {
      var model = {id: 'foo'};
      drafts.update(model, fixtures.draftWithTags);
      assert.deepEqual(drafts.getIfNotEmpty(model), fixtures.draftWithTags);
    });

    it('returns the draft if it has text', function () {
      var model = {id: 'foo'};
      drafts.update(model, fixtures.draftWithText);
      assert.deepEqual(drafts.getIfNotEmpty(model), fixtures.draftWithText);
    });

    it('returns null if the text and tags are empty', function () {
      var model = {id: 'foo'};
      drafts.update(model, fixtures.emptyDraft);
      assert.isNull(drafts.getIfNotEmpty(model));
    });
  });

  describe('#update', function () {
    it('should save changes', function () {
      var model = {id: 'foo'};
      assert.notOk(drafts.get(model));
      drafts.update(model, {isPrivate:true, tags:['foo'], text:'edit'});
      assert.deepEqual(
        drafts.get(model),
        {isPrivate: true, tags: ['foo'], text: 'edit'});
    });

    it('should replace existing drafts', function () {
      var model = {id: 'foo'};
      drafts.update(model, {isPrivate:true, tags:['foo'], text:'foo'});
      drafts.update(model, {isPrivate:true, tags:['foo'], text:'bar'});
      assert.equal(drafts.get(model).text, 'bar');
    });

    it('should replace existing drafts with the same ID', function () {
      var modelA = {id: 'foo'};
      var modelB = {id: 'foo'};
      drafts.update(modelA, {isPrivate:true, tags:['foo'], text:'foo'});
      drafts.update(modelB, {isPrivate:true, tags:['foo'], text:'bar'});
      assert.equal(drafts.get(modelA).text, 'bar');
    });

    it('should replace drafts with the same tag', function () {
      var modelA = {$tag: 'foo'};
      var modelB = {$tag: 'foo'};
      drafts.update(modelA, {isPrivate:true, tags:['foo'], text:'foo'});
      drafts.update(modelB, {isPrivate:true, tags:['foo'], text:'bar'});
      assert.equal(drafts.get(modelA).text, 'bar');
    });
  });

  describe('#remove', function () {
    it('should remove drafts', function () {
      var model = {id: 'foo'};
      drafts.update(model, {text: 'bar'});
      drafts.remove(model);
      assert.notOk(drafts.get(model));
    });
  });

  describe('#unsaved', function () {
    it('should return drafts for unsaved annotations', function () {
      var model = {$tag: 'local-tag', id: undefined};
      drafts.update(model, {text: 'bar'});
      assert.deepEqual(drafts.unsaved(), [model]);
    });

    it('should not return drafts for saved annotations', function () {
      var model = {id: 'foo'};
      drafts.update(model, {text: 'baz'});
      assert.deepEqual(drafts.unsaved(), []);
    });
  });
});
