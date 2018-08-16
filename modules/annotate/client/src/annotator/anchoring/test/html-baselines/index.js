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

// Fixtures for anchoring baseline tests. The goals of these tests are to:
//
// 1) Check for unexpected changes in the selectors captured when describing
//    a Range in a web page.
// 2) Test anchoring in larger and more complex pages than the basic anchoring
//    unit tests.
//
// Each fixture consists of:
//
//  - An HTML file for a web page
//  - A set of annotations in the JSON format returned by the Hypothesis API
//
// To add a new fixture:
//
//  1. Open up a web page in the browser and annotate it.
//  2. Save the web page (HTML only) via File -> Save Page As... as
//     `<fixture name>.html` in this directory.
//     It is important to save the page exactly as it was when annotated, since
//     many pages have at least some dynamic content.
//  3. Fetch the annotations for the web page via the Hypothesis API and save
//     them as `<fixture name>.json` in this directory
//  4. Add an entry to the fixture list below.

module.exports = [{
  name: 'Minimal Document',
  html: require('./minimal.html'),
  annotations: require('./minimal.json'),
},{
  name: 'Wikipedia - Regression Testing',
  html: require('./wikipedia-regression-testing.html'),
  annotations: require('./wikipedia-regression-testing.json'),
}];
