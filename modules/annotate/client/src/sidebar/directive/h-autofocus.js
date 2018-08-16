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

/** An attribute directive that focuses an <input> when it's linked by Angular.
 *
 * The HTML5 autofocus attribute automatically puts the keyboard focus in an
 * <input> on page load. But this doesn't work for <input>s that are
 * rendered by JavaScript/Angular after page load, for example an <input> that
 * is shown/hidden by JavaScript when an ng-if condition becomes true.
 *
 * To automatically put the keyboard focus on such an input when it's linked by
 * Angular, attach this directive to it as an attribute:
 *
 *   <input ng-if="..." h-autofocus>
 *
*/
module.exports = function() {
  return {
    restrict: 'A',
    link: function($scope, $element) {
      $element[0].focus();
    },
  };
};
