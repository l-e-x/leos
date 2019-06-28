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

var disableOpenerForExternalLinks = require('../disable-opener-for-external-links');

describe('sidebar.util.disable-opener-for-external-links', () => {
  var containerEl;
  var linkEl;

  beforeEach(() => {
    containerEl = document.createElement('div');
    linkEl = document.createElement('a');
    containerEl.appendChild(linkEl);
    document.body.appendChild(containerEl);
  });

  afterEach(() => {
    containerEl.remove();
  });

  function clickLink() {
    linkEl.dispatchEvent(new Event('click', {
      bubbles: true,
      cancelable: true,
    }));
  }

  it('disables opener for external links', () => {
    linkEl.target = '_blank';

    disableOpenerForExternalLinks(containerEl);
    clickLink();

    assert.equal(linkEl.rel, 'noopener');
  });

  it('does not disable opener for internal links', () => {
    disableOpenerForExternalLinks(containerEl);
    clickLink();
    assert.equal(linkEl.rel, '');
  });
});
