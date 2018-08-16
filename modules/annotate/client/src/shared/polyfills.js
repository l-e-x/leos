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

// ES2015
require('core-js/es6/promise');
require('core-js/es6/map');
require('core-js/es6/set');
require('core-js/es6/symbol');
require('core-js/fn/array/find');
require('core-js/fn/array/find-index');
require('core-js/fn/array/from');
require('core-js/fn/array/includes');
require('core-js/fn/object/assign');
require('core-js/fn/string/ends-with');
require('core-js/fn/string/starts-with');

// ES2017
require('core-js/fn/object/entries');
require('core-js/fn/object/values');

// URL constructor, required by IE 10/11,
// early versions of Microsoft Edge.
try {
  var url = new window.URL('https://hypothes.is');

  // Some browsers (eg. PhantomJS 2.x) include a `URL` constructor which works
  // but is broken.
  if (url.hostname !== 'hypothes.is') {
    throw new Error('Broken URL constructor');
  }
} catch (err) {
  require('js-polyfills/url');
}
