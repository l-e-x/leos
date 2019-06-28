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

var events = require('../shared/bridge-events');

var ANNOTATION_COUNT_ATTR = 'data-hypothesis-annotation-count';

/**
 * Update the elements in the container element with the count data attribute
 * with the new annotation count.
 *
 * @param {Element} rootEl - The DOM element which contains the elements that
 * display annotation count.
 */

function annotationCounts(rootEl, crossframe) {
  crossframe.on(events.PUBLIC_ANNOTATION_COUNT_CHANGED, updateAnnotationCountElems);

  function updateAnnotationCountElems(newCount) {
    var elems = rootEl.querySelectorAll('['+ANNOTATION_COUNT_ATTR+']');
    Array.from(elems).forEach(function(elem) {
      elem.textContent = newCount;
    });
  }
}

module.exports = annotationCounts;
