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

var unicode = require('../unicode')();

describe('unicode', () => {
  describe('#fold', () => {
    it('removes hungarian marks', () => {
      var text = 'Fürge rőt róka túlszökik zsíros étkű kutyán';
      var decoded = unicode.fold(unicode.normalize(text));
      var expected = 'Furge rot roka tulszokik zsiros etku kutyan';

      assert.equal(decoded, expected);
    });

    it('removes greek marks', () => {
      var text = 'Καλημέρα κόσμε';
      var decoded = unicode.fold(unicode.normalize(text));
      var expected = 'Καλημερα κοσμε';

      assert.equal(decoded, expected);
    });

    it('removes japanese marks', () => {
      var text = 'カタカナコンバータ';
      var decoded = unicode.fold(unicode.normalize(text));
      var expected = 'カタカナコンハータ';

      assert.equal(decoded, expected);
    });

    it('removes marathi marks', () => {
      var text = 'काचं शक्नोम्यत्तुम';
      var decoded = unicode.fold(unicode.normalize(text));
      var expected = 'कच शकनमयततम';

      assert.equal(decoded, expected);
    });

    it('removes thai marks', () => {
      var text = 'ฉันกินกระจกได้ แต่มันไม่ทำให้ฉันเจ็บ';
      var decoded = unicode.fold(unicode.normalize(text));
      var expected = 'ฉนกนกระจกได แตมนไมทาใหฉนเจบ';

      assert.equal(decoded, expected);
    });

    it('removes all marks', () => {
      var text = '̀ ́ ̂ ̃ ̄ ̅ ̆ ̇ ̈ ̉ ̊ ̋ ̌ ̍ ̎ ̏ ̐ ̑ ̒ ̓ ̔ ̕ ̖ ̗ ̘ ̙ ̚ ̛ ̜ ̝ ̞ ̟ ̠ ̡ ̢ ̣ ̤ ̥ ̦ ̧ ̨ ̩ ̪ ̫ ̬ ̭ ̮ ̯ ̰ ̱ ̲ ̳ ̴ ̵ ̶ ̷ ̸ ̹ ̺ ̻ ̼ ̽ ̾ ̿ ̀ ́ ͂ ̓ ̈́ ͅ ͠ ͡"';
      var decoded = unicode.fold(unicode.normalize(text));
      var expected = '                                                                       "';

      assert.equal(decoded, expected);
    });
  });
});
