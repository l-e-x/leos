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

var bridgeEvents = require('../../shared/bridge-events');
var { isThirdPartyUser } = require('../util/account-id');
var serviceConfig = require('../service-config');

module.exports = {
  controllerAs: 'vm',

  //@ngInject
  controller: function (bridge, serviceUrl, settings, $window) {
    this.serviceUrl = serviceUrl;

    this.isThirdPartyUser = function() {
      return isThirdPartyUser(this.auth.userid, settings.authDomain);
    };

    this.shouldShowLogOutButton = function () {
      if (this.auth.status !== 'logged-in') {
        return false;
      }
      var service = serviceConfig(settings);
      if (service && !service.onLogoutRequestProvided) {
        return false;
      }
      return true;
    };

    this.shouldEnableProfileButton = function () {
      var service = serviceConfig(settings);
      if (service) {
        return service.onProfileRequestProvided;
      }
      return true;
    };

    this.showProfile = function () {
      if (this.isThirdPartyUser()) {
        bridge.call(bridgeEvents.PROFILE_REQUESTED);
        return;
      }
      $window.open(this.serviceUrl('user', {user: this.auth.username}));
    };
  },

  bindings: {
    /**
     * An object representing the current authentication status.
     */
    auth: '<',
    /**
     * Called when the user clicks on the "About this version" text.
     */
    onShowHelpPanel: '&',
    /**
     * Called when the user clicks on the "Log in" text.
     */
    onLogin: '&',
    /**
     * Called when the user clicks on the "Sign Up" text.
     */
    onSignUp: '&',
    /**
     * Called when the user clicks on the "Log out" text.
     */
    onLogout: '&',
    /**
     * Whether or not to use the new design for the control.
     *
     * FIXME: should be removed when the old design is deprecated.
     */
    newStyle: '<',
  },
  template: require('../templates/login-control.html'),
};
