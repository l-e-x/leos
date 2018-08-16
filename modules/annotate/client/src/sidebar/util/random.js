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

/* global Uint8Array */

function byteToHex(val) {
  var str = val.toString(16);
  return str.length === 1 ? '0' + str : str;
}

/**
 * Generate a random hex string of `len` chars.
 *
 * @param {number} - An even-numbered length string to generate.
 * @return {string}
 */
function generateHexString(len) {
  var crypto = window.crypto || window.msCrypto /* IE 11 */;
  var bytes = new Uint8Array(len / 2);
  crypto.getRandomValues(bytes);
  return Array.from(bytes).map(byteToHex).join('');
}

module.exports = {
  generateHexString,
};
