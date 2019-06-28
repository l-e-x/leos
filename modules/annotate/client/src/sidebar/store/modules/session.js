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

var util = require('../util');

function init() {
  return {
    /**
     * The state of the user's login session.
     *
     * This includes their user ID, set of enabled features, and the list of
     * groups they are a member of.
     */
    session: {
      /** A map of features that are enabled for the current user. */
      features: {},
      /** A map of preference names and values. */
      preferences: {},
      /**
       * The authenticated user ID or null if the user is not logged in.
       */
      userid: null,
    },
  };
}

var update = {
  UPDATE_SESSION: function (state, action) {
    return {
      session: action.session,
    };
  },
};

var actions = util.actionTypes(update);

/**
 * Update the session state.
 */
function updateSession(session) {
  return {
    type: actions.UPDATE_SESSION,
    session: session,
  };
}

/**
 * Return true if a given feature flag is enabled.
 *
 * @param {object} state - The application state
 * @param {string} feature - The name of the feature flag. This matches the
 *        name of the feature flag as declared in the Hypothesis service.
 */
function isFeatureEnabled(state, feature) {
  return !!state.session.features[feature];
}

/**
 * Return the user's profile.
 *
 * Returns the current user's profile fetched from the `/api/profile` endpoint.
 */
function profile(state) {
  return state.session;
}

module.exports = {
  init,
  update,

  actions: {
    updateSession,
  },

  selectors: {
    isFeatureEnabled,
    profile,
  },
};
