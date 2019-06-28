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

var redux = require('redux');

/**
 * Utility function that creates a fake Redux store for use in tests.
 *
 * Unlike a real store, this has a `setState()` method that can be used to
 * set the state directly.
 *
 * @param {Object} initialState - Initial state for the store
 * @param {Object} methods - A set of additional properties to mixin to the
 *        returned store.
 * @return {Object} Redux store
 */
function fakeStore(initialState, methods) {
  function update(state, action) {
    if (action.state) {
      return Object.assign({}, state, action.state);
    } else {
      return state;
    }
  }

  var store = redux.createStore(update, initialState);

  store.setState = function (state) {
    store.dispatch({type: 'SET_STATE', state: state});
  };

  return Object.assign(store, methods);
}

module.exports = fakeStore;
