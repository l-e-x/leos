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

// @ngInject
function SearchInputController($element, $http, $scope) {
  var self = this;
  var button = $element.find('button');
  var input = $element.find('input')[0];
  var form = $element.find('form')[0];

  button.on('click', function () {
    input.focus();
  });

  $scope.$watch(
    function () { return $http.pendingRequests.length; },
    function (count) { self.loading = count > 0; }
  );

  form.onsubmit = function (e) {
    e.preventDefault();
    self.onSearch({$query: input.value});
  };

  this.inputClasses = function () {
    return {'is-expanded': self.alwaysExpanded || self.query};
  };

  this.$onChanges = function (changes) {
    if (changes.query) {
      input.value = changes.query.currentValue;
    }
  };
}

module.exports = {
  controller: SearchInputController,
  controllerAs: 'vm',
  bindings: {
    // Specifies whether the search input field should always be expanded,
    // regardless of whether the it is focused or has an active query.
    //
    // If false, it is only expanded when focused or when 'query' is non-empty
    alwaysExpanded: '<',
    query: '<',
    onSearch: '&',
  },
  template: require('../templates/search-input.html'),
};
