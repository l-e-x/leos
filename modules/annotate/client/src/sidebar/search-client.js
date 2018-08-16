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

var EventEmitter = require('tiny-emitter');

/**
 * Client for the Hypothesis search API.
 *
 * SearchClient handles paging through results, canceling search etc.
 */
class SearchClient extends EventEmitter {
  /**
   * @param {Object} searchFn - Function for querying the search API
   * @param {Object} opts - Search options
   */
  constructor(searchFn, opts) {
    super();
    opts = opts || {};

    var DEFAULT_CHUNK_SIZE = 200;
    this._searchFn = searchFn;
    this._chunkSize = opts.chunkSize || DEFAULT_CHUNK_SIZE;
    if (typeof opts.incremental !== 'undefined') {
      this._incremental = opts.incremental;
    } else {
      this._incremental = true;
    }
    this._canceled = false;
  }

  _getBatch(query, offset) {
    var searchQuery = Object.assign({
      limit: this._chunkSize,
      offset: offset,
      sort: 'created',
      order: 'asc',
      _separate_replies: true,
    }, query);

    var self = this;
    this._searchFn(searchQuery).then(function (results) {
      if (self._canceled) {
        return;
      }

      var chunk = results.rows.concat(results.replies || []);
      if (self._incremental) {
        self.emit('results', chunk);
      } else {
        self._results = self._results.concat(chunk);
      }

      // Check if there are additional pages of results to fetch. In addition to
      // checking the `total` figure from the server, we also require that at
      // least one result was returned in the current page, otherwise we would
      // end up repeating the same query for the next page. If the server's
      // `total` count is incorrect for any reason, that will lead to the client
      // polling the server indefinitely.
      var nextOffset = offset + results.rows.length;
      if (results.total > nextOffset && chunk.length > 0) {
        self._getBatch(query, nextOffset);
      } else {
        if (!self._incremental) {
          self.emit('results', self._results);
        }
        self.emit('end');
      }
    }).catch(function (err) {
      if (self._canceled) {
        return;
      }
      self.emit('error', err);
    }).then(function () {
      if (self._canceled) {
        return;
      }
      self.emit('end');
    });
  }

  /**
   * Perform a search against the Hypothesis API.
   *
   * Emits a 'results' event with an array of annotations as they become
   * available (in incremental mode) or when all annotations are available
   * (in non-incremental mode).
   *
   * Emits an 'error' event if the search fails.
   * Emits an 'end' event once the search completes.
   */
  get(query) {
    this._results = [];
    this._getBatch(query, 0);
  }

  /**
   * Cancel the current search and emit the 'end' event.
   * No further events will be emitted after this.
   */
  cancel() {
    this._canceled = true;
    this.emit('end');
  }
}

module.exports = SearchClient;
