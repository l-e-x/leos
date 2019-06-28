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

var Permissions = require('../permissions');

var userid = 'acct:flash@gord.on';

describe('permissions', function () {
  var fakeLocalStorage;
  var permissions;

  beforeEach(function () {
    fakeLocalStorage = {
      getItem: sinon.stub().returns(null),
      setItem: sinon.stub(),
    };
    permissions = new Permissions(fakeLocalStorage);
  });

  describe('#private', function () {
    it('only allows the user to read the annotation', function () {
      assert.deepEqual(permissions.private(userid), {
        read: [userid],
        update: [userid],
        delete: [userid],
      });
    });
  });

  describe('#shared', function () {
    it('allows the group to read the annotation', function () {
      assert.deepEqual(permissions.shared(userid, 'gid'), {
        read: ['group:gid'],
        update: [userid],
        delete: [userid],
      });
    });
  });

  describe('#default', function () {
    it('returns shared permissions by default', function () {
      assert.deepEqual(permissions.default(userid, 'gid'),
                       permissions.shared(userid, 'gid'));
    });

    it('returns private permissions if the saved level is "private"', function () {
      fakeLocalStorage.getItem.returns('private');
      assert.deepEqual(permissions.default(userid, 'gid'),
                       permissions.private(userid));
    });

    it('returns shared permissions if the saved level is "shared"', function () {
      fakeLocalStorage.getItem.returns('shared');
      assert.deepEqual(permissions.default(userid, 'gid'),
                       permissions.shared(userid, 'gid'));
    });
  });

  describe('#setDefault', function () {
    it('saves the default permissions', function () {
      permissions.setDefault('private');
      assert.calledWith(fakeLocalStorage.setItem, 'hypothesis.privacy', 'private');
    });
  });

  describe('#isShared', function () {
    it('returns true if a group can read the annotation', function () {
      var perms = permissions.shared(userid, 'gid');
      assert.isTrue(permissions.isShared(perms));
    });

    it('returns false if only specific users can read the annotation', function () {
      var perms = permissions.private(userid);
      assert.isFalse(permissions.isShared(perms));
    });
  });

  describe('#permits', function () {
    it('returns true if the user can perform the action', function () {
      var perms = permissions.private(userid);
      assert.isTrue(permissions.permits(perms, 'update', userid));
      assert.isTrue(permissions.permits(perms, 'delete', userid));
    });

    it('returns false if the user cannot perform the action', function () {
      var perms = permissions.private('acct:not.flash@gord.on');
      assert.isFalse(permissions.permits(perms, 'update', userid));
      assert.isFalse(permissions.permits(perms, 'delete', userid));
    });
  });
});
