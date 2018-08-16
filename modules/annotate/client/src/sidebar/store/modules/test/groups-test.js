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

const createStore = require('../../create-store');
const groups = require('../groups');

describe('sidebar.store.modules.groups', () => {
  const publicGroup = {
    id: '__world__',
    name: 'Public',
  };

  const privateGroup = {
    id: 'foo',
    name: 'Private',
  };

  let store;

  beforeEach(() => {
    store = createStore([groups]);
  });

  describe('focusGroup', () => {
    it('updates the focused group if valid', () => {
      store.loadGroups([publicGroup]);
      store.focusGroup(publicGroup.id);
      assert.equal(store.getState().focusedGroupId, publicGroup.id);
    });

    it('does not set the focused group if invalid', () => {
      store.loadGroups([publicGroup]);
      store.focusGroup(privateGroup.id);
      assert.equal(store.getState().focusedGroupId, null);
    });
  });

  describe('loadGroups', () => {
    it('updates the set of groups', () => {
      store.loadGroups([publicGroup]);
      assert.deepEqual(store.getState().groups, [publicGroup]);
    });

    it('resets the focused group if not in new set of groups', () => {
      store.loadGroups([publicGroup]);
      store.focusGroup(publicGroup.id);
      store.loadGroups([]);

      assert.equal(store.getState().focusedGroupId, null);
    });

    it('leaves focused group unchanged if in new set of groups', () => {
      store.loadGroups([publicGroup]);
      store.focusGroup(publicGroup.id);
      store.loadGroups([publicGroup, privateGroup]);

      assert.equal(store.getState().focusedGroupId, publicGroup.id);
    });
  });

  describe('allGroups', () => {
    it('returns all groups', () => {
      store.loadGroups([publicGroup, privateGroup]);
      assert.deepEqual(store.allGroups(), [publicGroup, privateGroup]);
    });
  });

  describe('getGroup', () => {
    it('returns the group with the given ID', () => {
      store.loadGroups([publicGroup, privateGroup]);
      assert.deepEqual(store.getGroup(privateGroup.id), privateGroup);
    });
  });

  describe('focusedGroup', () => {
    it('returns `null` if no group is focused', () => {
      assert.equal(store.focusedGroup(), null);
    });

    it('returns the focused group if a group has been focused', () => {
      store.loadGroups([privateGroup]);
      store.focusGroup(privateGroup.id);
      assert.deepEqual(store.focusedGroup(), privateGroup);
    });
  });

  describe('focusedGroupId', () => {
    it('returns `null` if no group is focused', () => {
      assert.equal(store.focusedGroupId(), null);
    });

    it('returns the focused group ID if a group has been focused', () => {
      store.loadGroups([privateGroup]);
      store.focusGroup(privateGroup.id);
      assert.equal(store.focusedGroupId(), privateGroup.id);
    });
  });
});
