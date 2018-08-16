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

let FrameUtil = require('./util/frame-util');
let debounce = require('lodash.debounce');

// Find difference of two arrays
let difference = (arrayA, arrayB) => {
  return arrayA.filter(x => !arrayB.includes(x));
};

const DEBOUNCE_WAIT = 40;

class FrameObserver {

  constructor (target) {
    this._target = target;
    this._handledFrames = [];

    this._mutationObserver = new MutationObserver(debounce(() => {
      this._discoverFrames();
    }, DEBOUNCE_WAIT));
  }

  observe (onFrameAddedCallback, onFrameRemovedCallback) {
    this._onFrameAdded = onFrameAddedCallback;
    this._onFrameRemoved = onFrameRemovedCallback;

    this._discoverFrames();
    this._mutationObserver.observe(this._target, {
      childList: true,
      subtree: true,
    });
  }

  disconnect () {
    this._mutationObserver.disconnect();
  }

  _addFrame (frame) {
    if (FrameUtil.isAccessible(frame)) {
      FrameUtil.isDocumentReady(frame, () => {
        frame.contentWindow.addEventListener('unload', () => {
          this._removeFrame(frame);
        });
        this._handledFrames.push(frame);
        this._onFrameAdded(frame);
      });
    } else {
      // Could warn here that frame was not cross origin accessible
    }
  }

  _removeFrame (frame) {
    this._onFrameRemoved(frame);

    // Remove the frame from our list
    this._handledFrames = this._handledFrames.filter(x => x !== frame);
  }

  _discoverFrames () {
    let frames = FrameUtil.findFrames(this._target);

    for (let frame of frames) {
      if (!this._handledFrames.includes(frame)) {
        this._addFrame(frame);
      }
    }

    for (let frame of difference(this._handledFrames, frames)) {
      this._removeFrame(frame);
    }
  }
}
FrameObserver.DEBOUNCE_WAIT = DEBOUNCE_WAIT;

module.exports = FrameObserver;