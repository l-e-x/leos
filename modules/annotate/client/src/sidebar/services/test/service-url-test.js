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

var proxyquire = require('proxyquire');

/** Return a fake store object. */
function fakeStore() {
  var links = null;
  return {
    updateLinks: function(newLinks) {
      links = newLinks;
    },
    getState: function() {
      return {links: links};
    },
  };
}

function createServiceUrl(linksPromise) {
  var replaceURLParams = sinon.stub().returns(
    {url: 'EXPANDED_URL', params: {}}
  );

  var serviceUrlFactory = proxyquire('../service-url', {
    '../util/url-util': { replaceURLParams: replaceURLParams },
  });

  var store = fakeStore();

  var apiRoutes = {
    links: sinon.stub().returns(linksPromise),
  };

  return {
    store: store,
    apiRoutes,
    serviceUrl: serviceUrlFactory(store, apiRoutes),
    replaceURLParams: replaceURLParams,
  };
}

describe('sidebar.service-url', function () {

  beforeEach(function() {
    sinon.stub(console, 'warn');
  });

  afterEach(function () {
    console.warn.restore();
  });

  context('before the API response has been received', function() {
    var serviceUrl;
    var apiRoutes;

    beforeEach(function() {
      // Create a serviceUrl function with an unresolved Promise that will
      // never be resolved - it never receives the links from store.links().
      var parts = createServiceUrl(new Promise(function() {}));

      serviceUrl = parts.serviceUrl;
      apiRoutes = parts.apiRoutes;
    });

    it('sends one API request for the links at boot time', function() {
      assert.calledOnce(apiRoutes.links);
      assert.isTrue(apiRoutes.links.calledWithExactly());
    });

    it('returns an empty string for any link', function() {
      assert.equal(serviceUrl('foo'), '');
    });

    it('returns an empty string even if link params are given', function() {
      assert.equal(serviceUrl('foo', {bar: 'bar'}), '');
    });
  });

  context('after the API response has been received', function() {
    var store;
    var linksPromise;
    var replaceURLParams;
    var serviceUrl;

    beforeEach(function() {
      // The links Promise that store.links() will return.
      linksPromise = Promise.resolve({
        first_link: 'http://example.com/first_page/:foo',
        second_link: 'http://example.com/second_page',
      });

      var parts = createServiceUrl(linksPromise);

      store = parts.store;
      serviceUrl = parts.serviceUrl;
      replaceURLParams = parts.replaceURLParams;
    });

    it('updates store with the real links', function() {
      return linksPromise.then(function(links) {
        assert.deepEqual(store.getState(), {links: links});
      });
    });

    it('calls replaceURLParams with the path and given params', function() {
      return linksPromise.then(function() {
        var params = {foo: 'bar'};

        serviceUrl('first_link', params);

        assert.calledOnce(replaceURLParams);
        assert.deepEqual(
          replaceURLParams.args[0],
          ['http://example.com/first_page/:foo', params]);
      });
    });

    it('passes an empty params object to replaceURLParams if no params are given', function() {
      return linksPromise.then(function() {
        serviceUrl('first_link');

        assert.calledOnce(replaceURLParams);
        assert.deepEqual(replaceURLParams.args[0][1], {});
      });
    });

    it('returns the expanded URL from replaceURLParams', function() {
      return linksPromise.then(function() {
        var renderedUrl = serviceUrl('first_link');

        assert.equal(renderedUrl, 'EXPANDED_URL');
      });
    });

    it("throws an error if it doesn't have the requested link", function() {
      return linksPromise.then(function() {
        assert.throws(
          function() { serviceUrl('madeUpLinkName'); },
          Error, 'Unknown link madeUpLinkName');
      });
    });

    it('throws an error if replaceURLParams returns unused params', function() {
      var params = {'unused_param_1': 'foo', 'unused_param_2': 'bar'};
      replaceURLParams.returns({
        url: 'EXPANDED_URL',
        params: params,
      });

      return linksPromise.then(function() {
        assert.throws(
          function() { serviceUrl('first_link', params); },
          Error, 'Unknown link parameters: unused_param_1, unused_param_2');
      });
    });
  });
});
