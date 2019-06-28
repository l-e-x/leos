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

var PDFMetadata = require('../pdf-metadata');

describe('pdf-metadata', function () {
  it('waits for the PDF to load before returning metadata', function () {
    var fakeApp = {};
    var pdfMetadata = new PDFMetadata(fakeApp);

    var event = document.createEvent('Event');
    event.initEvent('documentload', false, false);
    fakeApp.url = 'http://fake.com';
    fakeApp.documentFingerprint = 'fakeFingerprint';
    window.dispatchEvent(event);

    return pdfMetadata.getUri().then(function (uri) {
      assert.equal(uri, 'http://fake.com/');
    });
  });

  it('does not wait for the PDF to load if it has already loaded', function () {
    var fakePDFViewerApplication = {
      url: 'http://fake.com',
      documentFingerprint: 'fakeFingerprint',
    };
    var pdfMetadata = new PDFMetadata(fakePDFViewerApplication);
    return pdfMetadata.getUri().then(function (uri) {
      assert.equal(uri, 'http://fake.com/');
    });
  });

  describe('metadata sources', function () {
    var pdfMetadata;
    var fakePDFViewerApplication = {
      documentFingerprint: 'fakeFingerprint',
      documentInfo: {
        Title: 'fakeTitle',
      },
      metadata: {
        metadata: {
          'dc:title': 'fakeTitle',
        },
      },
      url: 'http://fake.com/',
    };

    beforeEach(function () {
      pdfMetadata = new PDFMetadata(fakePDFViewerApplication);
    });

    describe('#getUri', function () {
      it('returns the non-file URI', function() {
        return pdfMetadata.getUri().then(function (uri) {
          assert.equal(uri, 'http://fake.com/');
        });
      });

      it('returns the fingerprint as a URN when the PDF URL is a local file', function () {
        var fakePDFViewerApplication = {
          url: 'file:///test.pdf',
          documentFingerprint: 'fakeFingerprint',
        };
        var pdfMetadata = new PDFMetadata(fakePDFViewerApplication);

        return pdfMetadata.getUri().then(function (uri) {
          assert.equal(uri, 'urn:x-pdf:fakeFingerprint');
        });
      });

      it('resolves relative URLs', () => {
        var fakePDFViewerApplication = {
          url: 'index.php?action=download&file_id=wibble',
          documentFingerprint: 'fakeFingerprint',
        };
        var pdfMetadata = new PDFMetadata(fakePDFViewerApplication);

        return pdfMetadata.getUri().then(uri => {
          var expected = new URL(fakePDFViewerApplication.url,
                                 document.location.href).toString();
          assert.equal(uri, expected);
        });
      });
    });

    describe('#getMetadata', function () {
      it('gets the title from the dc:title field', function () {
        var expectedMetadata = {
          title: 'dcTitle',
          link: [{href: 'urn:x-pdf:' + fakePDFViewerApplication.documentFingerprint},
            {href: fakePDFViewerApplication.url}],
          documentFingerprint: fakePDFViewerApplication.documentFingerprint,
        };

        fakePDFViewerApplication.metadata.has = sinon.stub().returns(true);
        fakePDFViewerApplication.metadata.get = sinon.stub().returns('dcTitle');

        return pdfMetadata.getMetadata().then(function (actualMetadata) {
          assert.deepEqual(actualMetadata, expectedMetadata);
        });
      });

      it('gets the title from the documentInfo.Title field', function () {
        var expectedMetadata = {
          title: fakePDFViewerApplication.documentInfo.Title,
          link: [{href: 'urn:x-pdf:' + fakePDFViewerApplication.documentFingerprint},
            {href: fakePDFViewerApplication.url}],
          documentFingerprint: fakePDFViewerApplication.documentFingerprint,
        };

        fakePDFViewerApplication.metadata.has = sinon.stub().returns(false);

        return pdfMetadata.getMetadata().then(function (actualMetadata) {
          assert.deepEqual(actualMetadata, expectedMetadata);
        });
      });

      it('does not save file:// URLs in document metadata', function () {
        var pdfMetadata;
        var fakePDFViewerApplication = {
          documentFingerprint: 'fakeFingerprint',
          url: 'file://fake.pdf',
        };
        var expectedMetadata = {
          link: [{href: 'urn:x-pdf:' + fakePDFViewerApplication.documentFingerprint}],
        };

        pdfMetadata = new PDFMetadata(fakePDFViewerApplication);

        return pdfMetadata.getMetadata().then(function (actualMetadata) {
          assert.equal(actualMetadata.link.length, 1);
          assert.equal(actualMetadata.link[0].href, expectedMetadata.link[0].href);
        });
      });
    });
  });
});
