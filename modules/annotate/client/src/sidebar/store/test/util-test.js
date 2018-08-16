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

var util = require('../util');

var fixtures = {
  update: {
    ADD_ANNOTATIONS: function (state, action) {
      if (!state.annotations) {
        return {annotations: action.annotations};
      }
      return {annotations: state.annotations.concat(action.annotations)};
    },
    SELECT_TAB: function (state, action) {
      return {tab: action.tab};
    },
  },
  countAnnotations: function (state) {
    return state.annotations.length;
  },
};

describe('reducer utils', function () {
  describe('#actionTypes', function () {
    it('returns an object with values equal to keys', function () {
      assert.deepEqual(util.actionTypes({
        SOME_ACTION: sinon.stub(),
        ANOTHER_ACTION: sinon.stub(),
      }), {
        SOME_ACTION: 'SOME_ACTION',
        ANOTHER_ACTION: 'ANOTHER_ACTION',
      });
    });
  });

  describe('#createReducer', function () {
    it('returns a reducer that combines each update function from the input object', function () {
      var reducer = util.createReducer(fixtures.update);
      var newState = reducer({}, {
        type: 'ADD_ANNOTATIONS',
        annotations: [{id: 1}],
      });
      assert.deepEqual(newState, {
        annotations: [{id: 1}],
      });
    });

    it('returns a new object if the action was handled', function () {
      var reducer = util.createReducer(fixtures.update);
      var originalState = {someFlag: false};
      assert.notEqual(reducer(originalState, {type: 'SELECT_TAB', tab: 'notes'}),
        originalState);
    });

    it('returns the original object if the action was not handled', function () {
      var reducer = util.createReducer(fixtures.update);
      var originalState = {someFlag: false};
      assert.equal(reducer(originalState, {type: 'UNKNOWN_ACTION'}), originalState);
    });

    it('preserves state not modified by the update function', function () {
      var reducer = util.createReducer(fixtures.update);
      var newState = reducer({otherFlag: false}, {
        type: 'ADD_ANNOTATIONS',
        annotations: [{id: 1}],
      });
      assert.deepEqual(newState, {
        otherFlag: false,
        annotations: [{id: 1}],
      });
    });

    it('applies update functions from each input object', () => {
      var firstCounterActions = {
        INCREMENT_COUNTER(state) {
          return { firstCounter: state.firstCounter + 1 };
        },
      };
      var secondCounterActions = {
        INCREMENT_COUNTER(state) {
          return { secondCounter: state.secondCounter + 1 };
        },
      };
      var reducer = util.createReducer(firstCounterActions, secondCounterActions);

      var state =  { firstCounter: 5, secondCounter: 10 };
      var action = { type: 'INCREMENT_COUNTER' };
      var newState = reducer(state, action);

      assert.deepEqual(newState, { firstCounter: 6, secondCounter: 11 });
    });
  });

  describe('#bindSelectors', function () {
    it('bound functions call original functions with current value of getState()', function () {
      var annotations = [{id: 1}];
      var getState = sinon.stub().returns({annotations: annotations});
      var bound = util.bindSelectors({
        countAnnotations: fixtures.countAnnotations,
      }, getState);

      assert.equal(bound.countAnnotations(), 1);

      getState.returns({annotations: annotations.concat([{id: 2}])});
      assert.equal(bound.countAnnotations(), 2);
    });
  });
});
