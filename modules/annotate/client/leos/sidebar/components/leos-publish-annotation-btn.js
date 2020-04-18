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

var publishAnnotation = require('../../../src/sidebar/components/publish-annotation-btn');
var serviceConfig = require('../../../src/sidebar/service-config');
var SYSTEMIDS = require('../../../leos/shared/systemId');
var OPERATION_MODES = require('../../../leos/shared/operationMode');

// @ngInject
function LeosPublishAnnotationController($injector, groups, settings) {
    var svc = serviceConfig(settings);
    var groupsToLoad = (svc && svc.authority && svc.authority === SYSTEMIDS.ISC) ? [] : groups;
    $injector.invoke(publishAnnotation.controller, this, {groups: groupsToLoad});

    this.groupCategory = function (group) {
        return group.type === 'open' ? 'public' : 'group';
    };

    this.getAllGroups = function () {
        var searchBarSelectGroup = groups.focused();
        if (settings.operationMode === OPERATION_MODES.PRIVATE) {
            //on private mode, annotations cannot be published to any group, only to self
            return [];
        } else if (searchBarSelectGroup.type === 'open') {
            return groups.all();
        } else {
          return [searchBarSelectGroup];
        }
    };
    
    this.isAuthorityVisible = function() {
        var svc = serviceConfig(settings);
        var isVisible = true;
        if(svc && svc.authority && svc.authority === SYSTEMIDS.ISC) {
            isVisible = false;
        }
        return isVisible;
    };
}

module.exports = {
    controller: LeosPublishAnnotationController,
    bindings: {
      group: '<',
      updateSelectedGroup: '&',
      canPost: '<',
      isShared: '<',
      onCancel: '&',
      onSave: '&',
      onSetPrivacy: '&'
    },
    controllerAs: 'vm',
    template: require('../templates/leos-publish-annotation-btn.html')
};
