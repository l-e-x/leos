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

var viewer = require('../viewer');

var util = require('../../util');

var { init, actions, selectors } = viewer;
var update = util.createReducer(viewer.update);

describe('viewer reducer', function () {
  describe('#setAppIsSidebar', function () {
    it('sets a flag indicating that the app is the sidebar', function () {
      var state = update(init(), actions.setAppIsSidebar(true));
      assert.isTrue(selectors.isSidebar(state));
    });

    it('sets a flag indicating that the app is not the sidebar', function () {
      var state = update(init(), actions.setAppIsSidebar(false));
      assert.isFalse(selectors.isSidebar(state));
    });
  });
});
