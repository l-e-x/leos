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

var retry = require('retry');

/**
 * Retry a Promise-returning operation until it succeeds or
 * fails after a set number of attempts.
 *
 * @param {Function} opFn - The operation to retry
 * @param {Object} options - The options object to pass to retry.operation()
 *
 * @return A promise for the first successful result of the operation, if
 *         it succeeds within the allowed number of attempts.
 */
function retryPromiseOperation(opFn, options) {
  return new Promise(function (resolve, reject) {
    var operation = retry.operation(options);
    operation.attempt(function () {
      opFn().then(function (result) {
        operation.retry();
        resolve(result);
      }).catch(function (err) {
        if (!operation.retry(err)) {
          reject(err);
        }
      });
    });
  });
}

module.exports = {
  retryPromiseOperation: retryPromiseOperation,
};
