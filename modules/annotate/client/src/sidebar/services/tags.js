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

/**
 * @typedef Tag
 * @property {string} text - The label of the tag
 * @property {number} count - The number of times this tag has been used.
 * @property {number} updated - The timestamp when this tag was last used.
 */

/**
 * Service for fetching tag suggestions and storing data to generate them.
 *
 * The `tags` service stores metadata about recently used tags to local storage
 * and provides a `filter` method to fetch tags matching a query, ranked based
 * on frequency of usage.
 */
// @ngInject
function tags(localStorage) {
  var TAGS_LIST_KEY = 'hypothesis.user.tags.list';
  var TAGS_MAP_KEY = 'hypothesis.user.tags.map';

  /**
   * Return a list of tag suggestions matching `query`.
   *
   * @param {string} query
   * @return {Tag[]} List of matching tags
   */
  function filter(query) {
    var savedTags = localStorage.getObject(TAGS_LIST_KEY) || [];

    return savedTags.filter((e) => {
      return e.toLowerCase().indexOf(query.toLowerCase()) !== -1;
    });
  }

  /**
   * Update the list of stored tag suggestions based on the tags that a user has
   * entered for a given annotation.
   *
   * @param {Tag} tags - List of tags.
   */
  function store(tags) {
    // Update the stored (tag, frequency) map.
    var savedTags = localStorage.getObject(TAGS_MAP_KEY) || {};
    tags.forEach((tag) => {
      if (savedTags[tag.text]) {
        savedTags[tag.text].count += 1;
        savedTags[tag.text].updated = Date.now();
      } else {
        savedTags[tag.text] = {
          text: tag.text,
          count: 1,
          updated: Date.now(),
        };
      }
    });
    localStorage.setObject(TAGS_MAP_KEY, savedTags);

    // Sort tag suggestions by frequency.
    var tagsList = Object.keys(savedTags).sort((t1, t2) => {
      if (savedTags[t1].count !== savedTags[t2].count) {
        return savedTags[t2].count - savedTags[t1].count;
      }
      return t1.localeCompare(t2);
    });
    localStorage.setObject(TAGS_LIST_KEY, tagsList);
  }

  return {
    filter,
    store,
  };
}

module.exports = tags;
