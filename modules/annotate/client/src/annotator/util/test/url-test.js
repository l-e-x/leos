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

const { normalizeURI } = require('../url');

describe('annotator.util.url', () => {
  describe('normalizeURI', () => {
    it('resolves relative URLs against the provided base URI', () => {
      const base = 'http://example.com';
      assert.equal(normalizeURI('index.html', base), `${base}/index.html`);
    });

    it('resolves relative URLs against the document URI, if no base URI is provided', () => {
      // Strip filename from base URI.
      const base = document.baseURI.replace(/\/[^/]*$/, '');
      assert.equal(normalizeURI('foo.html'), `${base}/foo.html`);
    });

    it('does not modify absolute URIs', () => {
      const url = 'http://example.com/wibble';
      assert.equal(normalizeURI(url), url);
    });

    it('removes the fragment identifier', () => {
      const url = 'http://example.com/wibble#fragment';
      assert.equal(normalizeURI(url), 'http://example.com/wibble');
    });

    [
      'file:///Users/jane/article.pdf',
      'doi:10.1234/4567',
    ].forEach(url => {
      it('does not modify absolute non-HTTP/HTTPS URLs', () => {
        assert.equal(normalizeURI(url), url);
      });
    });
  });
});
