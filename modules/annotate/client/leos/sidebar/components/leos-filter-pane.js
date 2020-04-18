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

var events = require('../../../src/sidebar/events');

// @ngInject
function LeosFilterPaneController(session, settings, $scope, $rootScope, store, groups, bridge) {
  var self = this;
  var GROUP_TYPE = "Group=";
  var AUTHORS_TYPE = "Authors=";
  var TYPE_TYPE = "Type=";
  var STATUS_TYPE = "Status=";
  var CUSTOM_TEXT_TYPE = "Custom=";
  var FILTER_SEPARATOR = ', ';
  var GROUP_FILTER_PREFIX = 'group:';
  var AUTHOR_FILTER_PREFIX = 'user:';
  var TYPE_FILTER_PREFIX = 'tag:';
  var STATUS_FILTER_PREFIX = 'status:';

  this.isThemeClean = settings.theme === 'clean';
  this.leosFilterPaneVisible = false;

  $scope.$on('filterPane:toggleVisibility', function () {
    $scope.$apply(function () {
        $scope.leosFilterPaneVisible = !$scope.leosFilterPaneVisible;
    });

    if($scope.leosFilterPaneVisible) {
      //init groups filter
      if($scope.groupsList === undefined) {
        _loadGroupsFilter();
      }
      //init authors filter
      if($scope.authorsList === undefined) {
        _loadAuthorsFilter();
      }
    }
    _resetFilters();
  });

  $scope.$on(events.ANNOTATION_CREATED, function () {
    //No need to clear selected filters as this at most adds new authors or groups. Just need to reload authors and groups based on annotations
    _loadAuthorsFilter();
    _normalizeAnnotationsGroups();
    _loadGroupsFilter();
  });

  $scope.$on(events.LEOS_CLEAR_SELECTION, function () {
    _resetFilters();
  });

  /**
   * Reload authors list, if one of the "deleted" authors was present on selectedFilter, remove him
   */
  $scope.$on(events.ANNOTATION_DELETED, function () {
    //preserve the previous authors list before reloading
    var oldAuthors = $scope.authorsList === undefined ? [] : $scope.authorsList;
    //get the refreshed authors and groups lists
    _loadAuthorsFilter();
    _loadGroupsFilter();
    //get difference between both will give all authors that no longer have annotations present (should be removed from authors filter)
    var diff = oldAuthors.filter(function (value) { return !$scope.authorsList.includes(value) });
    diff.forEach(function (value, index) {
      //remove author from summary list
      _removeSelectedFilterValue(AUTHORS_TYPE, value);
      //remove author from selected authors list
      $scope.selectedAuthors = $scope.selectedAuthors.filter(function (value) { return $scope.authorsList.includes(value) });
    });
    _doSearch();
  });

  $scope.onTypeSelect = function(type) {
    var filterTypeIndex = _filterTypeSelected(type);
    _removeSelectedFilterType(TYPE_TYPE);
    _addSelectedFilter(TYPE_TYPE, type, filterTypeIndex);
    _doSearch();
  };

  $scope.onStatusSelect = function(status) {
    if (settings.showStatusFilter) {
      var filterStatusIndex = _filterTypeSelected(status);
      _removeSelectedFilterType(STATUS_TYPE);
      _addSelectedFilter(STATUS_TYPE, status, filterStatusIndex);
    }
    _doSearch();
  };

  $scope.showStatusFilter = function() {
    return settings.showStatusFilter;
  };

  $scope.afterSelectGroup = function(item) {
    _addSelectedFilter(GROUP_TYPE, item.name);
    _doSearch();
  };

  $scope.afterSelectAuthor = function(item) {
    _addSelectedFilter(AUTHORS_TYPE, item.name);
    _doSearch();
  };

  $scope.afterRemoveGroup = function(item){
    _removeSelectedFilterValue(GROUP_TYPE, item.name);
    _doSearch();
  };

  $scope.afterRemoveAuthor = function(item){
    _removeSelectedFilterValue(AUTHORS_TYPE, item.name);
    _doSearch();
  };

  $scope.onCustomTextInput = function(customText) {
    _removeSelectedFilterValue(CUSTOM_TEXT_TYPE);
    if(customText !== '') {
      _addSelectedFilter(CUSTOM_TEXT_TYPE, customText);
    }
    _doSearch();
  };

  /******************
   * PRIVATE METHODS
   ******************/
  var _buildAuthorShowLabel = function(annotationUserInfo) {
    return annotationUserInfo.display_name + ' (' + annotationUserInfo.entity_name + ')';
  };

  var _buildGroupId = function(group) {
    return GROUP_FILTER_PREFIX + group;
  };

  var _buildGroupName = function(group) {
    return group.replace($rootScope.ANNOTATION_GROUP_SPACE_REPLACE_TOKEN," ");
  };

  var _filterTypeSelected = function(type) {
    return $scope.selectedFilters.findIndex(element => element.includes(type));
  };

  var _addSelectedFilter = function(type, item, addAtIndex) {
    var filterTypeIndex = _filterTypeSelected(type);
    if(filterTypeIndex > -1) {
      $scope.selectedFilters[filterTypeIndex] += FILTER_SEPARATOR + item;
    } else {
      $scope.selectedFilters.splice(addAtIndex, 0, type + item);
    }
  };

  var _removeSelectedFilterValue = function (type, item) {
    var filterTypeIndex = _filterTypeSelected(type);
    if (filterTypeIndex > -1) {
      var filter = $scope.selectedFilters[filterTypeIndex];
      if(filter.indexOf(FILTER_SEPARATOR) > -1 && filter.indexOf(FILTER_SEPARATOR) < filter.indexOf(item)) { //item is NOT THE FIRST among the same items of its type
        $scope.selectedFilters[filterTypeIndex] = filter.replace(FILTER_SEPARATOR + item, '');
      } else if(filter.indexOf(FILTER_SEPARATOR, filter.indexOf(item)) > -1) { //item is NOT THE LAST among the same items of its type
        $scope.selectedFilters[filterTypeIndex] = filter.replace(item + FILTER_SEPARATOR, '');
      } else {
        $scope.selectedFilters.splice(filterTypeIndex, 1);
      }
    }
  };

  var _removeSelectedFilterType = function(type) {
    var filterTypeIndex = _filterTypeSelected(type);
    if(filterTypeIndex > -1) {
      $scope.selectedFilters.splice(filterTypeIndex, 1);
    }
  };

  var _normalizeAnnotationsGroups = function() {
    store.getState().annotations.forEach(function (item) {
      item.group = item.group.replace(" ",$rootScope.ANNOTATION_GROUP_SPACE_REPLACE_TOKEN);
    });
  };

  var _loadGroupsFilter = function() {
    $scope.groupsList = [];
    //take only the name of the group to build the group filter
    store.getState().annotations.forEach(function (item) {
      //Skip Collaborators group as selecting it is the same as not having any groups filtered
      if(item.group === groups.defaultGroupId()){
        return;
      }
      var groupId = _buildGroupId(item.group);
      var groupName = _buildGroupName(item.group);
      var groupObj = {id:groupId, name:groupName};
      if($scope.groupsList.filter(function (group) { return group.id === groupObj.id }).length === 0){
        $scope.groupsList.push(groupObj);
      }
    });
  };

  var _loadAuthorsFilter = function() {
    $scope.authorsList = [];
    store.getState().annotations.forEach(function (item) {
      var authorLabel = _buildAuthorShowLabel(item.user_info);
      var authorObj = {id:AUTHOR_FILTER_PREFIX + item.user, name:authorLabel};
      if($scope.authorsList.filter(function (author) { return author.id === authorObj.id }).length === 0){
        $scope.authorsList.push(authorObj);
      }
    });
  };

  var _resetFilters = function() {
    $scope.selectedFilters = [];
    $scope.selectedGroups = [];
    $scope.selectedAuthors = [];
    $scope.customText = '';
    $scope.type = 'All';
    $scope.status = settings.showStatusFilter ? 'All' : 'Non-Processed';
    _addSelectedFilter(TYPE_TYPE, $scope.type, -1);
    if (settings.showStatusFilter) {
      _addSelectedFilter(STATUS_TYPE, $scope.status, -1);
    }
    bridge.call('LEOS_refreshAnnotationLinkLines');
  };

  var _doSearch = function() {
    var searchQuery = '';
    //handle By Type
    if($scope.type === 'Comments') {
      searchQuery += " " + TYPE_FILTER_PREFIX + "comment";
    } else if($scope.type === 'Suggestions') {
      searchQuery += " " + TYPE_FILTER_PREFIX + "suggestion";
    }
    if (settings.showStatusFilter) {
      if($scope.status === 'Processed') {
        searchQuery += " " + STATUS_FILTER_PREFIX + "DELETED " + STATUS_FILTER_PREFIX + "REJECTED " + STATUS_FILTER_PREFIX + "ACCEPTED";
      } else if($scope.status === 'Non-Processed') {
        searchQuery += " " + STATUS_FILTER_PREFIX + "NORMAL";
      }
    }
    //handle By Group
    $scope.selectedGroups.forEach(function (group) { searchQuery += " " + group.id });
    //handle By Author
    $scope.selectedAuthors.forEach(function (author) { searchQuery += " " + author.id });
    //handle Custom
    if($scope.customText !== undefined && $scope.customText !== "") {
      searchQuery += " " + $scope.customText;
    }
    //do search
    self.searchController.update(searchQuery);
    bridge.call('LEOS_refreshAnnotationLinkLines');
  }
}

/**
 * @name leosFilterPane
 * @description Displays a filter pane in the sidebar.
 */
// @ngInject
module.exports = {
  controller: LeosFilterPaneController,
  controllerAs: 'vm',
  bindings: {
    searchController: '<'
  },
  template: require('../templates/leos-filter-pane.html')
};
