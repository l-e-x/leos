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

var links = require('../links');

var init   = links.init;
var update = links.update.UPDATE_LINKS;
var action = links.actions.updateLinks;

describe('sidebar.reducers.links', function() {
  describe('#init()', function() {
    it('returns a null links object', function() {
      assert.deepEqual(init(), {links: null});
    });
  });

  describe('#update.UPDATE_LINKS()', function() {
    it('returns the given newLinks as the links object', function() {
      assert.deepEqual(
        update('CURRENT_STATE', {newLinks: 'NEW_LINKS'}),
        {links: 'NEW_LINKS'});
    });
  });

  describe('#actions.updateLinks()', function() {
    it('returns an UPDATE_LINKS action object for the given newLinks', function() {
      assert.deepEqual(
        action('NEW_LINKS'),
        { type: 'UPDATE_LINKS', newLinks: 'NEW_LINKS' });
    });
  });
});
