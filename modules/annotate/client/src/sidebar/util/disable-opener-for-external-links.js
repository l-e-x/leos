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

/**
 * Prevent windows or tabs opened via links under `root` from accessing their
 * opening `Window`.
 *
 * This makes links with `target="blank"` attributes act as if they also had
 * the `rel="noopener"` [1] attribute set.
 *
 * In addition to preventing tab-jacking [2], this also enables multi-process
 * browsers to more easily use a new process for instances of Hypothesis in the
 * newly-opened tab and works around a bug in Chrome [3]
 *
 * [1] https://developer.mozilla.org/en-US/docs/Web/HTML/Link_types#noopener
 * [2] https://mathiasbynens.github.io/rel-noopener/
 * [3] https://bugs.chromium.org/p/chromium/issues/detail?id=753314
 *
 * @param {Element} root - Root element
 */
function disableOpenerForExternalLinks(root) {
  root.addEventListener('click', event => {
    if (event.target.tagName === 'A') {
      var linkEl = event.target;
      if (linkEl.target === '_blank') {
        linkEl.rel = 'noopener';
      }
    }
  });
}

module.exports = disableOpenerForExternalLinks;
