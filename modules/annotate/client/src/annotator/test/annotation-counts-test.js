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

var annotationCounts = require('../annotation-counts');

describe('annotationCounts', function () {
  var countEl1;
  var countEl2;
  var CrossFrame;
  var fakeCrossFrame;
  var sandbox;

  beforeEach(function () {
    CrossFrame = null;
    fakeCrossFrame = {};
    sandbox = sinon.sandbox.create();

    countEl1 = document.createElement('button');
    countEl1.setAttribute('data-hypothesis-annotation-count');
    document.body.appendChild(countEl1);

    countEl2 = document.createElement('button');
    countEl2.setAttribute('data-hypothesis-annotation-count');
    document.body.appendChild(countEl2);

    fakeCrossFrame.on = sandbox.stub().returns(fakeCrossFrame);
    
    CrossFrame = sandbox.stub();
    CrossFrame.returns(fakeCrossFrame);
  });

  afterEach(function () {
    sandbox.restore();
    countEl1.remove();
    countEl2.remove();
  });

  describe('listen for "publicAnnotationCountChanged" event', function () {
    var emitEvent = function () {
      var crossFrameArgs;
      var evt;
      var fn;

      var event = arguments[0];
      var args = 2 <= arguments.length ? Array.prototype.slice.call(arguments, 1) : [];

      crossFrameArgs = fakeCrossFrame.on.args;
      for (var i = 0, len = crossFrameArgs.length; i < len; i++) {
        evt = crossFrameArgs[i][0];
        fn = crossFrameArgs[i][1];

        if (event === evt) {
          fn.apply(null, args);
        }
      }
    };

    it('displays the updated annotation count on the appropriate elements', function () {
      var newCount = 10;
      annotationCounts(document.body, fakeCrossFrame);

      emitEvent('publicAnnotationCountChanged', newCount);

      assert.equal(countEl1.textContent, newCount);
      assert.equal(countEl2.textContent, newCount);
    });
  });
});
