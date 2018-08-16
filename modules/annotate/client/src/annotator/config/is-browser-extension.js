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
 * Return true if the client is from a browser extension.
 *
 * @returns {boolean} true if this instance of the Hypothesis client is one
 *   distributed in a browser extension, false if it's one embedded in a
 *   website.
 *
 */
function isBrowserExtension(app) {
  return !(app.startsWith('http://') || app.startsWith('https://'));
}

module.exports = isBrowserExtension;
