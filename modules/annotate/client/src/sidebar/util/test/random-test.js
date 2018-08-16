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

var random = require('../random');

describe('sidebar.util.random', () => {
  describe('#generateHexString', () => {
    [2,4,8,16].forEach((len) => {
      it(`returns a ${len} digit hex string`, () => {
        var re = new RegExp(`^[0-9a-fA-F]{${len}}$`);
        var str = random.generateHexString(len);
        assert.isTrue(re.test(str));
      });
    });
  });
});
