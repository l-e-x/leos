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

var annotationMetadata = require('../annotation-metadata');
var memoize = require('../util/memoize');
var { isThirdPartyUser, username } = require('../util/account-id');

// @ngInject
function AnnotationHeaderController(features, groups, settings, serviceUrl) {
  var self = this;

  this.user = function () {
    return self.annotation.user;
  };

  this.displayName = () => {
    var userInfo = this.annotation.user_info;
    var isThirdPartyUser_ = isThirdPartyUser(this.annotation.user, settings.authDomain);
    if (features.flagEnabled('client_display_names') || isThirdPartyUser_) {
      // userInfo is undefined if the api_render_user_info feature flag is off.
      if (userInfo) {
        // display_name is null if the user doesn't have a display name.
        if (userInfo.display_name) {
          return userInfo.display_name;
        }
      }
    }
    return username(this.annotation.user);
  };

  this.isThirdPartyUser = function () {
    return isThirdPartyUser(self.annotation.user, settings.authDomain);
  };

  this.thirdPartyUsernameLink = function () {
    return settings.usernameUrl ?
      settings.usernameUrl + username(this.annotation.user):
      null;
  };

  this.serviceUrl = serviceUrl;

  this.group = function () {
    return groups.get(self.annotation.group);
  };

  var documentMeta = memoize(annotationMetadata.domainAndTitle);
  this.documentMeta = function () {
    return documentMeta(self.annotation);
  };

  this.updated = function () {
    return self.annotation.updated;
  };

  this.htmlLink = function () {
    if (self.annotation.links && self.annotation.links.html) {
      return self.annotation.links.html;
    }
    return '';
  };
}

/**
 * Header component for an annotation card.
 *
 * Header which displays the username, last update timestamp and other key
 * metadata about an annotation.
 */
module.exports = {
  controller: AnnotationHeaderController,
  controllerAs: 'vm',
  bindings: {
    /**
     * The saved annotation
     */
    annotation: '<',

    /**
     * True if the annotation is private or will become private when the user
     * saves their changes.
     */
    isPrivate: '<',

    /** True if the user is currently editing the annotation. */
    isEditing: '<',

    /**
     * True if the annotation is a highlight.
     * FIXME: This should determined in AnnotationHeaderController
     */
    isHighlight: '<',
    onReplyCountClick: '&',
    replyCount: '<',

    /** True if document metadata should be shown. */
    showDocumentInfo: '<',
  },
  template: require('../templates/annotation-header.html'),
};
