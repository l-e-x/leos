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

let events = require('../../../src/sidebar/events');
let OPERATION_MODES = require('../../../leos/shared/operationMode');

//@ngInject
function LeosSuggestionController(
          $rootScope, $scope, $timeout, $window, analytics, bridge, flash, permissions, api, store) {
  let self = this;
  self.isAccepting = false;

  init();

  function getAnnotationAnchor() {
      return new Promise( function(resolve, reject) {
        let timeout = setTimeout((() => reject('timeout')), 500);
        bridge.call('requestAnnotationAnchor', self.annotation, function (error, [result]) {
          clearTimeout(timeout);
          self.annotation.anchorInfo = result;
          if (error) { return reject(error); } else { return resolve(result); }
        });
      });
  }

  function sendMergeSuggestionRequest() {
    if (!self.annotation || !self.annotation.anchorInfo) {
      return Promise.reject("Suggestion not valid");
    }
    let anchor = self.annotation.anchorInfo;
    anchor.newText = self.annotation.text;

    return new Promise( function(resolve, reject) {
      let timeout = setTimeout((() => reject('timeout')), 7000);
      bridge.call('requestMergeSuggestion', anchor, function (error, [result]) {
        clearTimeout(timeout);
        if (result && result.result && result.result !== 'SUCCESS') {
          error = result;
        }
        if (error) { return reject(error); } else { return resolve(result); }
      });
    });
  }

  function processSuggestionMerging() {
    return getAnnotationAnchor().then(result => {
      return sendMergeSuggestionRequest();
    }).catch(err => {
      return Promise.reject(err);
    });
  }

  /**
   * Initialize the controller instance.
   *
   * All initialization code except for assigning the controller instance's
   * methods goes here.
   */
  function init() {
    self.authorize = function(action) {
      return action === 'merge_suggestion'
          && permissions.getUserPermissions().indexOf('CAN_MERGE_SUGGESTION') !== -1;
    };

    self.isAcceptDisabled = function() {
      return store.hostState === 'OPEN'
          || store.operationMode === OPERATION_MODES.READ_ONLY
          || self.isAccepting
    };

    self.isRejectDisabled = function() {
      return store.operationMode === OPERATION_MODES.READ_ONLY
          || self.isAccepting
    };

    self.getAcceptTitle = function() {
      return self.isAcceptDisabled()
          ? 'Not possible to accept suggestion while editing'
          : 'Accept suggestion';
    };

    self.getRejectTitle = function() {
      return self.isRejectDisabled()
          ? 'Not possible to reject suggestion while editing'
          : 'Reject suggestion';
    };

    self.accept = function($event) {
      $event.stopPropagation(); // To avoid focus on non existing annotation after deletion and scrolling to the top
      self.isAccepting = true;
      if (!self.annotation.user) {
        flash.info('Please log in to accept suggestions.');
        return Promise.resolve();
      }

      return processSuggestionMerging().then(function(result) {
        acceptSuggestion(self.annotation).then(function(){
          let event = analytics.events.ANNOTATION_DELETED;
          analytics.track(event);
          setTimeout(function() { 
            $rootScope.$broadcast(events.ANNOTATION_DELETED, self.annotation); 
            flash.success('Suggestion successfully merged');
            self.isAccepting = false;
          }, 500);
        }).catch(function (err) {
          flash.error("Suggestion content merging failed");
          self.isAccepting = false;
        });
      }).catch(function (err) {
        flash.error(err.message);
        self.isAccepting = false;
      });
    };

    function acceptSuggestion(annotation) {
      return api.suggestion.accept({
        id: annotation.id,
      }).then(function () {
        return annotation;
      });
    }

    function rejectSuggestion(annotation) {
      return api.suggestion.reject({
        id: annotation.id,
      }).then(function () {
        $rootScope.$broadcast(events.ANNOTATION_DELETED, annotation);
        return annotation;
      }).catch(function (err) {
        flash.error(err.message);
      });
    }
  
    self.reject = function() {
      return $timeout(function() {
        const msg = 'Are you sure you want to reject this suggestion?';
        if ($window.confirm(msg)) {
          $scope.$apply(function() {
            rejectSuggestion(self.annotation).then(function(){
              let event = analytics.events.ANNOTATION_DELETED;
              analytics.track(event);
            });
          });
        }
      }, true);
    };
  }
}

module.exports = {
  controller: LeosSuggestionController, 
  controllerAs: 'vm',
  bindings: {
    annotation: '<',
    onSaving: '&?',
  },
  template: require('../templates/leos-suggestion-buttons.html'),
};
