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

/* eslint-disable no-console */

var redux = require('redux');

var debugMiddleware = require('../debug-middleware');

function id(state) {
  return state;
}

describe('debug middleware', function () {
  var store;

  beforeEach(function () {
    sinon.stub(console, 'log');
    sinon.stub(console, 'group');
    sinon.stub(console, 'groupEnd');

    var enhancer = redux.applyMiddleware(debugMiddleware);
    store = redux.createStore(id, {}, enhancer);
  });

  afterEach(function () {
    console.log.restore();
    console.group.restore();
    console.groupEnd.restore();

    delete window.debug;
  });

  it('logs app state changes when "window.debug" is truthy', function () {
    window.debug = true;
    store.dispatch({type: 'SOMETHING_HAPPENED'});
    assert.called(console.log);
  });

  it('logs nothing when "window.debug" is falsey', function () {
    store.dispatch({type: 'SOMETHING_HAPPENED'});
    assert.notCalled(console.log);
  });
});
