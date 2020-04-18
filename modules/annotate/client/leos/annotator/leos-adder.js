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
var hypothesis_adder = require('../../src/annotator/adder');
var template = require('./leos-adder.html');

var SUGGESTION_BTN_SELECTOR = '.js-suggestion-btn';
var COMMENT_BTN_SELECTOR = '.js-annotate-btn';
var HIGHLIGHT_BTN_SELECTOR = '.js-highlight-btn';
var REMOVED_HIGHLIGHT_BTN = null;

hypothesis_adder.Adder.prototype.handleSuggestCommand = function(event) {
  event.preventDefault();
  event.stopPropagation();

  this.options.onSuggest();

  this.hide();
};

hypothesis_adder.Adder.prototype.handleHighlightCommand = function(event) {
  event.preventDefault();
  event.stopPropagation();

  this.options.onHighlight();

  this.hide();
};

hypothesis_adder.Adder.prototype.extend = function(container, hostBridge, options) {
  this.hostBridge = hostBridge;
  this.options = options;
  var self = this;

  if (container.shadowRoot != null) {
    container.shadowRoot.querySelector("hypothesis-adder-actions").innerHTML = template;
    this.element = container.shadowRoot.querySelector('.js-adder');
  }
  else {
    container.querySelector("hypothesis-adder-actions").innerHTML = template;
    this.element = container.querySelector('.js-adder');
  }

  var suggestBtn = this.element.querySelector(SUGGESTION_BTN_SELECTOR);
  var commentBtn = this.element.querySelector(COMMENT_BTN_SELECTOR);
  var highlightBtn = this.element.querySelector(HIGHLIGHT_BTN_SELECTOR);
  if (highlightBtn) {
    highlightBtn.onclick = this.handleHighlightCommand.bind(this);
  }

  if (commentBtn) {
    commentBtn.onclick = function(event) {
      event.preventDefault();
      event.stopPropagation();

      options.onComment();
    
      self.hide();
    };
  }

  if (suggestBtn) {
    if ((self.hostBridge["requestUserPermissions"]) && (typeof self.hostBridge["requestUserPermissions"] == 'function')) {
      // Add handler on host bridge to let leos application responds
      self.hostBridge["responseUserPermissions"] = function(data) {
        console.log("Received message from host for request UserPermissions");
        if (data.indexOf('CAN_SUGGEST') !== -1) {
          suggestBtn.style.display = "flex";
        }
      };
      self.hostBridge["requestUserPermissions"]();
    }
  
    suggestBtn.style.display = "none";
    suggestBtn.onclick = this.handleSuggestCommand.bind(this);
  }
};

hypothesis_adder.Adder.prototype.disableSuggestionButton = function() {
  var button = this.element.querySelector(SUGGESTION_BTN_SELECTOR);
  if (button) {
    button.onclick = null;
    button.classList.add("annotator-disabled");
    button.setAttribute('data-title','Selection contains elements for which suggestions are not allowed.');
  }
};

hypothesis_adder.Adder.prototype.enableSuggestionButton = function() {
  var button = this.element.querySelector(SUGGESTION_BTN_SELECTOR);
  if (button) {
    button.onclick = this.handleSuggestCommand.bind(this);
    button.classList.remove("annotator-disabled");
    button.removeAttribute('data-title');
  }
};

hypothesis_adder.Adder.prototype.removeHighlightButton = function() {
  var button = this.element.querySelector(HIGHLIGHT_BTN_SELECTOR);
  if (button) {
    REMOVED_HIGHLIGHT_BTN = button;
    button.parentNode.removeChild(button);
  }
};

hypothesis_adder.Adder.prototype.addHighlightButton = function() {
  // All buttons share the same parent
  var highlightButton = this.element.querySelector(HIGHLIGHT_BTN_SELECTOR);
  if(!highlightButton && REMOVED_HIGHLIGHT_BTN) {
    var parent = this.element.querySelector(SUGGESTION_BTN_SELECTOR).parentNode;
    if(parent) {
      parent.appendChild(REMOVED_HIGHLIGHT_BTN);
    }
  }
};
