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

var retryUtil = require('../retry');
var toResult = require('../../../shared/test/promise-util').toResult;

describe('sidebar.util.retry', function () {
  describe('.retryPromiseOperation', function () {
    it('should return the result of the operation function', function () {
      var operation = sinon.stub().returns(Promise.resolve(42));
      var wrappedOperation = retryUtil.retryPromiseOperation(operation);
      return wrappedOperation.then(function (result) {
        assert.equal(result, 42);
      });
    });

    it('should retry the operation if it fails', function () {
      var results = [new Error('fail'), 'ok'];
      var operation = sinon.spy(function () {
        var nextResult = results.shift();
        if (nextResult instanceof Error) {
          return Promise.reject(nextResult);
        } else {
          return Promise.resolve(nextResult);
        }
      });
      var wrappedOperation = retryUtil.retryPromiseOperation(operation, {
        minTimeout: 1,
      });
      return wrappedOperation.then(function (result) {
        assert.equal(result, 'ok');
      });
    });

    it('should return the error if it repeatedly fails', function () {
      var error = new Error('error');
      var operation = sinon.spy(function () {
        return Promise.reject(error);
      });
      var wrappedOperation = retryUtil.retryPromiseOperation(operation, {
        minTimeout: 3,
        retries: 2,
      });
      return toResult(wrappedOperation).then(function (result) {
        assert.equal(result.error, error);
      });
    });
  });
});
