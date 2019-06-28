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

var SIDEBAR_TRIGGER_BTN_ATTR = 'data-hypothesis-trigger';

/**
 * Show the sidebar when user clicks on an element with the
 * trigger data attribute.
 *
 * @param {Element} rootEl - The DOM element which contains the trigger elements.
 * @param {Object} showFn - Function which shows the sidebar.
 */

function trigger(rootEl, showFn) {

  var triggerElems = rootEl.querySelectorAll('['+SIDEBAR_TRIGGER_BTN_ATTR+']');

  Array.from(triggerElems).forEach(function(triggerElem) {
    triggerElem.addEventListener('click', handleCommand);
  });

  function handleCommand(event) {
    showFn();
    event.stopPropagation();
  }
}

module.exports = trigger;
