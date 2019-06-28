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

const proxyquire = require('proxyquire');

describe('sidebar.util.isThirdPartyService', () => {
  let fakeServiceConfig;
  let fakeSettings;
  let isThirdPartyService;

  beforeEach(() => {
    fakeServiceConfig = sinon.stub();
    fakeSettings = {authDomain: 'hypothes.is'};

    isThirdPartyService = proxyquire('../is-third-party-service', {
      '../service-config': fakeServiceConfig,
      '@noCallThru': true,
    });
  });

  it('returns false for first-party services', () => {
    fakeServiceConfig.returns({authority: 'hypothes.is'});

    assert.isFalse(isThirdPartyService(fakeSettings));
  });

  it('returns true for third-party services', () => {
    fakeServiceConfig.returns({authority: 'elifesciences.org'});

    assert.isTrue(isThirdPartyService(fakeSettings));
  });

  it("returns false if there's no service config", () => {
    fakeServiceConfig.returns(null);

    assert.isFalse(isThirdPartyService(fakeSettings));
  });

  // It's not valid for a service config object to not contain an authority
  // (authority is a required field) but at the time of writing the config
  // isn't validated when it's read in, so make sure that isThirdPartyService()
  // handles invalid configs.
  it("returns false if the service config doesn't contain an authority", () => {
    fakeServiceConfig.returns({});

    assert.isFalse(isThirdPartyService(fakeSettings));
  });
});
