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

const frameUtil = require('../frame-util');

describe('annotator.util.frame-util', function () {
  describe('findFrames', function () {

    let container;

    const _addFrameToContainer = (options={})=>{
      const frame = document.createElement('iframe');
      frame.setAttribute('enable-annotation', '');
      frame.className = options.className || '';
      frame.style.height = `${(options.height || 150)}px`;
      frame.style.width = `${(options.width || 150)}px`;
      container.appendChild(frame);
      return frame;
    };

    beforeEach(function () {
      container = document.createElement('div');
      document.body.appendChild(container);
    });

    afterEach(function () {
      container.remove();
    });

    it('should return valid frames', function () {

      let foundFrames = frameUtil.findFrames(container);

      assert.lengthOf(foundFrames, 0, 'no frames appended so none should be found');

      const frame1 = _addFrameToContainer();
      const frame2 = _addFrameToContainer();

      foundFrames = frameUtil.findFrames(container);

      assert.deepEqual(foundFrames, [frame1, frame2], 'appended frames should be found');
    });

    it('should not return frames that have not opted into annotation', () => {
      const frame = _addFrameToContainer();

      frame.removeAttribute('enable-annotation');

      const foundFrames = frameUtil.findFrames(container);
      assert.lengthOf(foundFrames, 0);
    });

    it('should not return the Hypothesis sidebar', function () {

      _addFrameToContainer({className: 'h-sidebar-iframe other-class-too'});

      const foundFrames = frameUtil.findFrames(container);

      assert.lengthOf(foundFrames, 0, 'frames with hypothesis className should not be found');
    });
  });
});
