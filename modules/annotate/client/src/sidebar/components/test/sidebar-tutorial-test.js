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

var Controller = require('../sidebar-tutorial').controller;

describe('SidebarTutorialController', function () {

  describe('showSidebarTutorial', function () {
    var settings = {};

    it('returns true if show_sidebar_tutorial is true', function () {
      var session = {
        state: {
          preferences: {
            show_sidebar_tutorial: true,
          },
        },
      };
      var controller = new Controller(session, settings);

      var result = controller.showSidebarTutorial();

      assert.equal(result, true);
    });

    it('returns false if show_sidebar_tutorial is false', function () {
      var session = {
        state: {
          preferences: {
            show_sidebar_tutorial: false,
          },
        },
      };
      var controller = new Controller(session, settings);

      var result = controller.showSidebarTutorial();

      assert.equal(result, false);
    });

    it('returns false if show_sidebar_tutorial is missing', function () {
      var session = {state: {preferences: {}}};
      var controller = new Controller(session, settings);

      var result = controller.showSidebarTutorial();

      assert.equal(result, false);
    });
  });
});
