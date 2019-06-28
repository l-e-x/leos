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

var scopeTimeout = require('../scope-timeout');

function FakeScope() {
  this.listeners = {};
  this.$on = function (event, fn) {
    this.listeners[event] = this.listeners[event] || [];
    this.listeners[event].push(fn);
    return function () {
      this.listeners[event] = this.listeners[event].filter(function (otherFn) {
        return otherFn !== fn;
      });
    }.bind(this);
  };
}

describe('scope-timeout', function () {
  var fakeSetTimeout;
  var fakeClearTimeout;

  beforeEach(function () {
    fakeSetTimeout = sinon.stub().returns(42);
    fakeClearTimeout = sinon.stub();
  });

  it('schedules a timeout', function () {
    var $scope = new FakeScope();
    var callback = sinon.stub();
    scopeTimeout($scope, callback, 0, fakeSetTimeout, fakeClearTimeout);
    assert.calledOnce(fakeSetTimeout);
    var timeoutFn = fakeSetTimeout.args[0][0];
    timeoutFn();
    assert.called(callback);
  });

  it('removes the scope listener when the timeout fires', function () {
    var $scope = new FakeScope();
    var callback = sinon.stub();
    scopeTimeout($scope, callback, 0, fakeSetTimeout, fakeClearTimeout);
    assert.equal($scope.listeners.$destroy.length, 1);
    var timeoutFn = fakeSetTimeout.args[0][0];
    timeoutFn();
    assert.equal($scope.listeners.$destroy.length, 0);
  });

  it('clears the timeout when the scope is destroyed', function () {
    var $scope = new FakeScope();
    var callback = sinon.stub();
    scopeTimeout($scope, callback, 0, fakeSetTimeout, fakeClearTimeout);
    var destroyFn = $scope.listeners.$destroy[0];
    destroyFn();
    assert.calledWith(fakeClearTimeout, 42);
  });
});
