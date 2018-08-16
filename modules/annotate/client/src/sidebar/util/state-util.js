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

/**
 * Return a value from app state when it meets certain criteria.
 *
 * `await` returns a Promise which resolves when a selector function,
 * which reads values from a Redux store, returns non-null.
 *
 * @param {Object} store - Redux store
 * @param {Function<T|null>} selector - Function which returns a value from the
 *   store if the criteria is met or `null` otherwise.
 * @return {Promise<T>}
 */
function awaitStateChange(store, selector) {
  var result = selector(store);
  if (result !== null) {
    return Promise.resolve(result);
  }
  return new Promise(resolve => {
    var unsubscribe = store.subscribe(() => {
      var result = selector(store);
      if (result !== null) {
        unsubscribe();
        resolve(result);
      }
    });
  });
}

module.exports = { awaitStateChange } ;
