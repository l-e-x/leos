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

// cached date formatting instance.
// See https://github.com/hypothesis/h/issues/2820#issuecomment-166285361
var formatter;

/**
 * Returns a standard human-readable representation
 * of a date and time.
 */
function format(date) {
  if (typeof Intl !== 'undefined' && Intl.DateTimeFormat) {
    if (!formatter) {
      formatter = new Intl.DateTimeFormat(undefined, {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        weekday: 'long',
        hour: '2-digit',
        minute: '2-digit',
      });
    }
    return formatter.format(date);
  } else {
    // IE < 11, Safari <= 9.0.
    // In English, this generates the string most similar to
    // the toLocaleDateString() result above.
    return date.toDateString() + ' ' + date.toLocaleTimeString();
  }
}

module.exports = {
  format: format,
};
