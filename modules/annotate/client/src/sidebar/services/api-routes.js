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

var { retryPromiseOperation } = require('../util/retry');

/**
 * A service which fetches and caches API route metadata.
 */
// @ngInject
function apiRoutes($http, settings) {
  // Cache of route name => route metadata from API root.
  var routeCache;
  // Cache of links to pages on the service fetched from the API's "links"
  // endpoint.
  var linkCache;

  function getJSON(url) {
    return $http.get(url).then(({ status, data }) => {
      if (status !== 200) {
        throw new Error(`Fetching ${url} failed`);
      }
      return data;
    });
  }

  /**
   * Fetch and cache API route metadata.
   *
   * Routes are fetched without any authentication and therefore assumed to be
   * the same regardless of whether the user is authenticated or not.
   *
   * @return {Promise<Object>} - Map of routes to route metadata.
   */
  function routes() {
    if (!routeCache) {
      routeCache = retryPromiseOperation(() => getJSON(settings.apiUrl))
        .then((index) => index.links);
    }
    return routeCache;
  }

  /**
   * Fetch and cache service page links from the API.
   *
   * @return {Promise<Object>} - Map of link name to URL
   */
  function links() {
    if (!linkCache) {
      linkCache = routes().then(routes => {
        return getJSON(routes.links.url);
      });
    }
    return linkCache;
  }

  return { routes, links };
}

module.exports = apiRoutes;
