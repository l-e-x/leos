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

var sidebarContent = require('../../../src/sidebar/components/sidebar-content');
var SearchClient = require('../../../src/sidebar/search-client');

/**
 * Returns the group ID of the first annotation in `results` whose
 * ID is a key in `selection`.
 */
function groupIDFromSelection(selection, results) {
  var id = firstKey(selection);
  var annot = results.find(function (annot) {
    return annot.id === id;
  });
  if (!annot) {
    return null;
  }
  return annot.group;
}

// @ngInject
function LeosSidebarContentController(
  $injector, $scope, analytics, store, annotationMapper, drafts, features, frameSync,
  groups, rootThread, settings, streamer, streamFilter, api
) {
  $injector.invoke(sidebarContent.controller, this, {$scope: $scope, analytics: analytics, store: store, annotationMapper: annotationMapper
    , drafts: drafts, features: features, frameSync: frameSync, groups: groups, rootThread: rootThread, settings: settings, streamer: streamer,
    streamFilter: streamFilter, api: api});

  var searchClients = [];

  function _resetAnnotations() {
    annotationMapper.unloadAnnotations(store.savedAnnotations());
  }

  function _loadAnnotationsFor(uris, group) {
    var searchClient = new SearchClient(api.search, {
      // If no group is specified, we are fetching annotations from
      // all groups in order to find out which group contains the selected
      // annotation, therefore we need to load all chunks before processing
      // the results
      incremental: !!group,
    });
    searchClients.push(searchClient);
    searchClient.on('results', function (results) {
      if (store.hasSelectedAnnotations()) {
        // Focus the group containing the selected annotation and filter
        // annotations to those from this group
        var groupID = groupIDFromSelection(
          store.getState().selectedAnnotationMap, results);
        if (!groupID) {
          // If the selected annotation is not available, fall back to
          // loading annotations for the currently focused group
          groupID = groups.focused().id;
        }
        results = results.filter(function (result) {
          return result.group === groupID;
        });
        groups.focus(groupID);
      }

      if (results.length) {
        annotationMapper.loadAnnotations(results);
      }
    });
    searchClient.on('end', function () {
      // Remove client from list of active search clients.
      //
      // $evalAsync is required here because search results are emitted
      // asynchronously. A better solution would be that the loading state is
      // tracked as part of the app state.
      $scope.$evalAsync(function () {
        searchClients.splice(searchClients.indexOf(searchClient), 1);
      });

      store.frames().forEach(function (frame) {
        if (0 <= uris.indexOf(frame.uri)) {
          store.updateFrameAnnotationFetchStatus(frame.uri, true);
        }
      });
    });
    searchClient.get({uri: uris, group: group});
  }

  /**
   * Load annotations for all URLs associated with `frames`.
   */
  function loadAnnotations() {
    _resetAnnotations();

    searchClients.forEach(function (client) {
      client.cancel();
    });

    // If there is no selection, load annotations only for the focused group.
    //
    // If there is a selection, we load annotations for all groups, find out
    // which group the first selected annotation is in and then filter the
    // results on the client by that group.
    //
    // In the common case where the total number of annotations on
    // a page that are visible to the user is not greater than
    // the batch size, this saves an extra roundtrip to the server
    // to fetch the selected annotation in order to determine which group
    // it is in before fetching the remaining annotations.
    var group = store.hasSelectedAnnotations() ?
      null : groups.focused().id;

    var searchUris = store.searchUris();
    if (searchUris.length > 0) {
      _loadAnnotationsFor(searchUris, group);

      streamFilter.resetFilter().addClause('/uri', 'one_of', searchUris);
      streamer.setConfig('filter', {filter: streamFilter.getFilter()});
    }
  }

  $scope.$on('reloadAnnotations', function () {
    store.clearSelectedAnnotations();
    loadAnnotations();
  });
}

module.exports = {
  controller: LeosSidebarContentController,
  controllerAs: 'vm',
  bindings: {
    auth: '<',
    search: '<',
  },
  template: require('../../../src/sidebar/templates/sidebar-content.html'),
};