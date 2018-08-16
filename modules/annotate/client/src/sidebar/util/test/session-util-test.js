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

var sessionUtil = require('../session-util');

describe('sessionUtil.shouldShowSidebarTutorial', function () {
  it('shows sidebar tutorial if the settings object has the show_sidebar_tutorial key set', function () {
    var sessionState = {
      preferences: {
        show_sidebar_tutorial: true,
      },
    };

    assert.isTrue(sessionUtil.shouldShowSidebarTutorial(sessionState));
  });

  it('hides sidebar tutorial if the settings object does not have the show_sidebar_tutorial key set', function () {
    var sessionState = {
      preferences: {
        show_sidebar_tutorial: false,
      },
    };

    assert.isFalse(sessionUtil.shouldShowSidebarTutorial(sessionState));
  });
});
