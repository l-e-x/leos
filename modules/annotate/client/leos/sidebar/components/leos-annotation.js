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

var diff_match_patch = require('diff-match-patch');
import 'diff-match-patch-line-and-word';

var annotation = require('../../../src/sidebar/components/annotation');
var serviceConfig = require('../../../src/sidebar/service-config');
var SYSTEMIDS = require('../../../leos/shared/systemId');
var OPERATION_MODES = require('../../../leos/shared/operationMode');

// @ngInject
function LeosAnnotationController(
  $document, $element, $injector, $rootScope, $scope, $timeout, $window, analytics, store,
  annotationMapper, drafts, flash, groups, permissions, serviceUrl,
  session, settings, api, streamer) {

  $injector.invoke(annotation.controller, this, {$document: $document, $element: $element, $rootScope: $rootScope, $scope: $scope, $timeout: $timeout, 
      $window: $window, analytics: analytics, store: store, annotationMapper: annotationMapper, drafts: drafts, flash: flash
      , groups: groups, permissions: permissions, serviceUrl: serviceUrl, session: session, settings: settings, api: api, streamer: streamer});

  var parentAuthorize = this.authorize;
  //var parentSave = this.save;

  const docMetadata = (this.annotation.document != undefined) ? Object.keys(this.annotation.document.metadata)
    .filter(key => Object.keys(settings.displayMetadataCondition).indexOf(key) != -1)
    .reduce((obj, key) => {
      obj[settings.displayMetadataCondition[key]] = this.annotation.document.metadata[key];
      return obj;
    }, {}) : {};

  this.authorize = function (action) {
    var responseStatus = this.annotation.document.metadata.responseStatus || null;
    var annotationGroup = this.annotation.document.metadata.responseId || null;
    var annotationStatus = this.annotation.status.status || null;

    if (action === "delete" || action === "update") {
      if (settings.operationMode === OPERATION_MODES.READ_ONLY) {
        return false;
      }
      var svc = serviceConfig(settings);
      if (responseStatus === 'SENT' && svc && svc.authority && svc.authority === SYSTEMIDS.ISC) {
        if(annotationGroup !== settings.connectedEntity || (annotationStatus && annotationStatus !== 'NORMAL')) {
          //Annotation not from same group as connected unit, so no edit nor delete are allowed
          return false;
        } else if(action === 'update' || action === 'delete') {
          return true;
        }
      }
    }
    if (action === 'merge_suggestion') {
      return permissions.getUserPermissions().indexOf('CAN_MERGE_SUGGESTION') !== -1;
    }
    if (action === 'delete') {
      if (permissions.getUserPermissions().indexOf('CAN_DELETE') !== -1) {
        return true;
      } else {
        return parentAuthorize(action);
      }
    } else {
      return parentAuthorize(action);
    }
  };

  this.annotation.selected = false;

  this.isSelected = function() {
    return this.annotation.selected;
  };

  this.canReply = function() {
    //LEOS 3839 : disabling reply on ISC context
    var svc = serviceConfig(settings);
    if (svc && svc.authority && svc.authority === SYSTEMIDS.ISC || settings.operationMode === OPERATION_MODES.READ_ONLY) {
      return false;
    } else {
      return true;
    }
  };

  this.showButtons = function($event) {
    this.annotation.selected = true;
  };

  this.hideButtons = function($event) {
    this.annotation.selected = false;
  };

  this.isSuggestion = function() {
    return (this.state().tags && this.state().tags.includes('suggestion'));
  };

  this.updateSelectedGroup = function(group) {
    this.annotation.group = group.id;
  };

  this.getMetadata = function() {
    return docMetadata;
  };

  this.shouldDisplayMetadata = function() {
    return (Object.keys(docMetadata).length >= 0);
  };

  this.getMetadataInfoStyle = function(keytoFind) {
    var index = Object.keys(docMetadata).indexOf(keytoFind);
    return `leos-metadata-info-${index}`
  };

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
    };
}

module.exports = {
  controller: LeosAnnotationController,
  controllerAs: 'vm',
  bindings: annotation.bindings,
  template: require('../templates/leos-annotation.html'),

  // Private helper exposed for use in unit tests.
  updateModel: annotation.updateModel
};
