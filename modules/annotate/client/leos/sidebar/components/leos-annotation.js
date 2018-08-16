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

var diff_match_patch = require('diff-match-patch');
import 'diff-match-patch-line-and-word';

var annotation = require('../../../src/sidebar/components/annotation');

// @ngInject
function LeosAnnotationController(
  $document, $element, $injector, $rootScope, $scope, $timeout, $window, analytics, store,
  annotationMapper, drafts, flash, groups, permissions, serviceUrl,
  session, settings, api, streamer) {

  $injector.invoke(annotation.controller, this, {$document: $document, $element: $element, $rootScope: $rootScope, $scope: $scope, $timeout: $timeout, 
      $window: $window, analytics: analytics, store: store, annotationMapper: annotationMapper, drafts: drafts, flash: flash
      , groups: groups, permissions: permissions, serviceUrl: serviceUrl, session: session, settings: settings, api: api, streamer: streamer});

  this.annotation.selected = false;

  this.isSelected = function() {
    return this.annotation.selected;
  }
  
  this.showButtons = function($event) {
    this.annotation.selected = true;
  }

  this.hideButtons = function($event) {
    this.annotation.selected = false;
  }
    
  let parentAuthorize = this.authorize;
  this.authorize = function(action) {
    return parentAuthorize(action);
  }
    
  this.isSuggestion = function() {
    return (this.state().tags && this.state().tags.includes('suggestion'));
  }

  this.diffText = function() {
      var htmlDiff = this.state().text;
      if (this.editing() || !this.hasContent()) {
        return htmlDiff;
      }
      var origText = this.quote();
      if (this.isSuggestion() && origText) {
        var dmp = new diff_match_patch();
        var textDiff = dmp.diff_wordMode(origText, this.state().text);
        htmlDiff='<span class="leos-content-modified">';
        for (let d of textDiff) {
          if (d[0] === -1) {
            htmlDiff+=`<span class="leos-content-removed">${d[1]}</span>`;
          }
          else if (d[0] === 0) {
            htmlDiff+=d[1];
          }
          else if (d[0] === 1) {
            htmlDiff+=`<span class="leos-content-new">${d[1]}</span>`;
          }
        }
        htmlDiff+='</span>';
      }
      return htmlDiff;
    }
}

module.exports = {
  controller: LeosAnnotationController,
  controllerAs: 'vm',
  bindings: annotation.bindings,
  template: require('../templates/leos-annotation.html'),

  // Private helper exposed for use in unit tests.
  updateModel: annotation.updateModel,
};
