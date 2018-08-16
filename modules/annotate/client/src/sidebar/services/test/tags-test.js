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

var TAGS_LIST_KEY = 'hypothesis.user.tags.list';
var TAGS_MAP_KEY = 'hypothesis.user.tags.map';

class FakeStorage {
  constructor() {
    this._storage = {};
  }

  getObject(key) {
    return this._storage[key];
  }

  setObject(key, value) {
    this._storage[key] = value;
  }
}

describe('sidebar.tags', () => {
  var fakeLocalStorage;
  var tags;

  before(() => {
    angular.module('h', [])
           .service('tags', require('../tags'));
  });

  beforeEach(() => {
    fakeLocalStorage = new FakeStorage();

    var stamp = Date.now();
    var savedTagsMap = {
      foo: {
        text: 'foo',
        count: 1,
        updated: stamp,
      },
      bar: {
        text: 'bar',
        count: 5,
        updated: stamp,
      },
      future: {
        text: 'future',
        count: 2,
        updated: stamp,
      },
      argon: {
        text: 'argon',
        count: 1,
        updated: stamp,
      },
    };
    var savedTagsList = Object.keys(savedTagsMap);

    fakeLocalStorage.setObject(TAGS_MAP_KEY, savedTagsMap);
    fakeLocalStorage.setObject(TAGS_LIST_KEY, savedTagsList);

    angular.mock.module('h', {
      localStorage: fakeLocalStorage,
    });
    angular.mock.inject((_tags_) => {
      tags = _tags_;
    });
  });

  describe('#filter', () => {
    it('returns tags having the query as a substring', () => {
      assert.deepEqual(tags.filter('a'), ['bar', 'argon']);
    });

    it('is case insensitive', () => {
      assert.deepEqual(tags.filter('Ar'), ['bar', 'argon']);
    });
  });

  describe('#store', () => {
    it('saves new tags to storage', () => {
      tags.store([{text: 'new'}]);

      var storedTagsList = fakeLocalStorage.getObject(TAGS_LIST_KEY);
      assert.include(storedTagsList, 'new');

      var storedTagsMap = fakeLocalStorage.getObject(TAGS_MAP_KEY);
      assert.match(storedTagsMap.new, sinon.match({
        count: 1,
        text: 'new',
        updated: sinon.match.number,
      }));
    });

    it('increases the count for a tag already stored', () => {
      tags.store([{text: 'bar'}]);
      var storedTagsMap = fakeLocalStorage.getObject(TAGS_MAP_KEY);
      assert.equal(storedTagsMap.bar.count, 6);
    });

    it('orders list by count descending, lexical ascending', () => {
      for (var i = 0; i < 6; i++) {
        tags.store([{text: 'foo'}]);
      }

      var storedTagsList = fakeLocalStorage.getObject(TAGS_LIST_KEY);
      assert.deepEqual(storedTagsList, ['foo', 'bar', 'future', 'argon']);
    });
  });
});
