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

const createStore = require('../create-store');

const counterModule = {
  init(value = 0) {
    return { count: value };
  },

  update: {
    ['INCREMENT_COUNTER'](state, action) {
      return { count: state.count + action.amount };
    },
  },

  actions: {
    increment(amount) {
      return { type: 'INCREMENT_COUNTER', amount };
    },
  },

  selectors: {
    getCount(state) {
      return state.count;
    },
  },
};

function counterStore(initArgs = [], middleware = []) {
  return createStore([counterModule], initArgs, middleware);
}

describe('sidebar.store.create-store', () => {
  it('returns a working Redux store', () => {
    const store = counterStore();
    store.dispatch(counterModule.actions.increment(5));
    assert.equal(store.getState().count, 5);
  });

  it('notifies subscribers when state changes', () => {
    const store = counterStore();
    const subscriber = sinon.spy(() => assert.equal(store.getCount(), 1));

    store.subscribe(subscriber);
    store.increment(1);

    assert.calledWith(subscriber);
  });

  it('passes initial state args to `init` function', () => {
    const store = counterStore([21]);
    assert.equal(store.getState().count, 21);
  });

  it('adds actions as methods to the store', () => {
    const store = counterStore();
    store.increment(5);
    assert.equal(store.getState().count, 5);
  });

  it('adds selectors as methods to the store', () => {
    const store = counterStore();
    store.dispatch(counterModule.actions.increment(5));
    assert.equal(store.getCount(), 5);
  });

  it('applies `thunk` middleware by default', () => {
    const store = counterStore();
    const doubleAction = (dispatch, getState) => {
      dispatch(counterModule.actions.increment(getState().count));
    };

    store.increment(5);
    store.dispatch(doubleAction);

    assert.equal(store.getCount(), 10);
  });

  it('applies additional middleware', () => {
    const actions = [];
    const middleware = () => {
      return next => {
        return action => {
          actions.push(action);
          return next(action);
        };
      };
    };
    const store = counterStore([], [middleware]);

    store.increment(5);

    assert.deepEqual(actions, [{ type: 'INCREMENT_COUNTER', amount: 5 }]);
  });
});
