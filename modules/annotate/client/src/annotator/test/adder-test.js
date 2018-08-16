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

var adder = require('../adder');
var unroll = require('../../shared/test/util').unroll;

function rect(left, top, width, height) {
  return {left: left, top: top, width: width, height: height};
}

/**
 * Offset an `Element` from its default position.
 */
function offsetElement(el) {
  el.style.position = 'relative';
  el.style.left = '-200px';
  el.style.top = '-200px';
}

/**
 * Reset an element back to its default position.
 */
function revertOffsetElement(el) {
  el.style.position = 'static';
  el.style.left = '0';
  el.style.top = '0';
}


describe('annotator.adder', function () {
  var adderCtrl;
  var adderCallbacks;
  var adderEl;

  beforeEach(function () {
    adderCallbacks = {
      onAnnotate: sinon.stub(),
      onHighlight: sinon.stub(),
    };
    adderEl = document.createElement('div');
    document.body.appendChild(adderEl);

    adderCtrl = new adder.Adder(adderEl, adderCallbacks);
  });

  afterEach(function () {
    adderCtrl.hide();
    adderEl.remove();
  });

  function windowSize() {
    var window = adderCtrl.element.ownerDocument.defaultView;
    return {width: window.innerWidth, height: window.innerHeight};
  }

  function adderSize() {
    var rect = adderCtrl.element.getBoundingClientRect();
    return {width: rect.width, height: rect.height};
  }

  context('when Shadow DOM is supported', function () {
    unroll('creates the adder DOM in a shadow root', function (testCase) {
      var adderEl = document.createElement('div');
      var shadowEl;
      adderEl[testCase.attachFn] = sinon.spy(function () {
        shadowEl = document.createElement('shadow-root');
        adderEl.appendChild(shadowEl);
        return shadowEl;
      });
      document.body.appendChild(adderEl);

      new adder.Adder(adderEl, adderCallbacks);

      assert.called(adderEl[testCase.attachFn]);
      assert.equal(shadowEl.childNodes[0].tagName.toLowerCase(), 'hypothesis-adder-toolbar');

      adderEl.remove();
    },[{
      attachFn: 'createShadowRoot', // Shadow DOM v0 API
    },{
      attachFn: 'attachShadow', // Shadow DOM v1 API
    }]);
  });

  describe('button handling', function () {
    it('calls onHighlight callback when Highlight button is clicked', function () {
      var highlightBtn = adderCtrl.element.querySelector('.js-highlight-btn');
      highlightBtn.dispatchEvent(new Event('click'));
      assert.called(adderCallbacks.onHighlight);
    });

    it('calls onAnnotate callback when Annotate button is clicked', function () {
      var annotateBtn = adderCtrl.element.querySelector('.js-annotate-btn');
      annotateBtn.dispatchEvent(new Event('click'));
      assert.called(adderCallbacks.onAnnotate);
    });
  });

  describe('#target', function () {
    it('positions the adder below the selection if the selection is forwards', function () {
      var target = adderCtrl.target(rect(100,200,100,20), false);
      assert.isAbove(target.top, 220);
      assert.equal(target.arrowDirection, adder.ARROW_POINTING_UP);
    });

    it('positions the adder above the selection if the selection is backwards', function () {
      var target = adderCtrl.target(rect(100,200,100,20), true);
      assert.isBelow(target.top, 200);
      assert.equal(target.arrowDirection, adder.ARROW_POINTING_DOWN);
    });

    it('does not position the adder above the top of the viewport', function () {
      var target = adderCtrl.target(rect(100,-100,100,20), false);
      assert.isAtLeast(target.top, 0);
      assert.equal(target.arrowDirection, adder.ARROW_POINTING_UP);
    });

    it('does not position the adder below the bottom of the viewport', function () {
      var viewSize = windowSize();
      var target = adderCtrl.target(rect(0,viewSize.height + 100,10,20), false);
      assert.isAtMost(target.top, viewSize.height - adderSize().height);
    });

    it('does not position the adder beyond the right edge of the viewport', function () {
      var viewSize = windowSize();
      var target = adderCtrl.target(rect(viewSize.width + 100,100,10,20), false);
      assert.isAtMost(target.left, viewSize.width);
    });

    it('does not positon the adder beyond the left edge of the viewport', function () {
      var target = adderCtrl.target(rect(-100,100,10,10), false);
      assert.isAtLeast(target.left, 0);
    });
  });

  describe('#showAt', () => {
    context('when the document and body elements have no offset', () => {
      it('shows adder at target position', () => {
        adderCtrl.showAt(100, 100, adder.ARROW_POINTING_UP);

        var { left, top } = adderEl.getBoundingClientRect();
        assert.equal(left, 100);
        assert.equal(top, 100);
      });
    });

    context('when the body element is offset', () => {
      beforeEach(() => {
        offsetElement(document.body);
      });

      afterEach(() => {
        revertOffsetElement(document.body);
      });

      it('shows adder at target position', () => {
        adderCtrl.showAt(100, 100, adder.ARROW_POINTING_UP);

        var { left, top } = adderEl.getBoundingClientRect();
        assert.equal(left, 100);
        assert.equal(top, 100);
      });
    });

    context('when the document element is offset', () => {
      beforeEach(() => {
        offsetElement(document.documentElement);
      });

      afterEach(() => {
        revertOffsetElement(document.documentElement);
      });

      it('shows adder at target position when document element is offset', () => {
        adderCtrl.showAt(100, 100, adder.ARROW_POINTING_UP);

        var { left, top } = adderEl.getBoundingClientRect();
        assert.equal(left, 100);
        assert.equal(top, 100);
      });
    });
  });
});
