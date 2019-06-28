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

var theTooltip;

/**
 * A custom tooltip similar to the one used in Google Docs which appears
 * instantly when activated on a target element.
 *
 * The tooltip is displayed and hidden by setting its target element.
 *
 *  var tooltip = new Tooltip(document.body);
 *  tooltip.setState({target: aWidget}); // Show tooltip
 *  tooltip.setState({target: null}); // Hide tooltip
 *
 * The tooltip's label is derived from the target element's 'aria-label'
 * attribute.
 *
 * @param {Element} rootElement - The container for the tooltip.
 */
function Tooltip(rootElement) {
  this.setState = function (state) {
    this.state = Object.freeze(Object.assign({}, this.state, state));
    this.render();
  };

  this.render = function () {
    var TOOLTIP_ARROW_HEIGHT = 7;

    if (!this.state.target) {
      this._el.style.visibility = 'hidden';
      return;
    }

    var target = this.state.target;
    var label = target.getAttribute('aria-label');
    this._labelEl.textContent = label;

    var tooltipRect = this._el.getBoundingClientRect();

    var targetRect = target.getBoundingClientRect();
    var top;

    if (this.state.direction === 'up') {
      top = targetRect.bottom + TOOLTIP_ARROW_HEIGHT;
    } else {
      top = targetRect.top - tooltipRect.height - TOOLTIP_ARROW_HEIGHT;
    }
    var left = targetRect.right - tooltipRect.width;

    this._el.classList.toggle('tooltip--up', this.state.direction === 'up');
    this._el.classList.toggle('tooltip--down', this.state.direction === 'down');

    Object.assign(this._el.style, {
      visibility: '',
      top: top + 'px',
      left: left + 'px',
    });
  };

  this._el = rootElement.ownerDocument.createElement('div');
  this._el.innerHTML = '<span class="tooltip-label js-tooltip-label"></span>';
  this._el.className = 'tooltip';

  rootElement.appendChild(this._el);
  this._labelEl = this._el.querySelector('.js-tooltip-label');

  this.setState({
    direction: 'down',
  });
}

/**
 * Attribute directive which displays a custom tooltip when hovering the
 * associated element.
 *
 * The associated element should use the `aria-label` attribute to specify
 * the tooltip instead of the `title` attribute, which would trigger the
 * display of the browser's native tooltip.
 *
 * Example: '<button aria-label="Tooltip label" h-tooltip></button>'
 */
module.exports = function () {
  if (!theTooltip) {
    theTooltip = new Tooltip(document.body);
  }

  return {
    restrict: 'A',
    link: function ($scope, $element) {
      var el = $element[0];

      el.addEventListener('mouseover', function () {
        var direction = el.getAttribute('tooltip-direction') || 'down';
        theTooltip.setState({
          direction: direction,
          target: el,
        });
      });

      el.addEventListener('mouseout', function () {
        theTooltip.setState({target: null});
      });

      // Hide the tooltip if the element is removed whilst the tooltip is active
      $scope.$on('$destroy', function () {
        if (theTooltip.state.target === el) {
          theTooltip.setState({target: null});
        }
      });
    },
  };
};
