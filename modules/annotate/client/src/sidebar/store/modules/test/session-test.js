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

var session = require('../session');

var util = require('../../util');

var { init, actions, selectors } = session;
var update = util.createReducer(session.update);

describe('sidebar.reducers.session', function () {
  describe('#updateSession', function () {
    it('updates the session state', function () {
      var newSession = Object.assign(init(), {userid: 'john'});
      var state = update(init(), actions.updateSession(newSession));
      assert.deepEqual(state.session, newSession);
    });
  });

  describe('#profile', () => {
    it("returns the user's profile", () => {
      var newSession = Object.assign(init(), {userid: 'john'});
      var state = update(init(), actions.updateSession(newSession));
      assert.equal(selectors.profile(state), newSession);
    });
  });
});
